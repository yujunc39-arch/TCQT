package com.test.tcqt.features.general

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionPriority
import com.test.tcqt.core.dexkit.DexKitTask
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.env.QQVersion
import com.test.tcqt.core.env.toClass
import com.test.tcqt.core.hook.hookAfter
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.hook.paramCount
import com.test.tcqt.core.reflect.allConstructors
import com.test.tcqt.core.reflect.setObjectByType
import com.tencent.mobileqq.selectmember.ResultRecord
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseMatcher

@RegisterAction
object RemoveShareLimit : Feature(
    key = "remove_share_limit",
    name = "移除转发选择数量限制",
    desc = "移除转发消息时最多选择9名联系人的限制。",
    priority = ActionPriority.BACKGROUND,
), DexKitTask {

    private lateinit var friendListActivityCls: Class<*>
    private lateinit var recentActivityCls: Class<*>
    private lateinit var troopListFragmentCls: Class<*>
    private lateinit var selectTroopListFragmentCls: Class<*>


    /** 只在打开转发选择页时才会被调用，放到 BACKGROUND 错峰安装。 */

    private val isKuiklyUISupported: Boolean by lazy {
        HookEnv.requireMinQQVersion(QQVersion.QQ_9_2_25)
    }

    override fun install() {
        friendListActivityCls = "com.tencent.mobileqq.activity.ForwardFriendListActivity".toClass
        recentActivityCls = "com.tencent.mobileqq.activity.ForwardRecentActivity".toClass
        troopListFragmentCls = "com.tencent.mobileqq.activity.ForwardTroopListFragment".toClass
        selectTroopListFragmentCls = "com.tencent.mobileqq.selectmember.troop.SelectTroopListFragment".toClass

        if (isKuiklyUISupported) {
            requireClass("remove_share_limit")
                .allConstructors()
                .filter { it.paramCount >= 3 }
                .forEach {
                    it.hookBefore { param ->
                        param.args[2] = Int.MAX_VALUE
                    }
                }
        }

        listOf(
            friendListActivityCls,
            recentActivityCls,
            troopListFragmentCls,
            selectTroopListFragmentCls
        ).forEach { cls ->
            cls.allConstructors().first().hookAfter { param ->
                param.thisObject.setObjectByType<Map<String, ResultRecord>>(UnlimitedMap(), cls)
            }
        }
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = if (isKuiklyUISupported) {
        mapOf(
            "remove_share_limit" to FindClass().apply {
                matcher {
                    usingStrings("ChatSelectorConfig(")
                }
            }
        )
    } else {
        emptyMap()
    }

    private class UnlimitedMap<K, V> : LinkedHashMap<K, V>() {

        override val size: Int
            get() {
                val s = super.size
                return if (s == 9) 8 else s
            }
    }
}
