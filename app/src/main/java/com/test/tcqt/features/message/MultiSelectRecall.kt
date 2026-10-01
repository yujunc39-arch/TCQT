package com.test.tcqt.features.message

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.test.tcqt.R
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.env.HookEnv.toHostClass
import com.test.tcqt.core.env.Toasts
import com.test.tcqt.core.hook.hookAfter
import com.test.tcqt.core.log.Log
import com.test.tcqt.core.reflect.findMethod
import com.test.tcqt.core.reflect.getObject
import com.test.tcqt.core.reflect.getStaticObject
import com.test.tcqt.core.sync.ModuleScope
import com.test.tcqt.host.QQInterfaces
import com.tencent.mobileqq.aio.msg.AIOMsgItem
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact
import kotlinx.coroutines.delay
import java.lang.Thread.sleep
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

@RegisterAction
object MultiSelectRecall : Feature(
    key = "multi_select_recall",
    name = "消息多选撤回",
    desc = "启用本功能后,消息多选模式下可批量撤回选中消息,非管理员也可使用。",
) {

    private var multiSelectBarVM: Any? = null

    override fun install() {
        Reflection.init()

        Reflection.createVM.hookAfter { param ->
            multiSelectBarVM = param.result
        }

        Reflection.operationInvoke.hookAfter { param ->
            val layout = param.result as? LinearLayout ?: return@hookAfter
            injectRecallButton(layout, param.thisObject)
        }
    }

    private fun injectRecallButton(
        operationLayout: LinearLayout,
        operationLambda: Any
    ) {
        val vb = operationLambda.getObject($$"this$0")

        val recallBtn = Reflection.makeView.invoke(
            null,
            vb,
            R.drawable.ic_action_recall,
            R.drawable.ic_action_recall,
            View.OnClickListener { performBatchRecall() }
        ) as View

        // 灰度图标在暗色模式下与深色背景几乎同色（看不清），按主题重新着色
        applyThemeTint(recallBtn)

        val index = (operationLayout.childCount - 2).coerceAtLeast(0)
        operationLayout.addView(recallBtn, index)
    }

    /** 按当前暗/亮模式给图标着色：暗色 -> 浅色，亮色 -> 深色。 */
    private fun applyThemeTint(root: View) {
        val night = runCatching { HookEnv.isNightMode() }.getOrDefault(false)
        val color = if (night) 0xFFEAEAEA.toInt() else 0xFF3B3B3B.toInt()

        fun walk(v: View) {
            when (v) {
                is ImageView -> runCatching { v.setColorFilter(color) }
                is ViewGroup -> for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
        }
        runCatching { walk(root) }
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    private fun performBatchRecall() {
        runCatching {
            val vm = multiSelectBarVM
            val forward = Reflection.multiForwardClass.getStaticObject("a")
            val context = Reflection.getContext.invoke(vm)
            val msgList = Reflection.getMsgList.invoke(forward, context) as List<AIOMsgItem>

            if (msgList.isEmpty()) {
                Toasts.error("AIOMsgItem is empty")
                return
            }

            ModuleScope.launchIO {
                val list = CopyOnWriteArrayList(msgList)
                list.forEachIndexed { index, item ->
                    recallSingleItem(item)
                    if (index >= 10) {
                        delay(300L.milliseconds)
                    }
                }
            }

            QQInterfaces.topActivity.onBackPressed()
        }.onFailure {
            Log.e("performBatchRecall", it)
        }
    }

    private fun recallSingleItem(msg: AIOMsgItem) {
        val record = msg.msgRecord
        val contact = Contact(record.chatType, record.peerUid, record.guildId)

        recallWithRetry(contact, record.msgId, retry = 3)
    }

    private fun recallWithRetry(
        contact: Contact,
        msgId: Long,
        retry: Int
    ) {
        QQInterfaces.msgService.recallMsg(contact, arrayListOf(msgId)) { code, err ->
            if (code == 0) return@recallMsg

            if (retry > 0) {
                sleep(200L)
                recallWithRetry(contact, msgId, retry - 1)
            } else {
                Log.e("尝试撤回消息ID为 $msgId 时失败, errCode:$code, errStr:$err")
            }
        }
    }

    private object Reflection {

        lateinit var makeView: Method
        lateinit var createVM: Method
        lateinit var getMsgList: Method
        lateinit var getContext: Method
        lateinit var operationInvoke: Method
        lateinit var multiForwardClass: Class<*>

        fun init() {
            val barVB = if (HookEnv.isQQ()) {
                "com.tencent.mobileqq.aio.input.multiselect.MultiSelectBarVB"
            } else {
                "com.tencent.tim.aio.inputbar.TimMultiSelectBarVB"
            }.toHostClass()

            val operationLambda = $$"$${barVB.name}$mOperationLayout$2".toHostClass()

            multiForwardClass =
                "com.tencent.mobileqq.aio.msglist.holder.component.multifoward.b".toHostClass()

            getMsgList = multiForwardClass.findMethod {
                returnType = list
                paramCount = 1
            }

            getContext = "com.tencent.mvi.mvvm.framework.FrameworkVM".toHostClass().findMethod {
                returnType = getMsgList.parameterTypes[0].superclass
                paramCount = 0
            }

            makeView = barVB.findMethod {
                returnType = view
                paramCount = 4
                paramTypes = arrayOf(barVB, int, int, View.OnClickListener::class.java)
            }

            createVM = barVB.findMethod {
                returnType = "com.tencent.mvi.mvvm.BaseVM".toHostClass()
                paramCount = 0
            }

            operationInvoke = operationLambda.findMethod {
                name = "invoke"
            }
        }
    }
}
