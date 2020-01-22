package com.fauzan.kafe.Callback;

import com.fauzan.kafe.Model.BestDealModel;
import com.fauzan.kafe.Model.CategoryModel;

import java.util.List;

public interface CategoryCallBackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModelList);
    void onCategoryLoadFailed(String message);
}
