package com.fauzan.kafe.Model

data class FoodModel(
    var key: String? = null,
    var name: String? = null,
    var image: String? = null,
    var id: String? = null,
    var description: String? = null,
    var price: Long? = 0L,
    var addon: MutableList<AddonModel>? = null,
    var size: MutableList<SizeModel>? = null,
    var ratingValue: Double? = 0.0,
    var ratingCount: Long? = 0L,
    var userSelectedAddon: MutableList<AddonModel>? = null,
    var userSelectedSize: SizeModel? = null,
    var isAvailable: Boolean = true
)
