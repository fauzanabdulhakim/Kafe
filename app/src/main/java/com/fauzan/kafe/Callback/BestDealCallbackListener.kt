package com.fauzan.kafe.Callback

import com.fauzan.kafe.Model.BestDealModel

interface BestDealCallbackListener {
    fun onBestDealLoadSuccess(bestDealModels: List<BestDealModel>)
    fun onBestDealLoadFailed(message: String)
}
