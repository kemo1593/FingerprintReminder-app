package com.example.fingerprint.domain.model

enum class EventType(val englishLabel: String, val arabicLabel: String) {
    IN("In", "دخول"),
    OUT_IN("Out / In", "خروج / دخول"),
    OUT("Out", "خروج")
}
