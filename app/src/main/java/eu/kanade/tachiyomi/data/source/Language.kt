package eu.kanade.tachiyomi.data.source

class Language(val code: String, val lang: String)

val DE = Language("DE", "German")
val EN = Language("EN", "English")
val VN = Language("VN", "Vietnamese")

fun getLanguages() = listOf(DE, EN, RU, VN)
