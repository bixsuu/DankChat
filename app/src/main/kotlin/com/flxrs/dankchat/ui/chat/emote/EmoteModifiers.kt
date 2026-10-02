package com.flxrs.dankchat.ui.chat.emote

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.hasAnimatedEffects
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isAppear
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isBounce
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isCursed
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isFlipX
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isFlipY
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isGreyscale
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isHyperRed
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isJam
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isLeave
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isRainbow
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isRotate
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isRotate90
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isSepia
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isShake
import com.flxrs.dankchat.data.twitch.emote.FFZModifierFlags.isSlide
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class Keyframe(
    val pct: Float,
    val tx: Float,
    val sx: Float,
    val sy: Float,
    val ty: Float,
    val alpha: Float = 1f,
)

private val APPEAR_KEYFRAMES = listOf(
    Keyframe(pct = 0f, tx = -14f, sx = 0.1f, sy = 0.1f, ty = 0f, alpha = 0f),
    Keyframe(pct = 15f, tx = -14f, sx = 0.15f, sy = 0.15f, ty = 0f, alpha = 1f),
    Keyframe(pct = 22f, tx = -12f, sx = 0.3f, sy = 0.3f, ty = 0.6f, alpha = 1f),
    Keyframe(pct = 29f, tx = -10f, sx = 0.45f, sy = 0.45f, ty = -3.5f, alpha = 1f),
    Keyframe(pct = 36f, tx = -8f, sx = 0.6f, sy = 0.6f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 43f, tx = -6f, sx = 0.7f, sy = 0.7f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 50f, tx = -4f, sx = 0.8f, sy = 0.8f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 57f, tx = -2f, sx = 0.9f, sy = 0.9f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 64f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 1f),
    Keyframe(pct = 86f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 1f),
    Keyframe(pct = 96f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 0f),
    Keyframe(pct = 100f, tx = -14f, sx = 0.1f, sy = 0.1f, ty = 0f, alpha = 0f),
)

private val LEAVE_KEYFRAMES = listOf(
    Keyframe(pct = 0f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 0f),
    Keyframe(pct = 8f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 1f),
    Keyframe(pct = 35f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 1f),
    Keyframe(pct = 35.01f, tx = 0f, sx = -0.9f, sy = 0.9f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 42f, tx = -2f, sx = -0.8f, sy = 0.8f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 49f, tx = -4f, sx = -0.7f, sy = 0.7f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 56f, tx = -6f, sx = -0.6f, sy = 0.6f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 63f, tx = -8f, sx = -0.5f, sy = 0.5f, ty = -3.5f, alpha = 1f),
    Keyframe(pct = 70f, tx = -10f, sx = -0.4f, sy = 0.4f, ty = 0.6f, alpha = 1f),
    Keyframe(pct = 77f, tx = -12f, sx = -0.3f, sy = 0.3f, ty = -3.5f, alpha = 1f),
    Keyframe(pct = 84f, tx = -14f, sx = -0.2f, sy = 0.2f, ty = 0.6f, alpha = 1f),
    Keyframe(pct = 90f, tx = -14f, sx = -0.1f, sy = 0.1f, ty = 0f, alpha = 0f),
    Keyframe(pct = 100f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 0f),
)

private val BOTH_KEYFRAMES = listOf(
    Keyframe(pct = 0f, tx = -14f, sx = 0.1f, sy = 0.1f, ty = 0f, alpha = 0f),
    Keyframe(pct = 8f, tx = -14f, sx = 0.15f, sy = 0.15f, ty = 0f, alpha = 1f),
    Keyframe(pct = 12f, tx = -12f, sx = 0.3f, sy = 0.3f, ty = 0.6f, alpha = 1f),
    Keyframe(pct = 16f, tx = -10f, sx = 0.45f, sy = 0.45f, ty = -3.5f, alpha = 1f),
    Keyframe(pct = 20f, tx = -8f, sx = 0.6f, sy = 0.6f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 24f, tx = -6f, sx = 0.7f, sy = 0.7f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 28f, tx = -4f, sx = 0.8f, sy = 0.8f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 32f, tx = -2f, sx = 0.9f, sy = 0.9f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 36f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 1f),
    Keyframe(pct = 58f, tx = 0f, sx = 1f, sy = 1f, ty = 0f, alpha = 1f),
    Keyframe(pct = 58.01f, tx = 0f, sx = -0.9f, sy = 0.9f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 62f, tx = -2f, sx = -0.8f, sy = 0.8f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 66f, tx = -4f, sx = -0.7f, sy = 0.7f, ty = -2.5f, alpha = 1f),
    Keyframe(pct = 70f, tx = -6f, sx = -0.6f, sy = 0.6f, ty = 1.8f, alpha = 1f),
    Keyframe(pct = 74f, tx = -8f, sx = -0.5f, sy = 0.5f, ty = -3.5f, alpha = 1f),
    Keyframe(pct = 78f, tx = -10f, sx = -0.4f, sy = 0.4f, ty = 0.6f, alpha = 1f),
    Keyframe(pct = 82f, tx = -12f, sx = -0.3f, sy = 0.3f, ty = -3.5f, alpha = 1f),
    Keyframe(pct = 86f, tx = -14f, sx = -0.2f, sy = 0.2f, ty = 0.6f, alpha = 1f),
    Keyframe(pct = 92f, tx = -14f, sx = -0.1f, sy = 0.1f, ty = 0f, alpha = 0f),
    Keyframe(pct = 100f, tx = -14f, sx = 0.1f, sy = 0.1f, ty = 0f, alpha = 0f),
)

