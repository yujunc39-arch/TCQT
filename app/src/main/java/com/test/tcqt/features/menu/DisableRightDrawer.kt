package com.test.tcqt.features.menu

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.env.loadOrThrow
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.reflect.Visibility
import com.test.tcqt.core.reflect.findMethod

@RegisterAction
object DisableRightDrawer : Feature(
    key = "disable_right_drawer",
    name = "禁用聊天右侧抽屉",
    desc = "屏蔽在聊天界面向左滑动呼出右侧面板（如群应用、亲密关系等）",
) {


    override fun install() {
        loadOrThrow("com.tencent.aio.frame.drawer.DrawerFrameViewGroup").findMethod {
            visibility = Visibility.PRIVATE
            paramCount = 2
            paramTypes(float, string)
        }.hookBefore { param ->
            val dx = param.args[0] as? Float ?: return@hookBefore
            if (dx < 0.0f) {
                param.result = false
            }
        }
    }
}
