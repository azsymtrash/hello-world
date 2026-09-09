package com.callscribe.capture

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Не чете съдържание на екрана. Съществува само защото на Android 12+ фонов broadcast
 * не може да стартира foreground услуга с микрофон, а активна услуга за достъпност може.
 * Тук просто следим състоянието на телефонния разговор.
 */
class CallWatchAccessibilityService : AccessibilityService() {

    private var telephonyCallback: TelephonyCallback? = null

    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerCallStateListener()
        Log.i(TAG, "Услугата за следене на разговори е активна.")
    }

    @Suppress("DEPRECATION")
    private fun registerCallStateListener() {
        if (!Contacts.hasPermission(this, Manifest.permission.READ_PHONE_STATE)) {
            Log.w(TAG, "Липсва разрешение READ_PHONE_STATE.")
            return
        }
        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val context = applicationContext

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        CallStateHandler.onState(context, state)
                    }
                }
                telephonyCallback = callback
                telephony.registerTelephonyCallback(mainExecutor, callback)
            } else {
                val listener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        CallStateHandler.onState(context, state)
                    }
                }
                phoneStateListener = listener
                telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Не може да се абонира за състоянието на разговора: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { telephony?.unregisterTelephonyCallback(it) }
            } else {
                phoneStateListener?.let { telephony?.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        } catch (ignored: Exception) {
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "CallWatch"

        fun isEnabled(context: Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(context.packageName + "/" + CallWatchAccessibilityService::class.java.name)
        }
    }
}
