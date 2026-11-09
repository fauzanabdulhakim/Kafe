package com.fauzan.kafe

import com.fauzan.kafe.Model.FoodModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying FoodModel properties including Out-of-Stock (86-ing) availability status in Kafe.
 */
class FoodModelTest {

    @Test
    fun foodModel_defaultAvailability_shouldBeTrue() {
        val food = FoodModel(name = "Caramel Frappe", price = 35000L)
        assertTrue(food.isAvailable)
    }

    @Test
    fun foodModel_whenOutOfStock_shouldBeFalse() {
        val food = FoodModel(name = "Almond Milk Latte", price = 38000L, isAvailable = false)
        assertFalse(food.isAvailable)
    }
}
