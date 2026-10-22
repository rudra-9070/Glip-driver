package com.glip.driver.ads

/**
 * Supported Advertisement Creative Types
 */
enum class AdMediaType {
    IMAGE,
    GIF,
    VIDEO
}

/**
 * Advertisement data entity received from API with slider positioning & duration
 */
data class AppAd(
    val id: String,
    val name: String,
    val advertiserName: String,
    val bannerImageUrl: String,
    val destinationUrl: String,
    val targetCategory: String = "All Services",
    val slidePosition: Int = 1,
    val displayDurationSeconds: Int = 5
) {
    /**
     * Determines whether the ad creative is a Video, Animated GIF, or Static Image
     */
    fun getMediaType(): AdMediaType {
        val lower = bannerImageUrl.lowercase()
        return when {
            lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.contains("/video") -> AdMediaType.VIDEO
            lower.endsWith(".gif") || lower.contains("image/gif") -> AdMediaType.GIF
            else -> AdMediaType.IMAGE
        }
    }
}

/**
 * In-app placement container configuration supporting Multi-Ad Slider
 */
data class PlacementSlot(
    val slotId: String,
    val name: String,
    val screenName: String,
    val heightDp: Int,
    val widthDp: Int? = null,
    val aspectRatio: String = "",
    val rotationIntervalSeconds: Int = 10,
    val isActive: Boolean = true,
    val ad: AppAd? = null,
    val ads: List<AppAd> = emptyList()
) {
    /**
     * Returns the list of active ads to display in the slider.
     * Uses `ads` array if present, otherwise falls back to single `ad`.
     */
    fun getActiveAdsList(): List<AppAd> {
        return when {
            ads.isNotEmpty() -> ads
            ad != null && ad.bannerImageUrl.isNotBlank() -> listOf(ad)
            else -> emptyList()
        }
    }
}
