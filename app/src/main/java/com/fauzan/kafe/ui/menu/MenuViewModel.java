package com.fauzan.kafe.ui.menu;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fauzan.kafe.Callback.CategoryCallBackListener;
import com.fauzan.kafe.Common.Common;
import com.fauzan.kafe.Model.CategoryModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuViewModel extends ViewModel implements CategoryCallBackListener {

    private MutableLiveData<List<CategoryModel>> categoryListMultable;
    private MutableLiveData<String> messageError = new MutableLiveData<>();
    private CategoryCallBackListener categoryCallBackListener;

    public MenuViewModel() {
        categoryCallBackListener = this;
    }

    public MutableLiveData<List<CategoryModel>> getCategoryListMultable() {
        if (categoryListMultable == null)
        {
            categoryListMultable = new MutableLiveData<>();
            messageError = new MutableLiveData<>();
            loadCategories();
        }
        return categoryListMultable;
    }

    private void loadCategories() {
        List<CategoryModel> temList = new ArrayList<>();
        DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF);
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot itemSnapShot:dataSnapshot.getChildren())
                {
                    CategoryModel categoryModel = itemSnapShot.getValue(CategoryModel.class);
                    categoryModel.setMenu_id(itemSnapShot.getKey());
                    temList.add(categoryModel);
                }
                categoryCallBackListener.onCategoryLoadSuccess(temList);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                categoryCallBackListener.onCategoryLoadFailed(databaseError.getMessage());

            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onCategoryLoadSuccess(List<CategoryModel> categoryModelList) {
        categoryListMultable.setValue(categoryModelList);

    }

    @Override
    public void onCategoryLoadFailed(String message) {

    }
}