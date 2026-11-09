package com.fauzan.kafe.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Callback.CategoryCallBackListener
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuViewModel : ViewModel(), CategoryCallBackListener {

    private val _categoryList = MutableLiveData<List<CategoryModel>>()
    val categoryListMultable: MutableLiveData<List<CategoryModel>>
        get() {
            if (_categoryList.value == null) {
                loadCategories()
            }
            return _categoryList
        }

    private val _messageError = MutableLiveData<String>()
    val messageError: LiveData<String> = _messageError

    private val categoryCallBackListener: CategoryCallBackListener = this

    fun loadCategories() {
        val tempList = ArrayList<CategoryModel>()
        val categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (itemSnapShot in dataSnapshot.children) {
                    val categoryModel = itemSnapShot.getValue(CategoryModel::class.java)
                    if (categoryModel != null) {
                        categoryModel.menu_id = itemSnapShot.key
                        tempList.add(categoryModel)
                    }
                }
                categoryCallBackListener.onCategoryLoadSuccess(tempList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                categoryCallBackListener.onCategoryLoadFailed(databaseError.message)
            }
        })
    }

    override fun onCategoryLoadSuccess(categoryModelList: List<CategoryModel>) {
        _categoryList.value = categoryModelList
    }

    override fun onCategoryLoadFailed(message: String) {
        _messageError.value = message
    }
}
