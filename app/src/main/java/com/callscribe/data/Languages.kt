package com.callscribe.data

/**
 * Езикът важи и за двете стъпки: подава се на сървъра за транскрипция и определя
 * на какъв език моделът пише извлечените задачи.
 */
object Languages {

    data class Option(val code: String, val label: String)

    val all = listOf(
        Option("bg", "Български"),
        Option("en", "English")
    )

    const val DEFAULT = "bg"

    fun label(code: String): String =
        all.firstOrNull { it.code == code }?.label ?: code

    fun normalize(code: String): String =
        if (all.any { it.code == code }) code else DEFAULT
}
