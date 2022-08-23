package com.jensssen.wafflepod.classes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BroadcastReceiver : BroadcastReceiver() {
    internal object BroadcastTypes {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged"
        const val QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged"
        const val METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.
        val timeSentInMs = intent.getLongExtra("timeSent", 0L)
        val action = intent.action
        if (action == BroadcastTypes.METADATA_CHANGED) {
            val trackId = intent.getStringExtra("id")
            val artistName = intent.getStringExtra("artist")
            val albumName = intent.getStringExtra("album")
            val trackName = intent.getStringExtra("track")
            val trackLengthInSec = intent.getIntExtra("length", 0)
            Log.d("BroadcastReceiver", trackLengthInSec.toString())
            // Do something with extracted information...
        } else if (action == BroadcastTypes.PLAYBACK_STATE_CHANGED) {
            val playing = intent.getBooleanExtra("playing", false)
            val positionInS = intent.getIntExtra("playbackPosition", 0)/1000
            MessageHandler.setCurrentPlaybackPosition(positionInS)
            // Do something with extracted information
        } else if (action == BroadcastTypes.QUEUE_CHANGED) {
            // Sent only as a notification, your app may want to respond accordingly.
        }
    }
}