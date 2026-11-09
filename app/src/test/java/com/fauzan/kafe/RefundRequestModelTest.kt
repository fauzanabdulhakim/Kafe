package com.fauzan.kafe

import com.fauzan.kafe.Model.RefundRequestModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying RefundRequestModel properties and PCI DSS compliance.
 */
class RefundRequestModelTest {

    @Test
    fun defaultConstructor_shouldInitializeWithPendingStatus() {
        val model = RefundRequestModel()
        assertEquals("PENDING", model.status)
        assertFalse(model.isDone)
        assertTrue(model.requestDate > 0)
    }

    @Test
    fun parameterizedConstructor_shouldSetAllFieldsCorrectly() {
        val model = RefundRequestModel(
            orderNumber = "ORD-9999",
            name = "Customer Name",
            phone = "+62812345678",
            amount = 50000.0,
            reason = "Accidental duplicate order",
            transactionId = "bt_tx_12345"
        )

        assertEquals("ORD-9999", model.orderNumber)
        assertEquals("Customer Name", model.name)
        assertEquals("+62812345678", model.phone)
        assertEquals(50000.0, model.amount, 0.001)
        assertEquals("Accidental duplicate order", model.reason)
        assertEquals("bt_tx_12345", model.transactionId)
        assertEquals("PENDING", model.status)
        assertFalse(model.isDone)
    }

    @Test
    fun pciDssVerification_shouldNotContainSensitiveCardFields() {
        // Reflection check ensuring no raw cardholder fields exist in class definition
        val fields = RefundRequestModel::class.java.declaredFields
        for (field in fields) {
            val name = field.name.lowercase()
            assertFalse("Field $name violates PCI DSS", name.contains("cardnumber"))
            assertFalse("Field $name violates PCI DSS", name.contains("cvv"))
            assertFalse("Field $name violates PCI DSS", name.contains("cardexp"))
        }
    }
}
