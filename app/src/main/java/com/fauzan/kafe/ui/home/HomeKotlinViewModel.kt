package com.fauzan.kafe.ui.home

import androidx.lifecycle.ViewModel
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.BestDealModel
import com.fauzan.kafe.Model.PopularCategoryModel
import com.fauzan.kafe.domain.model.UiState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modernized Home ViewModel utilizing Kotlin Coroutines, StateFlow, and UiState.
 * Adheres strictly to Clean Architecture and Unidirectional Data Flow (UDF).
 */
class HomeKotlinViewModel : ViewModel() {

    data class HomeFeedData(
        val popularCategories: List<PopularCategoryModel>,
        val bestDeals: List<BestDealModel>
    )

    private val _uiState = MutableStateFlow<UiState<HomeFeedData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeFeedData>> = _uiState.asStateFlow()

    /**
     * Loads live popular categories and best deals from Firebase RTDB.
     */
    fun loadHomeData() {
        _uiState.value = UiState.Loading
        val popularList = mutableListOf<PopularCategoryModel>()
        val bestDealList = mutableListOf<BestDealModel>()

        try {
            val popularRef = FirebaseDatabase.getInstance().getReference(Common.POPULAR_CATEGORY_REF)
            val bestDealRef = FirebaseDatabase.getInstance().getReference(Common.BEST_DEAL_REF)

            popularRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (item in snapshot.children) {
                        val model = item.getValue(PopularCategoryModel::class.java)
                        if (model != null) popularList.add(model)
                    }

                    bestDealRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dealSnapshot: DataSnapshot) {
                            for (dealItem in dealSnapshot.children) {
                                val dealModel = dealItem.getValue(BestDealModel::class.java)
                                if (dealModel != null) bestDealList.add(dealModel)
                            }
                            setFeedData(popularList, bestDealList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            setError(error.message)
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    setError(error.message)
                }
            })
        } catch (e: Exception) {
            setError(e.message ?: "Failed to connect to Firebase")
        }
    }

    /**
     * Updates feed data with provided collections or sets empty state.
     */
    fun setFeedData(
        popular: List<PopularCategoryModel>?,
        bestDeals: List<BestDealModel>?
    ) {
        val popList = popular ?: emptyList()
        val dealList = bestDeals ?: emptyList()

        if (popList.isEmpty() && dealList.isEmpty()) {
            _uiState.value = UiState.Empty
        } else {
            _uiState.value = UiState.Success(HomeFeedData(popList, dealList))
        }
    }

    /**
     * Emits error state with descriptive message.
     */
    fun setError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    /**
     * Resets state to loading before triggering refresh.
     */
    fun refresh() {
        _uiState.value = UiState.Loading
    }
}
