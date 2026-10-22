package com.glip.driver.ads

import android.graphics.Bitmap
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.VideoView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Universal Multi-Ad Slider Container for Driver App supporting:
 * 1. Multiple ads per container (Ad1, Ad2, Ad3...)
 * 2. Individual slide durations (e.g. Ad1 for 6s, Ad2 for 2s)
 * 3. Static Images, Animated GIFs, and Looping Videos
 * 4. Slide indicator dots & telemetry reporting
 */
@Composable
fun AdBannerBox(
    slotId: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    contentScale: ContentScale = ContentScale.Crop,
    showAdBadge: Boolean = true,
    fallbackContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val slotsMap by AdManager.slotsState.collectAsState()
    val slot = slotsMap[slotId]

    // Trigger immediate fast refresh if slot is missing or data is stale
    LaunchedEffect(slotId) {
        if (slot == null || AdManager.isStale()) {
            AdManager.refresh()
        }
    }

    val adsList = slot?.getActiveAdsList() ?: emptyList()

    // If slot is explicitly disabled by admin and has no active ads, show fallback
    if (slot != null && !slot.isActive && adsList.isEmpty()) {
        fallbackContent?.invoke()
        return
    }

    if (adsList.isNotEmpty()) {
        // Multi-ad Slider: current slide index
        var currentIndex by remember(adsList) { mutableIntStateOf(0) }
        val safeIndex = if (adsList.isNotEmpty()) currentIndex % adsList.size else 0
        val currentAd = adsList[safeIndex]

        // Auto-advance slider based on individual ad's displayDurationSeconds
        if (adsList.size > 1) {
            LaunchedEffect(safeIndex, currentAd.id, adsList.size) {
                val durationMs = (currentAd.displayDurationSeconds.coerceAtLeast(1) * 1000L)
                delay(durationMs)
                currentIndex = (safeIndex + 1) % adsList.size
            }
        }

        // Report impression when current ad becomes visible
        LaunchedEffect(currentAd.id, slotId) {
            AdManager.onAdDisplayed(currentAd.id, slotId)
        }

        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable {
                        AdManager.onAdClicked(context, currentAd, slotId)
                    }
            ) {
                // Smooth transition between slides
                Crossfade(targetState = currentAd, label = "DriverAdSlideCrossfade") { ad ->
                    AdMediaRenderer(
                        ad = ad,
                        contentScale = contentScale
                    )
                }

                // Multi-Ad Slider Indicator Dots (at bottom center)
                if (adsList.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        adsList.forEachIndexed { index, _ ->
                            val isActive = index == safeIndex
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(if (isActive) 12.dp else 4.dp)
                                    .background(
                                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }

                // Small sponsor / ad badge
                if (showAdBadge) {
                    val badgeText = when (currentAd.getMediaType()) {
                        AdMediaType.VIDEO -> "Ad • Video"
                        AdMediaType.GIF -> "Ad • GIF"
                        AdMediaType.IMAGE -> "Ad"
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 8.sp,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }
    } else {
        // No ad currently assigned to this slot
        if (fallbackContent != null) {
            fallbackContent()
        } else {
            Card(
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Renders individual ad media (Video, GIF, or Static Bitmap)
 */
@Composable
private fun AdMediaRenderer(
    ad: AppAd,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val mediaType = ad.getMediaType()

    when (mediaType) {
        AdMediaType.VIDEO -> {
            // 1. VIDEO AD: Looping muted video
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(ad.bannerImageUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            start()
                        }
                        setOnErrorListener { _, _, _ -> true }
                    }
                },
                update = { videoView ->
                    if (!videoView.isPlaying) {
                        videoView.start()
                    }
                }
            )
        }

        AdMediaType.GIF -> {
            // 2. ANIMATED GIF AD: Native animated drawable
            var gifDrawable by remember(ad.bannerImageUrl) { mutableStateOf<Drawable?>(null) }

            LaunchedEffect(ad.bannerImageUrl) {
                gifDrawable = AdManager.loadGifDrawable(context, ad.bannerImageUrl)
            }

            if (gifDrawable != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageDrawable(gifDrawable)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                (gifDrawable as? AnimatedImageDrawable)?.start()
                            }
                        }
                    },
                    update = { imageView ->
                        imageView.setImageDrawable(gifDrawable)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            (gifDrawable as? AnimatedImageDrawable)?.start()
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }

        AdMediaType.IMAGE -> {
            // 3. STATIC IMAGE AD: Cached bitmap (frame 0 instant render from memory/disk cache)
            var bitmap by remember(ad.bannerImageUrl) {
                mutableStateOf(AdManager.getCachedBitmap(ad.bannerImageUrl))
            }

            LaunchedEffect(ad.bannerImageUrl) {
                if (bitmap == null) {
                    bitmap = AdManager.loadBitmap(ad.bannerImageUrl)
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = ad.name,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        }
    }
}
