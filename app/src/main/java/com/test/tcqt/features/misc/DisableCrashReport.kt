package com.test.tcqt.features.misc

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.env.toClass
import com.test.tcqt.core.hook.doNothing
import com.test.tcqt.core.reflect.findMethod

@RegisterAction
object DisableCrashReport : Feature(
    key = "disable_qq_crash_report_manager",
    name = "禁用崩溃上报",
    desc = "禁止BuglySDK初始化，用途意义不明。",
    processes = setOf(ActionProcess.ALL),
) {

    override fun install() {
        "com.tencent.feedback.eup.CrashReport".toClass.findMethod {
            name = "initCrashReport"
            isStatic = true
            paramTypes = arrayOf(context, string, boolean, null, long)
        }.doNothing()
    }
}
