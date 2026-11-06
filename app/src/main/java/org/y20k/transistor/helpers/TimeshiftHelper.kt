/*
 * TimeshiftHelper.kt
 * Implements the TimeshiftHelper object
 * A TimeshiftHelper provides methods to inspect and control the live window of a stream that can be rewound (e.g. HLS with a DVR window)
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import org.y20k.transistor.Keys
import java.util.Calendar


/*
 * TimeshiftHelper object
 */
object TimeshiftHelper {

    /* Define log tag */
    private val TAG: String = TimeshiftHelper::class.java.simpleName


    /* Describes where playback is within the live window of the current stream */
    data class TimeshiftState(
        val positionMs: Long,       // current playback position within the live window
        val livePositionMs: Long,   // position of the live edge (= default live position) within the live window
        val behindLiveMs: Long,     // how far playback lags behind the live edge
        val isAtLiveEdge: Boolean   // true if playback lags less than Keys.TIMESHIFT_LIVE_EDGE_TOLERANCE behind live
    )


    /* Returns the live window of the current media item - or null if there is none */
    private fun getCurrentWindow(player: Player): Timeline.Window? {
        if (!player.isCommandAvailable(Player.COMMAND_GET_TIMELINE)) return null
        val timeline: Timeline = player.currentTimeline
        val index: Int = player.currentMediaItemIndex
        if (timeline.isEmpty || index < 0 || index >= timeline.windowCount) return null
        return timeline.getWindow(index, Timeline.Window())
    }


    /* Checks if the current stream can be rewound: a seekable live stream with a live window that is long enough */
    fun isAvailable(player: Player): Boolean {
        val window: Timeline.Window = getCurrentWindow(player) ?: return false
        return window.isLive() && window.isSeekable && window.durationMs != C.TIME_UNSET && window.durationMs >= Keys.TIMESHIFT_MIN_WINDOW
    }


    /* Returns the current timeshift state - or null if the current stream can not be rewound */
    fun getState(player: Player): TimeshiftState? {
        val window: Timeline.Window = getCurrentWindow(player) ?: return null
        if (!(window.isLive() && window.isSeekable && window.durationMs != C.TIME_UNSET && window.durationMs >= Keys.TIMESHIFT_MIN_WINDOW)) return null
        // the default position of a live window is the live edge minus the target live offset
        val livePositionMs: Long = if (window.defaultPositionMs != C.TIME_UNSET) window.defaultPositionMs else window.durationMs
        val positionMs: Long = player.currentPosition.coerceIn(0L, livePositionMs)
        // prefer the wall clock based live offset (needs EXT-X-PROGRAM-DATE-TIME) - fall back to the distance to the default position
        val liveOffsetMs: Long = player.currentLiveOffset
        val targetOffsetMs: Long = window.liveConfiguration?.targetOffsetMs ?: C.TIME_UNSET
        val behindLiveMs: Long = if (liveOffsetMs != C.TIME_UNSET && targetOffsetMs != C.TIME_UNSET) {
            (liveOffsetMs - targetOffsetMs).coerceAtLeast(0L)
        } else {
            (livePositionMs - positionMs).coerceAtLeast(0L)
        }
        return TimeshiftState(
            positionMs = positionMs,
            livePositionMs = livePositionMs,
            behindLiveMs = behindLiveMs,
            isAtLiveEdge = behindLiveMs < Keys.TIMESHIFT_LIVE_EDGE_TOLERANCE
        )
    }


    /* Skips back within the live window */
    fun skipBack(player: Player) {
        if (!isAvailable(player)) return
        player.seekTo((player.currentPosition - Keys.TIMESHIFT_SEEK_INCREMENT).coerceAtLeast(0L))
    }


    /* Skips forward within the live window - jumps to live, if the live edge is reached */
    fun skipForward(player: Player) {
        val state: TimeshiftState = getState(player) ?: return
        val target: Long = state.positionMs + Keys.TIMESHIFT_SEEK_INCREMENT
        if (target >= state.livePositionMs - Keys.TIMESHIFT_LIVE_EDGE_TOLERANCE) {
            goLive(player)
        } else {
            player.seekTo(target)
        }
    }


    /* Seeks to the given position within the live window - jumps to live, if the position is at the live edge */
    fun seekTo(player: Player, positionMs: Long) {
        val state: TimeshiftState = getState(player) ?: return
        if (positionMs >= state.livePositionMs - Keys.TIMESHIFT_LIVE_EDGE_TOLERANCE) {
            goLive(player)
        } else {
            player.seekTo(positionMs.coerceAtLeast(0L))
        }
    }


    /* Skips back to the start of the hour that is currently playing (e.g. from 12:33 to 12:00) - from the start of an hour it skips to the start of the previous hour */
    fun skipToStartOfHour(player: Player) {
        val state: TimeshiftState = getState(player) ?: return
        val window: Timeline.Window = getCurrentWindow(player) ?: return
        val positionMs: Long = player.currentPosition.coerceAtLeast(0L)
        // wall clock time of the current playback position - prefer the stream's own clock (EXT-X-PROGRAM-DATE-TIME), fall back to the device clock
        val currentUnixMs: Long = if (window.windowStartTimeMs != C.TIME_UNSET) {
            window.windowStartTimeMs + positionMs
        } else {
            System.currentTimeMillis() - state.behindLiveMs
        }
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = currentUnixMs
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var hourStartUnixMs: Long = calendar.timeInMillis
        if (currentUnixMs - hourStartUnixMs < Keys.TIMESHIFT_LIVE_EDGE_TOLERANCE) {
            // already (almost) at the start of the hour - go to the previous one
            hourStartUnixMs -= Keys.ONE_HOUR
        }
        // the live window may not reach back that far - then the oldest available position is used
        player.seekTo((positionMs - (currentUnixMs - hourStartUnixMs)).coerceAtLeast(0L))
    }


    /* Jumps to the live edge */
    fun goLive(player: Player) {
        player.seekToDefaultPosition()
    }

}
