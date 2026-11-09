package com.fauzan.kafe.domain.usecase

import com.fauzan.kafe.Database.CartItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CalculateCartTotalUseCase business logic and edge cases.
 */
class CalculateCartTotalUseCaseTest {

    private lateinit var useCase: CalculateCartTotalUseCase

    @Before
    fun setUp() {
        useCase = CalculateCartTotalUseCase()
    }

    @Test
    fun `invoke with null or empty list should return zero summary`() {
        val summaryNull = useCase(null)
        assertEquals(0.0, summaryNull.subtotal, 0.001)
        assertEquals(0.0, summaryNull.finalTotal, 0.001)
        assertEquals(0, summaryNull.totalItemCount)

        val summaryEmpty = useCase(emptyList())
        assertEquals(0.0, summaryEmpty.subtotal, 0.001)
        assertEquals(0.0, summaryEmpty.finalTotal, 0.001)
        assertEquals(0, summaryEmpty.totalItemCount)
    }

    @Test
    fun `invoke with multiple items should compute correct subtotal and count`() {
        val item1 = CartItem().apply {
            foodId = "1"
            foodName = "Caffe Latte"
            foodPrice = 25000.0
            foodExtraPrice = 5000.0 // Large size
            foodQuantity = 2 // (25000 + 5000) * 2 = 60000
        }

        val item2 = CartItem().apply {
            foodId = "2"
            foodName = "Croissant"
            foodPrice = 15000.0
            foodExtraPrice = 0.0
            foodQuantity = 1 // 15000 * 1 = 15000
        }

        val summary = useCase(listOf(item1, item2))

        assertEquals(75000.0, summary.subtotal, 0.001)
        assertEquals(0.0, summary.discount, 0.001)
        assertEquals(75000.0, summary.finalTotal, 0.001)
        assertEquals(3, summary.totalItemCount)
    }

    @Test
    fun `invoke with discount percentage should apply discount correctly`() {
        val item = CartItem().apply {
            foodPrice = 100000.0
            foodExtraPrice = 0.0
            foodQuantity = 1
        }

        val summary = useCase(listOf(item), discountPercentage = 20.0)

        assertEquals(100000.0, summary.subtotal, 0.001)
        assertEquals(20000.0, summary.discount, 0.001)
        assertEquals(80000.0, summary.finalTotal, 0.001)
    }

    @Test
    fun `invoke with extreme discount percentage should clamp to 100 percent`() {
        val item = CartItem().apply {
            foodPrice = 50000.0
            foodExtraPrice = 0.0
            foodQuantity = 1
        }

        val summary = useCase(listOf(item), discountPercentage = 150.0)

        assertEquals(50000.0, summary.subtotal, 0.001)
        assertEquals(50000.0, summary.discount, 0.001)
        assertEquals(0.0, summary.finalTotal, 0.001)
    }
}
