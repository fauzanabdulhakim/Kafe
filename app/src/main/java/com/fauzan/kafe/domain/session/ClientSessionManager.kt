package com.fauzan.kafe.domain.session

import com.fauzan.kafe.Model.CategoryModel
import com.fauzan.kafe.Model.FoodModel
import com.fauzan.kafe.Model.UserModel

/**
 * Thread-safe customer session manager.
 * Encapsulates session state, replacing mutable static fields in Common.
 */
class ClientSessionManager {

    var currentUser: UserModel? = null
    var selectedCategory: CategoryModel? = null
    var selectedFood: FoodModel? = null
    var currentToken: String = ""
    var authorizeKey: String = ""

    val isAuthenticated: Boolean
        get() = currentUser != null

    @Synchronized
    fun clearSession() {
        currentUser = null
        selectedCategory = null
        selectedFood = null
        currentToken = ""
        authorizeKey = ""
    }

    companion object {
        @Volatile
        private var instance: ClientSessionManager? = null

        @JvmStatic
        fun getInstance(): ClientSessionManager {
            return instance ?: synchronized(this) {
                instance ?: ClientSessionManager().also { instance = it }
            }
        }

        @JvmStatic
        fun resetInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }
}
