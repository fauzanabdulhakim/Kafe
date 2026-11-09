package com.fauzan.kafe.ui.home

import com.fauzan.kafe.Model.BestDealModel
import com.fauzan.kafe.Model.PopularCategoryModel
import com.fauzan.kafe.domain.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeKotlinViewModelTest {

    private lateinit var viewModel: HomeKotlinViewModel

    @Before
    fun setUp() {
        viewModel = HomeKotlinViewModel()
    }

    @Test
    fun `initial state should be Loading`() {
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `setFeedData with items should transition state to Success`() {
        val category = PopularCategoryModel().apply {
            name = "Coffee"
        }
        val deal = BestDealModel().apply {
            name = "Buy 1 Get 1"
        }

        viewModel.setFeedData(listOf(category), listOf(deal))

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(1, data.popularCategories.size)
        assertEquals(1, data.bestDeals.size)
    }

    @Test
    fun `setFeedData with empty collections should transition state to Empty`() {
        viewModel.setFeedData(emptyList(), emptyList())
        assertTrue(viewModel.uiState.value is UiState.Empty)
    }

    @Test
    fun `setError should transition state to Error`() {
        viewModel.setError("Network timeout")
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Network timeout", (state as UiState.Error).message)
    }

    @Test
    fun `refresh should reset state to Loading`() {
        viewModel.setError("Something went wrong")
        viewModel.refresh()
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }
}
