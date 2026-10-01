package com.test.tcqt.features.misc

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.env.toClass
import com.test.tcqt.core.reflect.setValue

@RegisterAction
object OpenQLog : Feature(
    key = "open_q_log",
    name = "日志输出到Logcat",
    desc = "没事别瞎打开(可能会影响性能)，只是为了方便调试。",
    processes = setOf(ActionProcess.ALL),
) {

    override fun install() {
        "com.tencent.qphone.base.util.QLog".toClass.apply {
            setValue("useXlog", false)
            setValue("UIN_REPORTLOG_LEVEL", 4)
        }
    }
}
