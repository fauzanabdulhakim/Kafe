package com.asksira.loopingviewpager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

/**
 * Looping and auto-scrolling ViewPager widget.
 * Implemented locally to ensure offline reproducibility and eliminate dead JCenter dependencies.
 */
class LoopingViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    private var isAutoScroll = false
    private var interval: Long = 5000
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            adapter?.let { adp ->
                if (adp.count > 1) {
                    val next = (currentItem + 1) % adp.count
                    setCurrentItem(next, true)
                }
            }
            if (isAutoScroll) {
                autoScrollHandler.postDelayed(this, interval)
            }
        }
    }

    fun resumeAutoScroll() {
        isAutoScroll = true
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        autoScrollHandler.postDelayed(autoScrollRunnable, interval)
    }

    fun pauseAutoScroll() {
        isAutoScroll = false
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    fun setInterval(interval: Long) {
        this.interval = interval
    }
}
