package com.test.tcqt.core.log

import android.util.Log
import com.test.tcqt.core.env.TCQTBuild
import com.test.tcqt.core.hook.HookEngineManager

enum class LogLevel {

    VERBOSE, DEBUG, INFO, WARN, ERROR
}

private val XPOSED_OUTPUT_LEVELS =
    setOf(LogLevel.INFO, LogLevel.DEBUG, LogLevel.WARN, LogLevel.ERROR)

interface Logger {

    fun log(level: LogLevel, message: String, throwable: Throwable? = null)

    fun v(message: String, throwable: Throwable? = null) = log(LogLevel.VERBOSE, message, throwable)
    fun d(message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, message, throwable)
    fun i(message: String, throwable: Throwable? = null) = log(LogLevel.INFO, message, throwable)
    fun w(message: String, throwable: Throwable? = null) = log(LogLevel.WARN, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)
}

class AndroidLogger(private val tag: String) : Logger {

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
}

class XposedLogger(
    private val tag: String,
    private val androidLogger: Logger = AndroidLogger(tag)
) : Logger {

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        androidLogger.log(level, message, throwable)

        val priority = when (level) {
            LogLevel.VERBOSE -> Log.VERBOSE
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }

        if (level in XPOSED_OUTPUT_LEVELS && HookEngineManager.isInitialized) {
            HookEngineManager.engine.log(priority, tag, message, throwable)
        }

        // 落盘策略：正式版只记录 WARN / ERROR。
        // 正常流程（INFO 及以下）不写文件，避免 TCQT/log 目录持续增长；
        // 调试构建保持全量落盘，便于排查。
        if (!TCQTBuild.DEBUG && level.ordinal < LogLevel.WARN.ordinal) return

        when (level) {
            LogLevel.VERBOSE -> FileLog.v(message, tag, throwable)
            LogLevel.DEBUG -> FileLog.d(message, tag, throwable)
            LogLevel.INFO -> FileLog.i(message, tag, throwable)
            LogLevel.WARN -> FileLog.w(message, tag, throwable)
            LogLevel.ERROR -> FileLog.e(message, tag, throwable)
        }
    }
}

/**
 * 依据构建类型过滤日志。
 *
 * 注意：这里**不能**用 `: Logger by delegate`。Kotlin 的接口委托会把接口的
 * **全部**成员（包括 `v/d/i/w/e` 这些带默认实现的方法）直接转发给 delegate，
 * 于是 `debugLogger.i(...)` 会绕过被覆盖的 [log] 直接落到 [XposedLogger.i]，
 * 过滤形同虚设。必须逐个重写。
 *
 * 正式版（[TCQTBuild.DEBUG] == false）只放行 [LogLevel.WARN] 及以上：
 * 正常流程不产生任何日志，只有真正出问题时才留下记录。
 */
class DebugFilterLogger(
    private val delegate: Logger,
    private val isDebug: Boolean = TCQTBuild.DEBUG
) : Logger {

    private fun allow(level: LogLevel): Boolean =
        isDebug || level.ordinal >= LogLevel.WARN.ordinal

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        if (allow(level)) delegate.log(level, message, throwable)
    }

    override fun v(message: String, throwable: Throwable?) {
        if (allow(LogLevel.VERBOSE)) delegate.v(message, throwable)
    }

    override fun d(message: String, throwable: Throwable?) {
        if (allow(LogLevel.DEBUG)) delegate.d(message, throwable)
    }

    override fun i(message: String, throwable: Throwable?) {
        if (allow(LogLevel.INFO)) delegate.i(message, throwable)
    }

    override fun w(message: String, throwable: Throwable?) {
        if (allow(LogLevel.WARN)) delegate.w(message, throwable)
    }

    override fun e(message: String, throwable: Throwable?) {
        if (allow(LogLevel.ERROR)) delegate.e(message, throwable)
    }
}

object LogUtils {

    private const val TAG = TCQTBuild.HOOK_TAG

    val xposed: Logger = DebugFilterLogger(XposedLogger(TAG))

    val android: Logger = DebugFilterLogger(AndroidLogger(TAG))

    val xposedNoFilter: Logger = XposedLogger(TAG)

    val androidNoFilter: Logger = AndroidLogger(TAG)
}

object Log : Logger by LogUtils.xposed

object LogAndroid : Logger by LogUtils.android
