package com.test.tcqt.host.service

import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.log.CrashCatcher
import com.test.tcqt.core.log.Log
import com.test.tcqt.host.QQInterfaces
import com.test.tcqt.host.service.maple.MapleContact
import com.tencent.mobileqq.qroute.QRoute
import com.tencent.qqnt.kernel.nativeinterface.MsgConstant
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.PttElement
import com.tencent.qqnt.msg.api.IMsgService
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * 把本地音频文件作为语音（PTT）消息发送到指定会话。
 *
 * 复刻自 fork 版本 TCQT 的语音面板功能。
 */
object VoiceSendUtils {

    private const val TAG = "VoiceSendUtils"

    private const val FORMAT_AMR = 0
    private const val FORMAT_SILK = 1
    private const val FORMAT_MP3 = 2
    private const val FORMAT_AAC = 3

    /** 普通录音语音。 */
    private const val VOICE_TYPE_SOUND_RECORD = 2

    private val SUPPORTED_EXTS = setOf("mp3", "amr", "silk", "slk", "aac", "m4a", "wav", "ogg", "flac")

    private fun extOf(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

    fun isSupported(fileName: String): Boolean = SUPPORTED_EXTS.contains(extOf(fileName))

    fun getSupportedExtsDesc(): String =
        SUPPORTED_EXTS
            .filterNot { it == "slk" }   // slk 是内部格式，不对外暴露
            .joinToString("、") { it.uppercase(Locale.ROOT) }

    fun detectFormatType(fileName: String): Int = when (extOf(fileName)) {
        // QQ 发送侧本地文件用 .slk（不是 .silk），两个都要认
        "silk", "slk" -> FORMAT_SILK
        "aac", "m4a" -> FORMAT_AAC
        "mp3" -> FORMAT_MP3
        else -> FORMAT_AMR
    }

    private fun md5(file: File): String = runCatching {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    private fun readDurationMs(path: String): Int {
        val ms = runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(path)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toIntOrNull()
            }
        }.getOrNull()
        return if (ms != null && ms > 0) ms else 3000
    }

