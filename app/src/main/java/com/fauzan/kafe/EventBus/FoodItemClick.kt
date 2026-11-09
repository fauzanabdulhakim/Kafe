package com.fauzan.kafe.EventBus

import com.fauzan.kafe.Model.FoodModel

data class FoodItemClick(
    var isSuccess: Boolean = false,
    var foodModel: FoodModel? = null
)
