package com.fauzan.kafe.Callback

import com.fauzan.kafe.Model.PopularCategoryModel

interface PopularCallbackListener {
    fun onPopularLoadSuccess(popularCategoryModels: List<PopularCategoryModel>)
    fun onPopularLoadFailed(message: String)
}