    /** QQ 的 ptt 目录（本地语音文件按 md5 命名放这里，播放器从这里找文件）。 */
    private fun pttDir(): File? = runCatching {
        val uin = QQInterfaces.currentUin
        if (uin.isBlank()) return@runCatching null
        File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.tencent.mobileqq/Tencent/MobileQQ/$uin/ptt"
        )
    }.getOrNull()

    /**
     * 发送前转码：QQ 的语音播放器（SilkPlayer）只解码 silk，
     * 其余格式（mp3/aac/m4a…）需要先转成 silk，否则只有气泡没有声音。
     * 转码产物直接放进 QQ 的 ptt 目录（md5 命名），保证本地也能播放。
     */
    private fun ensureSilk(file: File): File {
        if (extOf(file.name) == "silk") {
            return file
        }
        val ctx = runCatching { HookEnv.hostAppContext }.getOrNull()
        if (ctx == null) {
            Log.e("$TAG: hostAppContext 为空，无法转码，回退原文件")
            CrashCatcher.flushBreadcrumbs("语音转码失败: hostAppContext 为空")
            return file
        }
        val target = pttDir()
        if (target == null) {
            Log.w("$TAG: pttDir 解析失败，转码产物落到 cacheDir")
        }
        val outDir = target ?: File(ctx.cacheDir, "voice_silk")
        val silk = VoiceSilkEncoder.convert(ctx, file, outDir)
        if (silk == null) {
            Log.e("$TAG: silk 转码失败，回退原始文件（本地将无法播放）")
            return file
        }
        // QQ 发送者本地的语音文件名实测为 "<md5>.slk"（silk 缩写，非 .silk/.amr）；
        // 接收侧才是 .amr。这里主文件用 .slk，另外补一份 .amr 兜底。
        val md5 = md5(silk)
        if (md5.isNotEmpty()) {
            val dir = silk.parentFile ?: return silk
            val primary = File(dir, "$md5.slk")
            runCatching {
                if (primary.absolutePath != silk.absolutePath) {
                    if (primary.exists()) primary.delete()
                    if (silk.exists()) silk.renameTo(primary)
                }
            }
            if (primary.exists()) {
                runCatching {
                    val amr = File(dir, "$md5.amr")
                    if (!amr.exists()) primary.copyTo(amr, overwrite = true)
                }
                return primary
            }
        }
        return silk
    }

    /**
     * @param chatType     会话类型（1 好友 / 2 群聊），与宿主 intent 的 `key_chat_type` 一致。
     * @param peerUid      会话对端 uid。
     * @param filePath     本地音频文件绝对路径。
     * @param sendOriginal 直接发送原文件（不转码为 silk）：
     *                     音质无损，但 QQ 只有 SilkPlayer，非 silk 的格式在部分设备
     *                     （尤其苹果端）可能无法播放，也存在其他不可控因素。
     */
    fun sendVoice(
        chatType: Int,
        peerUid: String,
        filePath: String,
        sendOriginal: Boolean = false,
    ): Boolean {
        if (peerUid.isBlank() || filePath.isBlank()) {
            Log.e("$TAG: peerUid 或 filePath 为空")
            CrashCatcher.flushBreadcrumbs("语音发送失败: peerUid 或 filePath 为空")
            return false
        }
        val src = File(filePath)
        if (!src.exists() || !src.isFile) {
            Log.e("$TAG: 文件不存在或不是文件: $filePath")
            CrashCatcher.flushBreadcrumbs("语音发送失败: 文件不存在 $filePath")
            return false
        }
        // 时长必须在转码**之前**从原文件读：silk 文件 MediaMetadataRetriever 解析不了。
        val durationSec = (readDurationMs(src.absolutePath) / 1000).coerceAtLeast(1)

        // ⚠️ 转码是重活（解码 + 重采样 + 逐帧 native 编码，几十秒音频要几秒到十几秒），
        // 绝不能跑在 UI 线程上 —— 否则主线程阻塞会触发 ANR，被系统直接杀掉进程
        // （表现就是"点发送就崩溃"，且不留 tombstone、不留 Java 堆栈）。
        // 这里整体挪到后台线程，只有 sendMsg 回主线程执行。
        Thread({
            try {
                CrashCatcher.breadcrumb("sendVoice: 后台线程开始, duration=${durationSec}s, 原文件模式=$sendOriginal")
                val file = if (sendOriginal) {
                    CrashCatcher.breadcrumb("sendVoice: 跳过转码，直接发原文件")
                    src
                } else {
                    ensureSilk(src)
                }
                CrashCatcher.breadcrumb("sendVoice: 文件就绪 -> ${file.name} ${file.length()}B")

                val ptt = PttElement().apply {
                    fileName = file.name
                    this.filePath = file.absolutePath
                    fileSize = file.length()
                    duration = durationSec
                    formatType = detectFormatType(file.name)
                    voiceType = VOICE_TYPE_SOUND_RECORD
                    voiceChangeType = 0
                    md5HexStr = md5(file)
                    // 波形数据（气泡渲染/播放需要；为空时气泡显示异常）
                    val wave = VoiceSilkEncoder.lastWaveAmplitudes
                    if (wave.isNotEmpty()) waveAmplitudes = wave
                }

                val element = MsgElement().apply {
                    elementType = MsgConstant.KELEMTYPEPTT
                    pttElement = ptt
                }

                val contact = ContactHelper.generateContactByUid(chatType, peerUid)
                if (contact is MapleContact.PublicContact) {
                    CrashCatcher.breadcrumb(
                        "sendVoice: 即将调用 sendMsg (fileName=${ptt.fileName}, formatType=${ptt.formatType})"
                    )
                    Handler(Looper.getMainLooper()).post {
                        try {
                            QRoute.api(IMsgService::class.java)
                                .sendMsg(contact.inner, arrayListOf(element)) { result, msg ->
                                    if (result != 0) {
                                        Log.e("$TAG: sendVoice failed (result=$result, msg=$msg)")
                                        CrashCatcher.flushBreadcrumbs("语音发送失败: sendMsg result=$result msg=$msg")
                                    }
                                }
                            CrashCatcher.breadcrumb("sendVoice: sendMsg 已返回")
                        } catch (t: Throwable) {
                            Log.e("$TAG: sendMsg 异常", t)
                            CrashCatcher.flushBreadcrumbs("语音发送异常: ${t.javaClass.simpleName}: ${t.message}")
                        }
                    }
                } else {
                    Log.e("$TAG: 宿主版本过低，无法生成 PublicContact（需 QQ 9.0.70+）")
                    CrashCatcher.flushBreadcrumbs("语音发送失败: 宿主版本过低（需 QQ 9.0.70+）")
                }
            } catch (t: Throwable) {
                Log.e("$TAG: sendVoice 后台异常", t)
                CrashCatcher.flushBreadcrumbs("语音发送异常: ${t.javaClass.simpleName}: ${t.message}")
            }
        }, "TCQT-VoiceEncode").start()

        return true
    }
}
