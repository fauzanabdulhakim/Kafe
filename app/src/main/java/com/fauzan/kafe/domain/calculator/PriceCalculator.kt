package com.fauzan.kafe.domain.calculator

import com.fauzan.kafe.Model.AddonModel
import com.fauzan.kafe.Model.SizeModel
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Pure Kotlin Domain calculator for item pricing and add-on costs.
 * Free of Android SDK dependencies to guarantee instantaneous JVM unit testing.
 */
class PriceCalculator {

    /**
     * Formats price value with comma grouping.
     */
    fun formatPrice(price: Double): String {
        return if (price != 0.0) {
            val df = DecimalFormat("#,##0")
            df.roundingMode = RoundingMode.UP
            df.format(price).replace(".", ",")
        } else {
            "0,00"
        }
    }

    /**
     * Computes cumulative additional price from selected size and active add-ons.
     */
    fun calculateExtraPrice(userSelectedSize: SizeModel?, userSelectedAddon: List<AddonModel>?): Double {
        var result = 0.0
        if (userSelectedSize == null && userSelectedAddon == null) {
            return 0.0
        } else if (userSelectedSize == null) {
            userSelectedAddon?.forEach { addon ->
                result += (addon.price ?: 0L)
            }
            return result
        } else if (userSelectedAddon == null) {
            return (userSelectedSize.price ?: 0L) * 1.0
        } else {
            result = (userSelectedSize.price ?: 0L) * 1.0
            userSelectedAddon.forEach { addon ->
                result += (addon.price ?: 0L)
            }
            return result
        }
    }

    /**
     * Computes the line item total (base price + extra price) multiplied by quantity.
     */
    fun calculateLineItemTotal(basePrice: Double, extraPrice: Double, quantity: Int): Double {
        if (quantity <= 0) return 0.0
        return (basePrice + extraPrice) * quantity
    }
}
