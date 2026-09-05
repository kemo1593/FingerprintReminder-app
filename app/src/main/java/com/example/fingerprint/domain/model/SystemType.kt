package com.example.fingerprint.domain.model

enum class SystemType(
    val displayNameEn: String,
    val displayNameAr: String,
    val isDailyShift: Boolean
) {
    SYSTEM_8_2("8 - 2", "8 - 2", true),
    SYSTEM_8_3("8 - 3", "8 - 3", true),
    SYSTEM_8_5("8 - 5", "8 - 5", true),
    SYSTEM_9_3("9 - 3", "9 - 3", true),
    SYSTEM_9_5("9 - 5", "9 - 5", true),
    SYSTEM_10_5("10 - 5", "10 - 5", true),
    SYSTEM_18_6("18 / 6", "18 / 6", false),
    SYSTEM_24_6("24 / 6", "24 / 6", false),
    SYSTEM_12_12("12 / 12", "12 / 12", false);

    companion object {
        fun get18_6Label(isAm: Boolean, isArabic: Boolean): String {
            return if (isArabic) "18 / 6 راحة" else "18 / 6 rest"
        }

        fun get24_6Label(isAm: Boolean, isArabic: Boolean): String {
            return if (isArabic) "24 / 6 راحة" else "24 / 6 rest"
        }

        fun get12_12Label(isAm: Boolean, isArabic: Boolean): String {
            return if (isArabic) "12 / 12 راحة" else "12 / 12 rest"
        }
    }
}
