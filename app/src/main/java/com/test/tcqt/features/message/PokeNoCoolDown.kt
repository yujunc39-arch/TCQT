package com.test.tcqt.features.message

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.env.HookEnv.toHostClass
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.reflect.findMethod

@RegisterAction
object PokeNoCoolDown : Feature(
    key = "poke_no_cool_down",
    name = "戳一戳无冷却",
    desc = "移除戳一戳冷却时间(每天上限200次)。",
) {


    override fun install() {
        "com.tencent.mobileqq.paiyipai.PaiYiPaiHandler".toHostClass().findMethod {
            returnType = boolean
            visibility = private
            paramTypes = arrayOf(string)
            paramCount = 1
        }.hookBefore { param -> param.result = true }
    }
}
