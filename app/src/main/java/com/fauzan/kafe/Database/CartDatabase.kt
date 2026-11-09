package com.fauzan.kafe.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(version = 1, entities = [CartItem::class], exportSchema = false)
abstract class CartDatabase : RoomDatabase() {
    abstract fun cartDAO(): CartDAO

    companion object {
        @Volatile
        private var instance: CartDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): CartDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CartDatabase::class.java,
                    "KafeDB2"
                ).build().also { instance = it }
            }
        }
    }
}
