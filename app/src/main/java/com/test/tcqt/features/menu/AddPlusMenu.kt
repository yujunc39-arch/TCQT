package com.test.tcqt.features.menu

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionPriority
import com.test.tcqt.core.command.ModuleCommandBus
import com.test.tcqt.core.dexkit.DexKitTask
import com.test.tcqt.host.service.ExtraMenuItem
import com.test.tcqt.host.service.PlusMenuManager
import org.luckypray.dexkit.query.base.BaseMatcher

@RegisterAction
object AddPlusMenu : Feature(
    key = "add_plus_menu",
    name = "添加额外选项",
    desc = "给主页右上角菜单添加额外功能选项(结束/重启进程)。",
    priority = ActionPriority.EARLY,
), DexKitTask {

    override fun install() {
        PlusMenuManager.registerAll(
            ExtraMenuItem(
                id = 23331,
                title = "结束进程",
                iconResId = com.test.tcqt.R.drawable.ic_item_exit_72dp,
                onClick = { ModuleCommandBus.sendCommand(hostApp, ModuleCommandBus.CMD_EXIT) }
            ),
            ExtraMenuItem(
                id = 23332,
                title = "重启进程",
                iconResId = com.test.tcqt.R.drawable.ic_item_reboot_72dp,
                onClick = { ModuleCommandBus.sendCommand(hostApp, ModuleCommandBus.CMD_RESTART) }
            )
        )

        PlusMenuManager.ensureHooksInstalled(this)
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        PlusMenuManager.PLUS_MENU_CLICK_QUERY to PlusMenuManager.clickActionMatcher()
    )
}
