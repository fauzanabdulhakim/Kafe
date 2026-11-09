package com.fauzan.kafe.Model

data class CategoryModel(
    var menu_id: String? = null,
    var name: String? = null,
    var image: String? = null,
    var foods: MutableList<FoodModel>? = null
)
