package com.test.tcqt.features.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.InfraTask
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.command.ModuleCommandBus
import com.test.tcqt.core.config.TCQTSetting
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.log.Log
import com.test.tcqt.core.sync.ReceiverRegistry
import mqq.app.MobileQQ

@RegisterAction
object ModuleCommand : InfraTask(
    key = "ModuleCommand",
    processes = setOf(ActionProcess.MAIN),
) {

    override fun install() {
        val filter = IntentFilter(ModuleCommandBus.ACTION_MODULE_COMMAND)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val cmd = intent.getStringExtra("cmd") ?: return
                when (cmd) {
                    ModuleCommandBus.CMD_RESTART -> {
                        MobileQQ.getMobileQQ()?.takeIf {
                            it.isRuntimeReady
                        }?.run {
                            HookEnv.resetApp()
                        }
                    }

                    ModuleCommandBus.CMD_EXIT -> {
                        MobileQQ.getMobileQQ()?.takeIf {
                            it.isRuntimeReady
                        }?.run {
                            otherProcessExit(false)
                            qqProcessExit(true)
                        }
                    }

                    ModuleCommandBus.CMD_CONFIG_CLEAR -> {
                        try {
                            TCQTSetting.clearAll()
                        } catch (t: Throwable) {
                            Log.e("ModuleCommand onReceive config_clear error", t)
                        }
                    }
                }
            }
        }

        ReceiverRegistry.register(hostApp, receiver, filter)
    }
}
