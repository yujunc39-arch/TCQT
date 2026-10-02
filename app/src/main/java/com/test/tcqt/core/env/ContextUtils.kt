package com.test.tcqt.core.env

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import com.test.tcqt.core.log.Log

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
internal object ContextUtils {

    /**
     * 反射取当前前台 Activity；取不到时返回 **null** 而不是抛异常。
     *
     * 这里是调用链的最后一层兜底（`QBaseActivity.sTopActivity` /
     * `Foreground.getTopActivity()` 都为空才会走到这），而启动早期或全部
     * Activity 均处于 paused 时本来就取不到 —— 属于预期情形。
     * 曾经在这里抛 `IllegalStateException`，结果冷启动时在主线程裸抛，
     * 被 QQ 的 crashdefend 看门狗包装成 `CrashDefendException` 强进安全模式。
     */
    fun getCurrentActivity(): Activity? {
        return runCatching {
            val activityThread = Class.forName(
                "android.app.ActivityThread",
                false,
                Application::class.java.classLoader
            ).getMethod("currentActivityThread").invoke(null) ?: return null

            val activities = activityThread::class.java
                .getDeclaredField("mActivities")
                .apply { isAccessible = true }
                .get(activityThread) as? Map<*, *> ?: return null

            val record = activities.values
                .firstOrNull { r ->
                    r != null && !r::class.java
                        .getDeclaredField("paused")
                        .apply { isAccessible = true }
                        .getBoolean(r)
                } ?: return null

            record::class.java
                .getDeclaredField("activity")
                .apply { isAccessible = true }
                .get(record) as? Activity
        }.onFailure {
            Log.w("getCurrentActivity failed: ${it.message}")
        }.getOrNull()
    }

    fun getCurApplication(): Application {
        return tryGetApplication("android.app.ActivityThread", "currentApplication")
            ?: tryGetApplication("android.app.AppGlobals", "getInitialApplication")
            ?: throw IllegalStateException("Failed to get current application")
    }

    private fun tryGetApplication(className: String, methodName: String): Application? {
        return runCatching {
            Class.forName(className)
                .getDeclaredMethod(methodName)
                .apply { isAccessible = true }
                .invoke(null) as? Application
        }.onFailure {
            Log.e("getCurApplication: $className.$methodName failed", it)
        }.getOrNull()
    }
}
