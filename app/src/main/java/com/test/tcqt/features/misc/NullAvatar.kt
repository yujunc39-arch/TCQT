package com.test.tcqt.features.misc

import android.graphics.Bitmap
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.dexkit.DexKitTask
import com.test.tcqt.core.env.avatar.AvatarUtil
import com.test.tcqt.core.env.avatar.toStream
import com.test.tcqt.core.hook.hookBefore
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseMatcher
import java.io.File
import java.io.FileOutputStream

@RegisterAction
object NullAvatar : Feature(
    key = "null_avatar",
    name = "上传透明头像",
    desc = "随便从相册选择一张图片上传即可，自动替换为透明头像。",
), DexKitTask {

    override fun install() {
        requireMethod("NullAvatar").hookBefore { param ->
            File(param.args[0] as String).outputStream().use {
                AvatarUtil.getBitmap(hostApp).toStream().writeTo(it)
            }
        }

        requireMethod("compressUtils").hookBefore { param ->
            val path = param.args[0] as String
            val bitmap = param.args[1] as Bitmap

            FileOutputStream(path).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            param.result = true
        }
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        "NullAvatar" to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.util")
            matcher {
                usingEqStrings("image illegal, size must be square.")
            }
        },
        "compressUtils" to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.pic.compress")
            matcher {
                usingEqStrings("JpegCompressor.compress() error")
            }
        }
    )
}
