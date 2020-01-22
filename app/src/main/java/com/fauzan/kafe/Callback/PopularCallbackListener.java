package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.PopularCategoryModel;

import java.util.List;

public interface PopularCallbackListener {
    void onPopularLoadSuccess(List<PopularCategoryModel> popularCategoryModels);
    void onPopularLoadFailed(String message);
}
