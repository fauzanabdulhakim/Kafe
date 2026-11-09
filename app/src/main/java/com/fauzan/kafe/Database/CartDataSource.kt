package com.fauzan.kafe.Database

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface CartDataSource {
    fun getAllCart(uid: String): Flowable<List<CartItem>>
    fun countItemInCart(uid: String): Single<Int>
    fun sumPriceInCart(uid: String): Single<Double>
    fun getItemCart(foodId: String, uid: String): Single<CartItem>
    fun insertOrReplaceAll(vararg cartItems: CartItem): Completable
    fun updateCartItems(cartItem: CartItem): Single<Int>
    fun deleteCartItem(cartItem: CartItem): Single<Int>
    fun cleanCart(uid: String): Single<Int>
    fun getItemWithAllOptionsInCart(uid: String, foodId: String, foodSize: String, foodAddon: String): Single<CartItem>
}
