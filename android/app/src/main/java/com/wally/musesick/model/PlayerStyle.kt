package com.wally.musesick.model

enum class PlayerStyle(val displayName: String, val description: String) {
    FULLSCREEN_ALBUM_ART(
        displayName = "Full Screen Album Art",
        description = "Full-bleed album artwork background with gradient scrims and clean white controls"
    ),
    NON_FULLSCREEN_ALBUM_ART(
        displayName = "Non Full Screen Album Art",
        description = "Classic centered album artwork card with Material palette styling. Reflects chosen theme color."
    );

    companion object {
        fun fromString(value: String?): PlayerStyle {
            return when (value) {
                "FULLSCREEN_ALBUM_ART", "MODERN" -> FULLSCREEN_ALBUM_ART
                "NON_FULLSCREEN_ALBUM_ART", "GENERIC" -> NON_FULLSCREEN_ALBUM_ART
                else -> FULLSCREEN_ALBUM_ART
            }
        }
    }
}
