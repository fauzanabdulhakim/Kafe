package com.fauzan.kafe.Model

import com.fauzan.kafe.Database.CartItem

data class OrderModel(
    var userId: String? = null,
    var userName: String? = null,
    var userPhone: String? = null,
    var shippingAddress: String? = null,
    var comment: String? = null,
    var transactionId: String? = null,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var totalPayment: Double = 0.0,
    var finalPayment: Double = 0.0,
    var isCod: Boolean = false,
    var discount: Int = 0,
    var cartItemList: MutableList<CartItem>? = null,
    var createDate: Long = 0L,
    var orderNumber: String? = null,
    var orderStatus: Int = 0
)
