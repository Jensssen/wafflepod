package com.jensssen.wafflepod.classes

import android.os.Handler
import android.util.Log
import android.widget.SeekBar
import com.jensssen.wafflepod.Adapter.MessageAdapter

class TrackProgressBar(private val seekBar: SeekBar, private var adapter: MessageAdapter, private val seekStopListener: (Long) -> Unit) {
    private val handler: Handler

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            MessageHandler.setCurrentPlaybackPosition(progress/1000)
            // calculate how many seconds one interval has
            val totalLength = MessageHandler.getCurrentTrackLength()
            val currentPosition = progress/1000
            val intervalLength = totalLength/24.0
            val secionIdx = (currentPosition / intervalLength).toInt()

            val leftBound = intervalLength*secionIdx
            val rightBound = intervalLength*secionIdx+intervalLength
            Log.d("progress","${secionIdx}: Position ${progress/1000} is between $leftBound and $rightBound")
            MessageHandler.updateFinalMessageList(secionIdx, leftBound.toInt(), rightBound.toInt(), adapter)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            seekStopListener.invoke(seekBar.progress.toLong())
        }
    }

    init {
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        handler = Handler()
    }

    private val seekUpdateRunnable = object : Runnable {
        override fun run() {
            val progress = seekBar.progress
            seekBar.progress = progress + LOOP_DURATION
            handler.postDelayed(this, LOOP_DURATION.toLong())
        }
    }

    fun setDuration(duration: Long) {
        seekBar.max = duration.toInt()
    }

    fun update(progress: Long) {
        seekBar.progress = progress.toInt()
    }

    fun pause() {
        handler.removeCallbacks(seekUpdateRunnable)
    }

    fun unpause() {
        handler.removeCallbacks(seekUpdateRunnable)
        handler.postDelayed(seekUpdateRunnable, LOOP_DURATION.toLong())
    }

    companion object {
        private const val LOOP_DURATION = 500
    }
}