package com.test.tcqt.features.menu

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.api.Requires
import com.test.tcqt.core.env.QQVersion
import com.test.tcqt.core.env.toClass
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.reflect.findMethod

@RegisterAction
object SettingMeTab : Feature(
    key = "setting_me_tab",
    name = "转移设置页入口",
    desc = "将抽屉设置页面入口移动到下方我的Tab页面",
    // 排到「长按菜单按钮净化」(1005) 之后
    uiOrder = 1010,
    requires = Requires(minQQVersion = QQVersion.QQ_9_1_75),
) {

    override fun install() {
        "com.tencent.mobileqq.api.impl.DrawerApiImpl".toClass.findMethod {
            name = "needUsedSettingMeTab"
            returnType = boolean
        }.hookBefore { param ->
            param.result = true
        }
    }
}
