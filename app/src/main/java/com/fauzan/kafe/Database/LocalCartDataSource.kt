package com.fauzan.kafe.Database

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

class LocalCartDataSource(private val cartDAO: CartDAO) : CartDataSource {

    override fun getAllCart(uid: String): Flowable<List<CartItem>> = cartDAO.getAllCart(uid)

    override fun countItemInCart(uid: String): Single<Int> = cartDAO.countItemInCart(uid)

    override fun sumPriceInCart(uid: String): Single<Double> = cartDAO.sumPriceInCart(uid)

    override fun getItemCart(foodId: String, uid: String): Single<CartItem> = cartDAO.getItemCart(foodId, uid)

    override fun insertOrReplaceAll(vararg cartItems: CartItem): Completable = cartDAO.insertOrReplaceAll(*cartItems)

    override fun updateCartItems(cartItem: CartItem): Single<Int> = cartDAO.updateCartItems(cartItem)

    override fun deleteCartItem(cartItem: CartItem): Single<Int> = cartDAO.deleteCartItem(cartItem)

    override fun cleanCart(uid: String): Single<Int> = cartDAO.cleanCart(uid)

    override fun getItemWithAllOptionsInCart(
        uid: String,
        foodId: String,
        foodSize: String,
        foodAddon: String
    ): Single<CartItem> = cartDAO.getItemWithAllOptionsInCart(uid, foodId, foodSize, foodAddon)
}
