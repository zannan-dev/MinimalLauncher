package com.example.minimallauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class LauncherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance == this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }

    companion object {
        var instance: LauncherAccessibilityService? = null
            private set

        fun lockScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
        }
    }
}
