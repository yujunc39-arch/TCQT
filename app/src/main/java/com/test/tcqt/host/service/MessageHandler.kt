package com.test.tcqt.host.service

import com.test.tcqt.core.hook.MethodHookParam

interface MessageHandler {

    fun handleInfoSyncPush(buffer: ByteArray, param: MethodHookParam)
    fun handleMsgPush(buffer: ByteArray, param: MethodHookParam)
}
