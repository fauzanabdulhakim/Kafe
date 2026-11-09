package com.fauzan.kafe.domain.usecase

import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.Model.OrderModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying ValidateOrderPlacementUseCase business and security rules.
 */
class ValidateOrderPlacementUseCaseTest {

    private lateinit var useCase: ValidateOrderPlacementUseCase

    @Before
    fun setUp() {
        useCase = ValidateOrderPlacementUseCase()
    }

    private fun createValidOrder(): OrderModel {
        val cartItems = mutableListOf(
            CartItem(
                foodId = "food_1",
                foodName = "Espresso",
                foodPrice = 20000.0,
                foodQuantity = 2,
                foodExtraPrice = 5000.0,
                foodAddon = "Default",
                foodSize = "Large",
                uid = "user_123"
            )
        )
        // Subtotal = (20000 + 5000) * 2 = 50000
        return OrderModel(
            userId = "user_123",
            userName = "Customer Test",
            userPhone = "+62812345678",
            shippingAddress = "Jl. Merdeka No. 45, Jakarta",
            comment = "No sugar",
            lat = -6.2,
            lng = 106.8,
            cartItemList = cartItems,
            totalPayment = 50000.0,
            discount = 0,
            finalPayment = 50000.0,
            isCod = true,
            transactionId = "Cash On Delivery"
        )
    }

    @Test
    fun validOrder_shouldReturnValid() {
        val order = createValidOrder()
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Valid)
    }

    @Test
    fun emptyUserId_shouldReturnInvalid() {
        val order = createValidOrder().apply { userId = "" }
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("User must be authenticated", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun emptyUserPhone_shouldReturnInvalid() {
        val order = createValidOrder().apply { userPhone = null }
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("User phone number is required", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun emptyCartItems_shouldReturnInvalid() {
        val order = createValidOrder().apply { cartItemList = mutableListOf() }
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("Cart cannot be empty", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun zeroOrNegativeItemQuantity_shouldReturnInvalid() {
        val order = createValidOrder()
        order.cartItemList?.first()?.foodQuantity = 0
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("Invalid item quantity in cart", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun negativeItemPrice_shouldReturnInvalid() {
        val order = createValidOrder()
        order.cartItemList?.first()?.foodPrice = -100.0
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("Item price cannot be negative", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun blankShippingAddress_shouldReturnInvalid() {
        val order = createValidOrder().apply { shippingAddress = "   " }
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("Valid shipping address is required", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun addressNotFound_shouldReturnInvalid() {
        val order = createValidOrder().apply { shippingAddress = "Address not found" }
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("Valid shipping address is required", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun tamperedPrice_shouldReturnInvalid() {
        // Manipulated client-side payment: subtotal is 50000 but client sets payment to 1.0
        val order = createValidOrder().apply {
            finalPayment = 1.0
            totalPayment = 1.0
        }
        val result = useCase(order)
        assertTrue(result is ValidateOrderPlacementUseCase.ValidationResult.Invalid)
        assertEquals("Order payment amount does not match calculated total", (result as ValidateOrderPlacementUseCase.ValidationResult.Invalid).reason)
    }
}
