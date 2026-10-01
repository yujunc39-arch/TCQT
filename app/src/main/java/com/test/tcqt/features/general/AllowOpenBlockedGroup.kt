package com.test.tcqt.features.general

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.api.Requires
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.hook.paramCount
import com.test.tcqt.core.reflect.findMethod
import com.test.tcqt.core.reflect.getObjectByType
import com.test.tcqt.core.reflect.setObjectByType
import com.tencent.mobileqq.data.troop.TroopInfo
import com.tencent.mobileqq.troop.troopsetting.vm.TroopSettingViewModel

@RegisterAction
object AllowOpenBlockedGroup : Feature(
    key = "allow_open_blocked_group",
    name = "允许打开被封禁群组",
    desc = "解除被封禁群组无法进入聊天页面的限制。",
    requires = Requires(host = Requires.Host.QQOnly),
) {

    override fun install() {
        TroopInfo::class.java.apply {
            findMethod {
                name = "isUnreadableBlock"
                returnType = boolean
            }.hookBefore { param ->
                param.result = false
            }
            findMethod {
                name = "isNeedInterceptOnBlockTroop"
                returnType = boolean
            }.hookBefore { param ->
                param.result = false
            }
        }

        TroopSettingViewModel::class.java.declaredMethods.single { m ->
            m.paramCount == 3 &&
            m.parameterTypes[0] == m.declaringClass &&
            m.parameterTypes[1] == String::class.java &&
            m.parameterTypes[2].simpleName != "TroopSearchWay"
        }.hookBefore { param ->
            if (param.args[2]!!.getObjectByType<Int>() == 72) { // 群已解散/已不是群成员
                param.args[2]!!.setObjectByType<Int>(0)
            }
        }
    }
}
