package com.test.tcqt.features.appearance

import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.api.Requires
import com.test.tcqt.core.env.QQVersion
import com.test.tcqt.core.env.toClass
import com.test.tcqt.core.hook.doNothing
import com.test.tcqt.core.reflect.findMethod

@RegisterAction
object BlockChainAniSticker : Feature(
    key = "block_chain_ani_sticker",
    name = "屏蔽全屏动画彩蛋",
    desc = "屏蔽发送或接收超级表情时触发的全屏连锁动画播放。",
    requires = Requires(minQQVersion = QQVersion.QQ_9_0_20),
) {

    override fun install() {
        "com.tencent.mobileqq.aio.animation.api.impl.AioAnimationApiImpl".toClass.findMethod {
            name = "handleNewMsg"
        }.doNothing()
    }
}
