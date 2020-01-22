package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.BestDealModel;

import java.util.List;

public interface BestDealCallbackListener {
    void onBestDealLoadSuccess(List<BestDealModel> bestDealModels);
    void onBestDealLoadFailed(String message);
}
