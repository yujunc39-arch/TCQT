package com.test.tcqt.features.message

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.api.Requires
import com.test.tcqt.core.env.QQVersion
import com.test.tcqt.core.hook.hookMethodAfter
import com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil
import com.tencent.qqnt.kernel.nativeinterface.ReplyMsgMainInfo

@RegisterAction
object RemoveReplyMsgCheck : Feature(
    key = "remove_reply_msg_check",
    name = "移除回复消息不存在限制",
    desc = "常见于解决‘转发的聊天记录中不包含该内容’的提示，允许查看回复消息。",
    requires = Requires(host = Requires.Host.QQOnly, minQQVersion = QQVersion.QQ_9_1_75),
) {

    override fun install() {
        QQNTWrapperUtil.CppProxy::class.java.hookMethodAfter({
            name = "findSourceOfReplyMsgFrom"
            paramTypes = arrayOf(arrayList, ReplyMsgMainInfo::class.java)
        }) { param ->
            val result = param.result as Long
            if (result == 0L) {
                param.result = 1L
            }
        }
    }
}
