package com.callscribe.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Настройки на приложението. Ключовете за API се пазят в SharedPreferences —
 * това НЕ е сигурно хранилище: всеки с root достъп или ADB backup може да ги прочете.
 * За реална употреба насочи `aiBaseUrl` към собствен прокси сървър и остави ключа празен.
 */
class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("callscribe", Context.MODE_PRIVATE)

    var consentAccepted: Boolean
        get() = prefs.getBoolean(K_CONSENT, false)
        set(v) = prefs.edit().putBoolean(K_CONSENT, v).apply()

    var recordCalls: Boolean
        get() = prefs.getBoolean(K_RECORD_CALLS, false)
        set(v) = prefs.edit().putBoolean(K_RECORD_CALLS, v).apply()

    var forceSpeaker: Boolean
        get() = prefs.getBoolean(K_FORCE_SPEAKER, true)
        set(v) = prefs.edit().putBoolean(K_FORCE_SPEAKER, v).apply()

    var readSms: Boolean
        get() = prefs.getBoolean(K_READ_SMS, false)
        set(v) = prefs.edit().putBoolean(K_READ_SMS, v).apply()

    /** Автоматично засичане на нови съобщения на фон, без натискане на бутон. */
    var autoSync: Boolean
        get() = prefs.getBoolean(K_AUTO_SYNC, true)
        set(v) = prefs.edit().putBoolean(K_AUTO_SYNC, v).apply()

    /** Докъде е стигнало последното сканиране на съобщенията. */
    var lastSmsImportAt: Long
        get() = prefs.getLong(K_LAST_SMS_IMPORT, 0L)
        set(v) = prefs.edit().putLong(K_LAST_SMS_IMPORT, v).apply()

    var deleteAudioAfterTranscript: Boolean
        get() = prefs.getBoolean(K_DELETE_AUDIO, false)
        set(v) = prefs.edit().putBoolean(K_DELETE_AUDIO, v).apply()

    /** Отместване по подразбиране на напомнянето преди крайния срок, в минути. */
    var reminderOffsetMinutes: Int
        get() = prefs.getInt(K_REMINDER_OFFSET, 30)
        set(v) = prefs.edit().putInt(K_REMINDER_OFFSET, v).apply()

    // --- Claude (извличане на задачи) ---
    var aiBaseUrl: String
        get() = prefs.getString(K_AI_BASE, "https://api.anthropic.com") ?: ""
        set(v) = prefs.edit().putString(K_AI_BASE, v.trim().trimEnd('/')).apply()

    var aiApiKey: String
        get() = prefs.getString(K_AI_KEY, "") ?: ""
        set(v) = prefs.edit().putString(K_AI_KEY, v.trim()).apply()

    var aiModel: String
        get() = prefs.getString(K_AI_MODEL, "claude-sonnet-5") ?: ""
        set(v) = prefs.edit().putString(K_AI_MODEL, v.trim()).apply()

    // --- Транскрипция (OpenAI-съвместим /v1/audio/transcriptions) ---
    var asrBaseUrl: String
        get() = prefs.getString(K_ASR_BASE, "") ?: ""
        set(v) = prefs.edit().putString(K_ASR_BASE, v.trim().trimEnd('/')).apply()

    var asrApiKey: String
        get() = prefs.getString(K_ASR_KEY, "") ?: ""
        set(v) = prefs.edit().putString(K_ASR_KEY, v.trim()).apply()

    var asrModel: String
        get() = prefs.getString(K_ASR_MODEL, "whisper-1") ?: ""
        set(v) = prefs.edit().putString(K_ASR_MODEL, v.trim()).apply()

    var language: String
        get() = prefs.getString(K_LANG, "bg") ?: "bg"
        set(v) = prefs.edit().putString(K_LANG, v.trim()).apply()

    val aiConfigured: Boolean get() = aiBaseUrl.isNotBlank() && (aiApiKey.isNotBlank() || !aiBaseUrl.contains("api.anthropic.com"))
    val asrConfigured: Boolean get() = asrBaseUrl.isNotBlank()

    private companion object {
        const val K_CONSENT = "consent"
        const val K_RECORD_CALLS = "record_calls"
        const val K_FORCE_SPEAKER = "force_speaker"
        const val K_READ_SMS = "read_sms"
        const val K_DELETE_AUDIO = "delete_audio"
        const val K_AUTO_SYNC = "auto_sync"
        const val K_LAST_SMS_IMPORT = "last_sms_import"
        const val K_REMINDER_OFFSET = "reminder_offset"
        const val K_AI_BASE = "ai_base"
        const val K_AI_KEY = "ai_key"
        const val K_AI_MODEL = "ai_model"
        const val K_ASR_BASE = "asr_base"
        const val K_ASR_KEY = "asr_key"
        const val K_ASR_MODEL = "asr_model"
        const val K_LANG = "lang"
    }
}
