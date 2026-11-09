package com.cepheuen.elegantnumberbutton.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Quantity selector widget providing decrement, count display, and increment controls.
 * Clean, lightweight local replacement for unmaintained JCenter dependency.
 */
class ElegantNumberButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun interface OnValueChangeListener {
        fun onValueChange(view: ElegantNumberButton, oldValue: Int, newValue: Int)
    }

    private var currentNumber = 1
    private var minNumber = 1
    private var maxNumber = 99
    private lateinit var txtNumber: TextView
    private lateinit var btnMinus: Button
    private lateinit var btnPlus: Button
    private var onValueChangeListener: OnValueChangeListener? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        btnMinus = Button(context).apply {
            text = "-"
            textSize = 16f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        txtNumber = TextView(context).apply {
            text = currentNumber.toString()
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0)
        }

        btnPlus = Button(context).apply {
            text = "+"
            textSize = 16f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        btnMinus.setOnClickListener {
            if (currentNumber > minNumber) {
                val oldVal = currentNumber
                currentNumber--
                txtNumber.text = currentNumber.toString()
                onValueChangeListener?.onValueChange(this, oldVal, currentNumber)
            }
        }

        btnPlus.setOnClickListener {
            if (currentNumber < maxNumber) {
                val oldVal = currentNumber
                currentNumber++
                txtNumber.text = currentNumber.toString()
                onValueChangeListener?.onValueChange(this, oldVal, currentNumber)
            }
        }

        addView(btnMinus)
        addView(txtNumber)
        addView(btnPlus)
    }

    var number: String
        get() = currentNumber.toString()
        set(value) {
            try {
                val parsed = value.toInt()
                currentNumber = parsed.coerceIn(minNumber, maxNumber)
                if (::txtNumber.isInitialized) {
                    txtNumber.text = currentNumber.toString()
                }
            } catch (ignored: NumberFormatException) {
            }
        }

    fun setRange(min: Int, max: Int) {
        this.minNumber = min
        this.maxNumber = max
        number = currentNumber.toString()
    }

    fun setOnValueChangeListener(listener: OnValueChangeListener?) {
        this.onValueChangeListener = listener
    }
}
