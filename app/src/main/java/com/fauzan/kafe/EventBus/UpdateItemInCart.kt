package com.fauzan.kafe.EventBus

import com.fauzan.kafe.Database.CartItem

data class UpdateItemInCart(
    var cartItem: CartItem? = null
)