private fun interpolateKeyframes(frames: List<Keyframe>, pct: Float): Keyframe {
    if (pct <= frames.first().pct) return frames.first()
    if (pct >= frames.last().pct) return frames.last()

    for (i in 0 until frames.size - 1) {
        val f1 = frames[i]
        val f2 = frames[i + 1]
        if (pct in f1.pct..f2.pct) {
            val range = f2.pct - f1.pct
            val fraction = if (range > 0f) (pct - f1.pct) / range else 0f
            return Keyframe(
                pct = pct,
                tx = f1.tx + (f2.tx - f1.tx) * fraction,
                sx = f1.sx + (f2.sx - f1.sx) * fraction,
                sy = f1.sy + (f2.sy - f1.sy) * fraction,
                ty = f1.ty + (f2.ty - f1.ty) * fraction,
                alpha = f1.alpha + (f2.alpha - f1.alpha) * fraction,
            )
        }
    }
    return frames.last()
}

private val SEPIA_MATRIX = ColorMatrix(
    floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

private val HYPER_RED_MATRIX = ColorMatrix(
    floatArrayOf(
        2.5f, 0f, 0f, 0f, 0.1f,
        0.1f, 0.3f, 0f, 0f, 0f,
        0.1f, 0f, 0.3f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

private val CURSED_MATRIX = ColorMatrix(
    floatArrayOf(
        0.6f, 1.2f, 0.2f, 0f, -0.4f,
        0.6f, 1.2f, 0.2f, 0f, -0.4f,
        0.6f, 1.2f, 0.2f, 0f, -0.4f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

private fun createHueRotateMatrix(angleDegrees: Float): ColorMatrix {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val cosVal = cos(radians).toFloat()
    val sinVal = sin(radians).toFloat()

    val a00 = 0.213f + cosVal * 0.787f - sinVal * 0.213f
    val a01 = 0.715f - cosVal * 0.715f - sinVal * 0.715f
    val a02 = 0.072f - cosVal * 0.072f + sinVal * 0.928f

    val a10 = 0.213f - cosVal * 0.213f + sinVal * 0.143f
    val a11 = 0.715f + cosVal * 0.285f + sinVal * 0.140f
    val a12 = 0.072f - cosVal * 0.072f - sinVal * 0.283f

    val a20 = 0.213f - cosVal * 0.213f - sinVal * 0.787f
    val a21 = 0.715f - cosVal * 0.715f + sinVal * 0.715f
    val a22 = 0.072f + cosVal * 0.928f + sinVal * 0.072f

    return ColorMatrix(
        floatArrayOf(
            a00, a01, a02, 0f, 0f,
            a10, a11, a12, 0f, 0f,
            a20, a21, a22, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

@Composable
fun Modifier.applyEmoteModifierTransforms(
    modifierFlags: Int,
    animateGifs: Boolean,
    isPageVisible: Boolean,
): Modifier {
    if (modifierFlags == 0) return this

    val isFlipX = modifierFlags.isFlipX
    val isFlipY = modifierFlags.isFlipY
    val isRotate90 = modifierFlags.isRotate90
    val isRotate = modifierFlags.isRotate
    val isShake = modifierFlags.isShake
    val isJam = modifierFlags.isJam
    val isBounce = modifierFlags.isBounce
    val isAppear = modifierFlags.isAppear
    val isLeave = modifierFlags.isLeave
    val isSlide = modifierFlags.isSlide

    val shouldAnimate = animateGifs && isPageVisible && modifierFlags.hasAnimatedEffects

    val walkFrame = if (shouldAnimate && (isAppear || isLeave)) {
        val isBoth = isAppear && isLeave
        val duration = if (isBoth) 5500 else 3000
        val transition = rememberInfiniteTransition(label = "ffzWalk")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "walkProgress",
        )
        when {
            isBoth -> interpolateKeyframes(BOTH_KEYFRAMES, progress)
            isAppear -> interpolateKeyframes(APPEAR_KEYFRAMES, progress)
            else -> interpolateKeyframes(LEAVE_KEYFRAMES, progress)
        }
    } else {
        Keyframe(0f, 0f, 1f, 1f, 0f, 1f)
    }

    val slideOffset = if (shouldAnimate && isSlide) {
        val transition = rememberInfiniteTransition(label = "ffzSlide")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "slidePhase",
        )
        sin(phase * 2 * PI.toFloat()) * 8f
    } else {
        0f
    }

    val spinAngle = if (shouldAnimate && isRotate) {
        val transition = rememberInfiniteTransition(label = "ffzSpin")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "spinAngle",
        )
        angle
    } else {
        0f
    }

    val jamProgress = if (shouldAnimate && isJam) {
        val transition = rememberInfiniteTransition(label = "ffzJam")
        val progress by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "jamProgress",
        )
        progress
    } else {
        0f
    }

    val bounceScaleY = if (shouldAnimate && isBounce) {
        val transition = rememberInfiniteTransition(label = "ffzBounce")
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bounceScaleY",
        )
        scale
    } else {
        1f
    }

    val shakeOffsets = if (shouldAnimate && isShake) {
        val transition = rememberInfiniteTransition(label = "ffzShake")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shakePhase",
        )
        val offsetX = sin(phase * 2 * PI.toFloat() * 3) * 3f
        val offsetY = cos(phase * 2 * PI.toFloat() * 2) * 2f
        offsetX to offsetY
    } else {
        0f to 0f
    }

    val density = LocalDensity.current.density

    return graphicsLayer {
        val baseSx = if (isFlipX) -1f else 1f
        var baseSy = if (isFlipY) -1f else 1f

        if (isBounce) {
            baseSy *= bounceScaleY
            transformOrigin = TransformOrigin(0.5f, 1f)
        }

        val sx = baseSx * walkFrame.sx
        scaleX = if (abs(sx) < 0.01f) (if (sx < 0) -0.01f else 0.01f) else sx

        val sy = baseSy * walkFrame.sy
        scaleY = if (abs(sy) < 0.01f) (if (sy < 0) -0.01f else 0.01f) else sy

        alpha = walkFrame.alpha

        var rot = if (isRotate90) 90f else 0f
        if (isRotate) {
            rot += spinAngle
        }
        if (isJam) {
            rot += jamProgress * 6f
        }
        rotationZ = rot

        var tx = walkFrame.tx * density
        var ty = walkFrame.ty * density
        if (isSlide) {
            tx += slideOffset * density
        }
        if (isJam) {
            tx += jamProgress * 2f * density
            ty += abs(jamProgress) * 2f * density
        }
        if (isShake) {
            tx += shakeOffsets.first * density
            ty += shakeOffsets.second * density
        }
        translationX = tx
        translationY = ty
    }
}

@Composable
fun rememberEmoteColorFilter(
    modifierFlags: Int,
    animateGifs: Boolean,
    isPageVisible: Boolean,
): ColorFilter? {
    if (modifierFlags == 0) return null

    val isGreyscale = modifierFlags.isGreyscale
    val isSepia = modifierFlags.isSepia
    val isCursed = modifierFlags.isCursed
    val isHyperRed = modifierFlags.isHyperRed
    val isRainbow = modifierFlags.isRainbow

    if (!isGreyscale && !isSepia && !isCursed && !isHyperRed && !isRainbow) {
        return null
    }

    if (isHyperRed) {
        return remember { ColorFilter.colorMatrix(HYPER_RED_MATRIX) }
    }
    if (isCursed) {
        return remember { ColorFilter.colorMatrix(CURSED_MATRIX) }
    }
    if (isSepia) {
        return remember { ColorFilter.colorMatrix(SEPIA_MATRIX) }
    }
    if (isGreyscale) {
        return remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    }
    if (isRainbow) {
        val shouldAnimate = animateGifs && isPageVisible
        if (!shouldAnimate) {
            return remember { ColorFilter.colorMatrix(createHueRotateMatrix(0f)) }
        }
        val transition = rememberInfiniteTransition(label = "ffzRainbow")
        val hueAngle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rainbowHue",
        )
        return remember(hueAngle) {
            ColorFilter.colorMatrix(createHueRotateMatrix(hueAngle))
        }
    }

    return null
}
