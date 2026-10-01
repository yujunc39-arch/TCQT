package com.test.tcqt.features.internal.pipeline

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.InfraTask
import com.test.tcqt.api.PipelineDecorator
import com.test.tcqt.api.Requires
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.reflect.findMethod
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService
import com.tencent.qqnt.kernel.nativeinterface.MsgElement

@RegisterAction
object AIOSendMsgBefore : InfraTask(
    key = "AIOSendMsgBefore",
    requires = Requires(ntOnly = true),
) {

    @Suppress("UNCHECKED_CAST")
    override fun install() {
        val activeDecorators = PipelineDecorators.all(OnAIOSendMsgBefore::class.java)
            .filter { it.isAvailable() }
            .onEach { it.activate() }
            .takeIf { it.isNotEmpty() } ?: return

        IKernelMsgService.CppProxy::class.java.findMethod {
            name = "sendMsg"
        }.hookBefore { param ->
            val elements = param.args[2] as ArrayList<MsgElement>
            activeDecorators.forEach {
                it.onSend(elements)
            }
        }
    }
}

fun interface OnAIOSendMsgBefore : PipelineDecorator {

    fun onSend(elements: ArrayList<MsgElement>)
}
