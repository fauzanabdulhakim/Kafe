package com.fauzan.kafe.EventBus

import com.fauzan.kafe.Model.CategoryModel

data class CategoryClick(
    var isSuccess: Boolean = false,
    var categoryModel: CategoryModel? = null
)
