package com.fauzan.kafe.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface CartDAO {
    @Query("SELECT * FROM Cart WHERE uid=:uid")
    fun getAllCart(uid: String): Flowable<List<CartItem>>

    @Query("SELECT SUM(foodQuantity) from Cart WHERE uid=:uid")
    fun countItemInCart(uid: String): Single<Int>

    @Query("SELECT SUM((foodPrice+foodExtraPrice) * foodQuantity) FROM Cart WHERE uid=:uid")
    fun sumPriceInCart(uid: String): Single<Double>

    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND uid=:uid")
    fun getItemCart(foodId: String, uid: String): Single<CartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceAll(vararg cartItems: CartItem): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCartItems(cartItem: CartItem): Single<Int>

    @Delete
    fun deleteCartItem(cartItem: CartItem): Single<Int>

    @Query("DELETE FROM cart WHERE uid=:uid")
    fun cleanCart(uid: String): Single<Int>

    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND uid=:uid AND foodSize=:foodSize AND foodAddon=:foodAddon")
    fun getItemWithAllOptionsInCart(uid: String, foodId: String, foodSize: String, foodAddon: String): Single<CartItem>
}
