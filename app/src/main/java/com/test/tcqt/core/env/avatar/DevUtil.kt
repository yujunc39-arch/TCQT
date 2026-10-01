/** https://github.com/HdShare/NullAvatar */

package com.test.tcqt.core.env.avatar

import com.test.tcqt.core.log.Log

fun printStackTrace() {
    val stackTrace = Throwable().stackTrace
    val stackTraceStr = stackTrace.joinToString("\n") { element ->
        "at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
    }
    Log.d("StackTrace\n$stackTraceStr")
}
