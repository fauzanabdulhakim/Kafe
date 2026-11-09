package com.fauzan.kafe.domain.usecase

import com.fauzan.kafe.Model.OrderModel
import kotlin.math.abs

/**
 * Domain UseCase verifying integrity, completeness, and pricing consistency
 * before an order is persisted or charged.
 * Prevents client-authoritative price tampering and invalid orders.
 */
class ValidateOrderPlacementUseCase(
    private val calculateCartTotalUseCase: CalculateCartTotalUseCase = CalculateCartTotalUseCase()
) {

    sealed interface ValidationResult {
        object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    operator fun invoke(order: OrderModel?): ValidationResult {
        if (order == null) {
            return ValidationResult.Invalid("Order cannot be null")
        }

        if (order.userId.isNullOrBlank()) {
            return ValidationResult.Invalid("User must be authenticated")
        }

        if (order.userPhone.isNullOrBlank()) {
            return ValidationResult.Invalid("User phone number is required")
        }

        val items = order.cartItemList
        if (items.isNullOrEmpty()) {
            return ValidationResult.Invalid("Cart cannot be empty")
        }

        for (item in items) {
            if (item.foodId.isBlank()) {
                return ValidationResult.Invalid("Invalid food item ID")
            }
            if (item.foodQuantity <= 0) {
                return ValidationResult.Invalid("Invalid item quantity in cart")
            }
            if ((item.foodPrice ?: 0.0) < 0.0 || (item.foodExtraPrice ?: 0.0) < 0.0) {
                return ValidationResult.Invalid("Item price cannot be negative")
            }
        }

        val address = order.shippingAddress?.trim()
        if (address.isNullOrBlank() || address == "Address not found") {
            return ValidationResult.Invalid("Valid shipping address is required")
        }

        val summary = calculateCartTotalUseCase(items, order.discount.toDouble())

        if (summary.finalTotal <= 0.0) {
            return ValidationResult.Invalid("Calculated order total must be greater than zero")
        }

        if (abs(order.finalPayment - summary.finalTotal) > 0.01) {
            return ValidationResult.Invalid("Order payment amount does not match calculated total")
        }

        return ValidationResult.Valid
    }
}
