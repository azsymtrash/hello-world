package com.callscribe.capture

import android.content.Context
import android.telephony.TelephonyManager

/**
 * Единна точка за обработка на състоянието на разговора.
 * Използва се както от AccessibilityService, така и от PHONE_STATE broadcast-а,
 * затова пази последното състояние, за да не стартира записа два пъти.
 */
object CallStateHandler {

    @Volatile
    private var lastState: Int = TelephonyManager.CALL_STATE_IDLE

    @Synchronized
    fun onState(context: Context, state: Int) {
        if (state == lastState) return
        val previous = lastState
        lastState = state

        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> CallRecordingService.start(context)
            TelephonyManager.CALL_STATE_IDLE ->
                if (previous == TelephonyManager.CALL_STATE_OFFHOOK) CallRecordingService.stop(context)
            else -> Unit // RINGING — изчакваме вдигане
        }
    }

    fun onState(context: Context, stateName: String?) {
        val state = when (stateName) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> return
        }
        onState(context, state)
    }
}
