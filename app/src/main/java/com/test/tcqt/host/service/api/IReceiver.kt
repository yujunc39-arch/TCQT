package com.test.tcqt.host.service.api

fun interface IReceiver {
    fun onReceive(data: ByteArray)
}
