package com.fauzan.kafe.Callback

import com.fauzan.kafe.Model.CategoryModel

interface CategoryCallBackListener {
    fun onCategoryLoadSuccess(categoryModelList: List<CategoryModel>)
    fun onCategoryLoadFailed(message: String)
}
