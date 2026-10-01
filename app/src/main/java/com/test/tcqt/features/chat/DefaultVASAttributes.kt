package com.test.tcqt.features.chat

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.api.Requires
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.env.QQVersion
import com.test.tcqt.core.env.isFlagEnabled
import com.test.tcqt.core.env.toClass
import com.test.tcqt.core.hook.hookAfter
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.reflect.findMethod
import com.test.tcqt.host.QQInterfaces
import com.tencent.mobileqq.aio.msg.AIOMsgItem
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord

@RegisterAction
object DefaultVASAttributes : Feature(
    key = "default_vas_attrs",
    name = "净化聊天界面装扮",
    desc = "默认禁用他人消息的个性化气泡、字体、QQ秀头像与头像挂件，若需保留特定项目（如头像挂件），请在下方勾选排除。",
    requires = Requires(ntOnly = true, host = Requires.Host.QQOnly),
) {

    private val options by multiIntOption(
        settingKey = "type",
        name = "可选保留",
        defaultValue = 0,
        options = listOf("保留个性气泡", "保留个性字体", "保留头像挂件", "保留QQ秀头像"),
    )

    override fun install() {
        AIOMsgItem::class.java.findMethod {
            name = "getMsgRecord"
            paramCount = 0
        }.hookAfter { param ->
            val msgRecord = param.result as MsgRecord
            if (msgRecord.senderUin.toString() != QQInterfaces.currentUin) {
                msgRecord.msgAttrs?.values?.forEach { u ->
                    u?.vasMsgInfo?.let { vasInfo ->

                        // 隐藏头像挂件
                        if (options.isFlagEnabled(2).not()) {
                            vasInfo.avatarPendantInfo?.pendantId = 0L
                            vasInfo.avatarPendantInfo?.pendantDiyInfoId = 0
                        }

                        // 强制默认气泡
                        if (options.isFlagEnabled(0).not()) {
                            vasInfo.bubbleInfo?.bubbleId = 0
                            vasInfo.bubbleInfo?.subBubbleId = 0
                        }

                        // 强制默认字体
                        if (options.isFlagEnabled(1).not()) {
                            vasInfo.vasFont?.fontId = 0
                            vasInfo.vasFont?.subFontId = 0L
                            vasInfo.vasFont?.magicFontType = 0
                        }
                    }
                }
            }
        }

        if (options.isFlagEnabled(3).not()) {
            if (HookEnv.requireMinQQVersion(QQVersion.QQ_9_2_27)) {
                "com.tencent.mobileqq.ai.avatar.api.impl.AIAvatarSwitchApiImpl".toClass
                    .findMethod {
                        name = "isQQShowEnableForAIO"
                        paramTypes(long, int, long)
                    }.hookBefore { param ->
                        val uin = (param.args[2] as Long).toString()
                        if (uin != QQInterfaces.currentUin) {
                            param.result = false
                        }
                    }
            }
        }
    }
}
