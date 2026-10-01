package com.test.tcqt.features.misc

import android.app.Activity
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.hook.hookBefore

@RegisterAction
object FakeMultiWindowStatus : Feature(
    key = "fake_multi_window_status",
    name = "伪装多窗口状态",
    desc = "不知道有什么用。",
    processes = setOf(ActionProcess.ALL),
) {


    override fun install() {
        Activity::class.java.getDeclaredMethod("isInMultiWindowMode")
            .hookBefore { it.result = false }

        Activity::class.java.getDeclaredMethod("isInPictureInPictureMode")
            .hookBefore { it.result = false }
    }

}
