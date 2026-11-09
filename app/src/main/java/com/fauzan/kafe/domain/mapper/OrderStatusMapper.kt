package com.fauzan.kafe.domain.mapper

/**
 * Pure Kotlin Domain mapper for customer order status codes and day names.
 */
class OrderStatusMapper {

    companion object {
        const val STATUS_CANCELLED: Int = -1
        const val STATUS_PLACED: Int = 0
        const val STATUS_SHIPPING: Int = 1
        const val STATUS_SHIPPED: Int = 2
    }

    fun convertStatusToText(orderStatus: Int): String {
        return when (orderStatus) {
            STATUS_PLACED -> "Placed"
            STATUS_SHIPPING -> "Shipping"
            STATUS_SHIPPED -> "Shipped"
            STATUS_CANCELLED -> "Cancelled"
            else -> "Unk"
        }
    }

    fun getDateOfWeek(dayIndex: Int): String {
        return when (dayIndex) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unk"
        }
    }
}
