package com.fauzan.kafe

import com.fauzan.kafe.Model.AddonModel
import com.fauzan.kafe.Model.SizeModel
import com.fauzan.kafe.domain.calculator.PriceCalculator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying PriceCalculator business rules, currency formatting, and totals.
 */
class PriceCalculatorTest {

    private lateinit var calculator: PriceCalculator

    @Before
    fun setUp() {
        calculator = PriceCalculator()
    }

    @Test
    fun formatPrice_whenZero_shouldReturnZeroFormatted() {
        assertEquals("0,00", calculator.formatPrice(0.0))
    }

    @Test
    fun formatPrice_whenPositiveAmount_shouldFormatWithCommas() {
        assertEquals("25,000", calculator.formatPrice(25000.0))
        assertEquals("1,500,000", calculator.formatPrice(1500000.0))
    }

    @Test
    fun calculateExtraPrice_whenBothNull_shouldReturnZero() {
        val extra = calculator.calculateExtraPrice(null, null)
        assertEquals(0.0, extra, 0.001)
    }

    @Test
    fun calculateExtraPrice_whenOnlySizeSelected_shouldReturnSizePrice() {
        val size = SizeModel(name = "Large", price = 5000L)
        val extra = calculator.calculateExtraPrice(size, null)
        assertEquals(5000.0, extra, 0.001)
    }

    @Test
    fun calculateExtraPrice_whenOnlyAddonsSelected_shouldSumAddonPrices() {
        val addon1 = AddonModel(name = "Extra Shot", price = 3000L)
        val addon2 = AddonModel(name = "Caramel Drizzle", price = 2000L)

        val extra = calculator.calculateExtraPrice(null, listOf(addon1, addon2))
        assertEquals(5000.0, extra, 0.001)
    }

    @Test
    fun calculateExtraPrice_whenBothSizeAndAddonsSelected_shouldSumBoth() {
        val size = SizeModel(name = "Grande", price = 7000L)
        val addon = AddonModel(name = "Oat Milk", price = 6000L)

        val extra = calculator.calculateExtraPrice(size, listOf(addon))
        assertEquals(13000.0, extra, 0.001)
    }

    @Test
    fun calculateLineItemTotal_shouldMultiplyBasePlusExtraByQuantity() {
        val lineTotal = calculator.calculateLineItemTotal(20000.0, 5000.0, 3)
        assertEquals(75000.0, lineTotal, 0.001)
    }

    @Test
    fun calculateLineItemTotal_whenQuantityZeroOrNegative_shouldReturnZero() {
        assertEquals(0.0, calculator.calculateLineItemTotal(20000.0, 5000.0, 0), 0.001)
        assertEquals(0.0, calculator.calculateLineItemTotal(20000.0, 5000.0, -2), 0.001)
    }
}
