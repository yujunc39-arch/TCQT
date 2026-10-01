package com.test.tcqt.features.misc

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.widget.EditText
import android.widget.TextView
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.reflect.findMethod

@RegisterAction
object FilterSpecialCharacters : Feature(
    key = "filter_special_characters",
    name = "过滤聊天消息特殊字符",
    desc = "将聊天消息中出现的特殊字符替换为空格。",
) {

    override fun install() {
        TextView::class.java.findMethod {
            name = "onDraw"
            paramTypes(Canvas::class.java)
        }.hookBefore { param ->
            val tv = param.thisObject as? TextView ?: return@hookBefore
            if (tv is EditText) return@hookBefore

            // 未 attach 的 TextView（例如 getDrawingCache 用到的临时 View）没有
            // LayoutParams，此时 setText 会因 checkForRelayout 读 null 而 NPE。
            if (tv.layoutParams == null) return@hookBefore

            val str = tv.text?.toString() ?: return@hookBefore
            if (BLACKLIST.none { str.contains(it) }) return@hookBefore

            val filtered = filterControlCharacter(str)
            if (filtered == str) return@hookBefore

            // 延迟到下一帧再改文本，避免在 onDraw 中直接 setText 触发重绘。
            tv.post {
                runCatching {
                    if (tv.layoutParams != null && tv.text?.toString() != filtered) {
                        tv.text = filtered
                    }
                }
            }
        }
    }

    private fun filterControlCharacter(str: CharSequence): CharSequence {
        var ret = str.toString()
        BLACKLIST.forEach { ret = ret.replace(it, ' ') }
        return ret
    }

    @SuppressLint("BidiSpoofing")
    private const val BLACKLIST = "‭‮‪‫‎⁦⁧‏"
}
