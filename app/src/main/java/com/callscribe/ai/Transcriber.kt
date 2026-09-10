package com.callscribe.ai

import android.content.Context
import com.callscribe.R
import com.callscribe.data.Settings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

/**
 * Транскрипция през OpenAI-съвместим endpoint `POST {base}/v1/audio/transcriptions`.
 * Работи с локален whisper.cpp сървър, faster-whisper-server, OpenAI и т.н.
 */
class Transcriber(private val context: Context, private val settings: Settings) {

    fun transcribe(file: File): String {
        val base = settings.asrBaseUrl
        if (base.isBlank()) throw AiException(context.getString(R.string.err_no_asr))
        if (!file.exists() || file.length() == 0L) throw AiException(context.getString(R.string.err_empty_audio))

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("audio/mp4".toMediaType()))
            .addFormDataPart("model", settings.asrModel.ifBlank { "whisper-1" })
            .addFormDataPart("language", settings.language.ifBlank { "bg" })
            .addFormDataPart("response_format", "json")
            .build()

        val builder = Request.Builder()
            .url("$base/v1/audio/transcriptions")
            .post(body)
        if (settings.asrApiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${settings.asrApiKey}")
        }

        Http.client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiException(context.getString(R.string.err_transcribe, response.code, text.take(300)))
            }
            return try {
                JSONObject(text).optString("text").trim()
            } catch (e: Exception) {
                text.trim()
            }
        }
    }
}
