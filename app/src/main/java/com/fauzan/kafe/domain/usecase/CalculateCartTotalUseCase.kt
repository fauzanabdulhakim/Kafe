package com.fauzan.kafe.domain.usecase

import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.domain.calculator.PriceCalculator

/**
 * Domain UseCase calculating cart subtotal, taxes, and final payable amount.
 * Encapsulates core business logic with zero Android framework dependencies.
 */
class CalculateCartTotalUseCase(
    private val priceCalculator: PriceCalculator = PriceCalculator()
) {

    data class CartSummary(
        val subtotal: Double,
        val discount: Double,
        val finalTotal: Double,
        val totalItemCount: Int
    )

    /**
     * Executes pricing calculation on the provided cart items.
     *
     * @param items active list of CartItems
     * @param discountPercentage discount percentage (0 to 100)
     * @return CartSummary with calculated totals
     */
    operator fun invoke(items: List<CartItem>?, discountPercentage: Double = 0.0): CartSummary {
        if (items.isNullOrEmpty()) {
            return CartSummary(
                subtotal = 0.0,
                discount = 0.0,
                finalTotal = 0.0,
                totalItemCount = 0
            )
        }

        var subtotal = 0.0
        var totalCount = 0

        for (item in items) {
            val basePrice = item.foodPrice ?: 0.0
            val extraPrice = item.foodExtraPrice ?: 0.0
            val qty = item.foodQuantity

            subtotal += priceCalculator.calculateLineItemTotal(basePrice, extraPrice, qty)
            totalCount += qty
        }

        val clampedDiscount = discountPercentage.coerceIn(0.0, 100.0)
        val discountAmount = subtotal * (clampedDiscount / 100.0)
        val finalTotal = (subtotal - discountAmount).coerceAtLeast(0.0)

        return CartSummary(
            subtotal = subtotal,
            discount = discountAmount,
            finalTotal = finalTotal,
            totalItemCount = totalCount
        )
    }
}
