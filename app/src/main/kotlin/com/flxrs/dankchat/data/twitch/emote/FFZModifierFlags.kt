package com.flxrs.dankchat.data.twitch.emote

object FFZModifierFlags {
    const val NONE = 0
    const val HIDDEN = 1 // 1 shl 0
    const val FLIP_X = 2 // 1 shl 1
    const val FLIP_Y = 4 // 1 shl 2
    const val GROW_X = 8 // 1 shl 3
    const val SLIDE = 16 // 1 shl 4
    const val APPEAR = 32 // 1 shl 5
    const val LEAVE = 64 // 1 shl 6
    const val ROTATE = 128 // 1 shl 7
    const val ROTATE_90 = 256 // 1 shl 8
    const val GREYSCALE = 512 // 1 shl 9
    const val SEPIA = 1024 // 1 shl 10
    const val RAINBOW = 2048 // 1 shl 11
    const val HYPER_RED = 4096 // 1 shl 12
    const val SHAKE = 8192 // 1 shl 13
    const val CURSED = 16384 // 1 shl 14
    const val JAM = 32768 // 1 shl 15
    const val BOUNCE = 65536 // 1 shl 16
    const val NO_SPACE = 131072 // 1 shl 17

    val Int.isHiddenEffect: Boolean
        get() = (this and HIDDEN) != 0

    val Int.isFlipX: Boolean
        get() = (this and FLIP_X) != 0

    val Int.isFlipY: Boolean
        get() = (this and FLIP_Y) != 0

    val Int.isGrowX: Boolean
        get() = (this and GROW_X) != 0

    val Int.isSlide: Boolean
        get() = (this and SLIDE) != 0

    val Int.isAppear: Boolean
        get() = (this and APPEAR) != 0

    val Int.isLeave: Boolean
        get() = (this and LEAVE) != 0

    val Int.isRotate90: Boolean
        get() = (this and ROTATE_90) != 0

    val Int.isRotate: Boolean
        get() = (this and ROTATE) != 0

    val Int.isGreyscale: Boolean
        get() = (this and GREYSCALE) != 0

    val Int.isSepia: Boolean
        get() = (this and SEPIA) != 0

    val Int.isRainbow: Boolean
        get() = (this and RAINBOW) != 0

    val Int.isHyperRed: Boolean
        get() = (this and HYPER_RED) != 0

    val Int.isShake: Boolean
        get() = (this and SHAKE) != 0

    val Int.isCursed: Boolean
        get() = (this and CURSED) != 0

    val Int.isJam: Boolean
        get() = (this and JAM) != 0

    val Int.isBounce: Boolean
        get() = (this and BOUNCE) != 0

    private const val ANIMATED_FLAGS_MASK =
        ROTATE or RAINBOW or SHAKE or JAM or BOUNCE or APPEAR or LEAVE or SLIDE

    val Int.hasAnimatedEffects: Boolean
        get() = (this and ANIMATED_FLAGS_MASK) != 0
}
