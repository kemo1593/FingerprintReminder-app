package com.example.fingerprint.domain.model

enum class EventStatus(val englishLabel: String, val arabicLabel: String) {
    PENDING("Pending", "قيد الانتظار"),
    DONE("Done", "بصمت"),
    SNOOZED("Snoozed", "غفوة")
}
