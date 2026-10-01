package com.test.tcqt.features.cleanup

import android.os.Message
import android.view.View
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionPriority
import com.test.tcqt.core.env.ClassCacheUtils
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.env.load
import com.test.tcqt.core.env.loadOrThrow
import com.test.tcqt.core.hook.emptyParam
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.hook.hookMethodBefore
import com.test.tcqt.core.hook.isFinal
import com.test.tcqt.core.hook.isNotStatic
import com.test.tcqt.core.hook.isPublic
import com.test.tcqt.core.hook.paramCount

@RegisterAction
object RemoveAD : Feature(
    key = "remove_ad",
    name = "移除部分广告",
    desc = "移除一些常见的广告弹窗。",
    priority = ActionPriority.CRITICAL,
) {


    override fun install() {
        removeImmersionBannerAD()
        removeKeywordAD()
        removePopupAD()
    }

    private fun removeImmersionBannerAD() {
        ClassCacheUtils.findClass {
            candidates(
                "cooperation.vip.qqbanner.QbossADImmersionBannerManager",
                "cooperation.vip.qqbanner.manager.VasADImmersionBannerManager"
            )
            syntheticIndex(1, 2, 3, 5)
        }?.declaredMethods
            ?.filter { it.returnType == View::class.java && it.emptyParam && it.isNotStatic }
            ?.onEach { it.hookBefore { p -> p.result = null } }
    }

    private fun removeKeywordAD() {
        if (HookEnv.isQQ()) {
            loadOrThrow("com.tencent.mobileqq.springhb.interactive.ui.InteractivePopManager")
                .declaredMethods
                .firstOrNull {
                    it.isPublic && it.paramCount > 0 &&
                            it.parameterTypes[0].name == "androidx.fragment.app.Fragment"
                }?.hookBefore { param -> param.result = null }
            load("com.tencent.mobileqq.aio.animation.pag.PagEasterEggPopManager")
                ?.declaredMethods
                ?.firstOrNull {
                    it.isPublic && it.paramCount > 0 &&
                            it.parameterTypes[0].name == "androidx.fragment.app.Fragment"
                }?.hookBefore { param -> param.result = null }
        }
    }

    private fun removePopupAD() {
        load(
            "com.tencent.mobileqq.activity.recent.bannerprocessor.VasADBannerProcessor"
        )?.hookMethodBefore({
            name = "updateBanner"
            paramTypes = arrayOf(null, Message::class.java)
        }) {
            it.result = null
        }

        load("cooperation.vip.ad.GrowHalfLayerHelper")
            ?.declaredMethods
            ?.firstOrNull { method ->
                method.returnType == Void.TYPE && method.isPublic &&
                        method.isFinal && method.paramCount == 3 &&
                        method.parameterTypes[0].name == "android.app.Activity"
            }?.hookBefore { param -> param.result = null }
    }
}
