package com.test.tcqt.features.advanced

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess

@RegisterAction
object DisableHotPatch : Feature(
    key = "disable_hot_patch",
    name = "禁用热补丁加载",
    desc = "顾名思义，但不会删除已有的热补丁文件。",
    processes = setOf(ActionProcess.ALL),
) {


    override fun install() = Unit
}
