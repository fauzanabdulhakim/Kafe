package com.fauzan.kafe

import com.fauzan.kafe.domain.mapper.OrderStatusMapper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying customer-facing order status texts and day mapping.
 */
class OrderStatusMapperTest {

    private lateinit var mapper: OrderStatusMapper

    @Before
    fun setUp() {
        mapper = OrderStatusMapper()
    }

    @Test
    fun convertStatusToText_shouldMapKnownCodesCorrectly() {
        assertEquals("Placed", mapper.convertStatusToText(0))
        assertEquals("Shipping", mapper.convertStatusToText(1))
        assertEquals("Shipped", mapper.convertStatusToText(2))
        assertEquals("Cancelled", mapper.convertStatusToText(-1))
    }

    @Test
    fun convertStatusToText_whenUnknownCode_shouldReturnUnk() {
        assertEquals("Unk", mapper.convertStatusToText(99))
        assertEquals("Unk", mapper.convertStatusToText(-5))
    }

    @Test
    fun getDateOfWeek_shouldMapDaysCorrectly() {
        assertEquals("Monday", mapper.getDateOfWeek(1))
        assertEquals("Tuesday", mapper.getDateOfWeek(2))
        assertEquals("Wednesday", mapper.getDateOfWeek(3))
        assertEquals("Thursday", mapper.getDateOfWeek(4))
        assertEquals("Friday", mapper.getDateOfWeek(5))
        assertEquals("Saturday", mapper.getDateOfWeek(6))
        assertEquals("Sunday", mapper.getDateOfWeek(7))
    }

    @Test
    fun getDateOfWeek_whenInvalidIndex_shouldReturnUnk() {
        assertEquals("Unk", mapper.getDateOfWeek(0))
        assertEquals("Unk", mapper.getDateOfWeek(8))
        assertEquals("Unk", mapper.getDateOfWeek(-1))
    }
}
