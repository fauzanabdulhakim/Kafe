package com.fauzan.kafe.Common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.fauzan.kafe.Callback.MyButtonClickListener
import java.util.LinkedList
import java.util.Queue

/**
 * Custom swipe-to-reveal action helper for RecyclerView.
 */
abstract class MySwipeHelper(
    context: Context,
    private val recyclerView: RecyclerView,
    var buttonWidth: Int
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private var buttonList: MutableList<MyButton> = mutableListOf()
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            for (button in buttonList) {
                if (button.onClick(e.x, e.y)) {
                    break
                }
            }
            return true
        }
    }
    private val gestureDetector: GestureDetector = GestureDetector(context, gestureListener)
    private var swipePosition = -1
    private var swipeThreshold = 0.5f
    private val buttonBuffer: MutableMap<Int, MutableList<MyButton>> = HashMap()
    private val removeQueue: Queue<Int> = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            return if (contains(element)) false else super.add(element)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = View.OnTouchListener { _, motionEvent ->
        if (swipePosition < 0) return@OnTouchListener false
        val point = Point(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())
        val swipeViewHolder = recyclerView.findViewHolderForAdapterPosition(swipePosition)
        val swipedItem = swipeViewHolder?.itemView ?: return@OnTouchListener false
        val rect = Rect()
        swipedItem.getGlobalVisibleRect(rect)

        if (motionEvent.action == MotionEvent.ACTION_DOWN ||
            motionEvent.action == MotionEvent.ACTION_UP ||
            motionEvent.action == MotionEvent.ACTION_MOVE
        ) {
            if (rect.top < point.y && rect.bottom > point.y) {
                gestureDetector.onTouchEvent(motionEvent)
            } else {
                removeQueue.add(swipePosition)
                swipePosition = -1
            }
        }
        false
    }

    init {
        recyclerView.setOnTouchListener(onTouchListener)
        attachSwipe()
    }

    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    @Synchronized
    private fun recoverSwipedItem() {
        while (!removeQueue.isEmpty()) {
            val pos = removeQueue.poll()
            if (pos != null && pos > -1) {
                recyclerView.adapter?.notifyItemChanged(pos)
            }
        }
    }

    inner class MyButton(
        private val context: Context,
        private val text: String,
        private val textSize: Int,
        private val imageResId: Int,
        private val color: Int,
        private val listener: MyButtonClickListener
    ) {
        private var clickRegion: RectF? = null
        private var pos = 0

        fun onClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                listener.onClick(pos)
                return true
            }
            return false
        }

        fun onDraw(c: Canvas, rectF: RectF, pos: Int) {
            val p = Paint()
            p.color = color
            c.drawRect(rectF, p)

            // Text
            p.color = Color.WHITE
            p.textSize = textSize.toFloat()

            val r = Rect()
            val cHeight = rectF.height()
            val cWidth = rectF.width()
            p.textAlign = Paint.Align.LEFT
            p.getTextBounds(text, 0, text.length, r)
            val x: Float
            val y: Float
            if (imageResId == 0) {
                x = cWidth / 2f - r.width() / 2f - r.left
                y = cHeight / 2f + r.height() / 2f - r.bottom
                c.drawText(text, rectF.left + x, rectF.top + y, p)
            } else {
                val d = ContextCompat.getDrawable(context, imageResId)
                d?.let {
                    val bitmap = drawableToBitmap(it)
                    c.drawBitmap(bitmap, (rectF.left + rectF.right) / 2, (rectF.top + rectF.bottom) / 2, p)
                }
            }
            clickRegion = rectF
            this.pos = pos
        }
    }

    private fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable) return d.bitmap
        val bitmap = Bitmap.createBitmap(
            if (d.intrinsicWidth > 0) d.intrinsicWidth else 1,
            if (d.intrinsicHeight > 0) d.intrinsicHeight else 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bitmap
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition
        if (swipePosition != pos) {
            removeQueue.add(swipePosition)
        }
        swipePosition = pos
        if (buttonBuffer.containsKey(swipePosition)) {
            buttonList = buttonBuffer[swipePosition] ?: mutableListOf()
        } else {
            buttonList.clear()
        }
        buttonBuffer.clear()
        swipeThreshold = 0.5f * buttonList.size * buttonWidth
        recoverSwipedItem()
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = swipeThreshold

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = 0.1f * defaultValue

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float = 5.0f * defaultValue

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView
        if (pos < 0) {
            swipePosition = pos
            return
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                val buffer: MutableList<MyButton>
                if (!buttonBuffer.containsKey(pos)) {
                    buffer = mutableListOf()
                    instantiateMyButton(viewHolder, buffer)
                    buttonBuffer[pos] = buffer
                } else {
                    buffer = buttonBuffer[pos] ?: mutableListOf()
                }
                translationX = dX * buffer.size * buttonWidth / itemView.width
                drawButton(c, itemView, buffer, pos, translationX)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
    }

    private fun drawButton(
        c: Canvas,
        itemView: View,
        buffer: List<MyButton>,
        pos: Int,
        translationX: Float
    ) {
        var right = itemView.right.toFloat()
        val dButtonWidth = -1 * translationX / buffer.size
        for (button in buffer) {
            val left = right - dButtonWidth
            button.onDraw(c, RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()), pos)
            right = left
        }
    }

    abstract fun instantiateMyButton(viewHolder: RecyclerView.ViewHolder, buf: MutableList<MyButton>)
}
