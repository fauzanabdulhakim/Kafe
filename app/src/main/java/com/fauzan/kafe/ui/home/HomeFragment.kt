package com.fauzan.kafe.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fauzan.kafe.Adapter.MyBestDealAdapter
import com.fauzan.kafe.Adapter.MyPopularCategoriesAdapter
import com.fauzan.kafe.R
import com.fauzan.kafe.databinding.FragmentHomeBinding
import com.fauzan.kafe.domain.model.UiState

/**
 * Customer Home screen displaying popular categories and best deals.
 * Built with Kotlin, ViewBinding, and reactive StateFlow-as-LiveData observation.
 */
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeKotlinViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var layoutAnimationController: LayoutAnimationController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeKotlinViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        initViews()
        observeViewModel()
        homeViewModel.loadHomeData()

        return binding.root
    }

    private fun initViews() {
        context?.let { ctx ->
            layoutAnimationController = AnimationUtils.loadLayoutAnimation(ctx, R.anim.layout_item_from_left)
            binding.recyclerPopular.setHasFixedSize(true)
            binding.recyclerPopular.layoutManager = LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false)
        }
    }

    private fun observeViewModel() {
        homeViewModel.uiState.asLiveData().observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    // Loading visual state placeholder
                }
                is UiState.Success -> {
                    context?.let { ctx ->
                        val popularAdapter = MyPopularCategoriesAdapter(ctx, state.data.popularCategories)
                        binding.recyclerPopular.adapter = popularAdapter
                        layoutAnimationController?.let {
                            binding.recyclerPopular.layoutAnimation = it
                        }

                        val bestDealAdapter = MyBestDealAdapter(ctx, state.data.bestDeals, true)
                        binding.viewpager.adapter = bestDealAdapter
                    }
                }
                is UiState.Empty -> {
                    // Handle empty feed
                }
                is UiState.Error -> {
                    context?.let { ctx ->
                        Toast.makeText(ctx, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is UiState.Idle -> {
                    // Initial idle state
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.viewpager?.resumeAutoScroll()
    }

    override fun onPause() {
        _binding?.viewpager?.pauseAutoScroll()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
