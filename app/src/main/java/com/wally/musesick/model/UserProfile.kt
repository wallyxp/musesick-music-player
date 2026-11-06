package com.wally.musesick.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val name: String = "Guest",
    val avatarPath: String? = null
)

