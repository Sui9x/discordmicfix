package com.sui.discordmicfix

import android.media.AudioManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Hook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "DiscordMicFix"

        private val TARGET_MANAGER_CLASSES = arrayOf(
            "com.discord.audio.DiscordAudioManager",
            "com.discord.audio.DiscordAudioManager2"
        )

        private const val CLS_AUDIO_MANAGER = "android.media.AudioManager"

        // nest
        private val scopeDepth = ThreadLocal<Int>()

        private fun log(msg: String) {
            XposedBridge.log("$TAG: $msg")
        }

        private fun hookAll(
            className: String,
            methodName: String,
            lpparam: XC_LoadPackage.LoadPackageParam,
            hook: XC_MethodHook
        ) {
            try {
                val clazz = XposedHelpers.findClass(className, lpparam.classLoader)
                XposedBridge.hookAllMethods(clazz, methodName, hook)
                log("HOOKED(all): $className->$methodName")
            } catch (t: Throwable) {
                log("HOOK FAIL: $className->$methodName : $t")
            }
        }

        private fun enterScope() {
            val cur = scopeDepth.get() ?: 0
            scopeDepth.set(cur + 1)
        }

        private fun exitScope() {
            val cur = scopeDepth.get() ?: 0
            if (cur <= 1) {
                scopeDepth.remove()
            } else {
                scopeDepth.set(cur - 1)
            }
        }

        private fun inScope(): Boolean {
            return (scopeDepth.get() ?: 0) > 0
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        log("handleLoadPackage: ${lpparam.packageName}")

        val commModeHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val on = (param.args.getOrNull(0) as? Boolean) ?: return
                if (!on) return

                enterScope()

                val cls = param.thisObject?.javaClass?.name ?: "unknown"
                log("ENTER $cls.setCommunicationModeOn(true), depth=${scopeDepth.get() ?: 0}")
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val on = (param.args.getOrNull(0) as? Boolean) ?: return
                if (!on) return

                val cls = param.thisObject?.javaClass?.name ?: "unknown"
                log("EXIT  $cls.setCommunicationModeOn(true), depth=${scopeDepth.get() ?: 0}")

                exitScope()
            }
        }

        // DiscordAudioManager / DiscordAudioManager2
        for (cls in TARGET_MANAGER_CLASSES) {
            hookAll(
                cls,
                "setCommunicationModeOn",
                lpparam,
                commModeHook
            )
        }

        // in target scope
        hookAll(
            CLS_AUDIO_MANAGER,
            "setMode",
            lpparam,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!inScope()) return

                    val mode = (param.args.getOrNull(0) as? Int) ?: return
                    if (mode == AudioManager.MODE_IN_COMMUNICATION) {
                        log("BLOCK setMode(MODE_IN_COMMUNICATION) -> MODE_NORMAL")
                        param.args[0] = AudioManager.MODE_NORMAL
                    }
                }
            }
        )
    }
}