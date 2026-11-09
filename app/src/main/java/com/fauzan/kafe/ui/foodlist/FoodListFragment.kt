package com.fauzan.kafe.ui.foodlist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.fauzan.kafe.Adapter.MyFoodListAdapter
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.EventBus.MenuItemBack
import com.fauzan.kafe.Model.FoodModel
import com.fauzan.kafe.R
import com.fauzan.kafe.databinding.FragmentFoodListBinding
import org.greenrobot.eventbus.EventBus
import java.util.Locale

/**
 * Fragment displaying food items belonging to a selected category.
 * Migrated to Android Jetpack ViewBinding with proper lifecycle nullification.
 */
class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel
    private var _binding: FragmentFoodListBinding? = null
    private val binding get() = _binding!!
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyFoodListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        foodListViewModel = ViewModelProvider(this).get(FoodListViewModel::class.java)
        _binding = FragmentFoodListBinding.inflate(inflater, container, false)

        initViews()

        foodListViewModel.mutableLiveDataFoodList.observe(viewLifecycleOwner) { foodModels ->
            if (foodModels != null && context != null && _binding != null) {
                adapter = MyFoodListAdapter(requireContext(), foodModels)
                binding.recyclerFoodList.adapter = adapter
                layoutAnimationController?.let { binding.recyclerFoodList.layoutAnimation = it }
            }
        }

        return binding.root
    }

    private fun initViews() {
        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            Common.categorySelected?.let { cat ->
                ab.title = cat.name
            }
        }

        setHasOptionsMenu(true)

        val ctx = context ?: return
        binding.recyclerFoodList.setHasFixedSize(true)
        binding.recyclerFoodList.layoutManager = LinearLayoutManager(ctx)
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(ctx, R.anim.layout_item_from_left)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)

        val menuItem = menu.findItem(R.id.action_search)
        val act = activity
        if (act != null && menuItem != null) {
            val searchManager = act.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
            val searchView = menuItem.actionView as? SearchView
            if (searchView != null && searchManager != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(act.componentName))
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(s: String): Boolean {
                        startSearch(s)
                        return true
                    }

                    override fun onQueryTextChange(s: String): Boolean = false
                })

                val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
                closeButton?.setOnClickListener {
                    val ed = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
                    ed?.setText("")
                    searchView.setQuery("", false)
                    searchView.onActionViewCollapsed()
                    menuItem.collapseActionView()
                    foodListViewModel.mutableLiveDataFoodList
                }
            }
        }
    }

    private fun startSearch(s: String) {
        val foods = Common.categorySelected?.foods ?: return

        val resultList = ArrayList<FoodModel>()
        for (foodModel in foods) {
            if (foodModel.name?.lowercase(Locale.getDefault())?.contains(s.lowercase(Locale.getDefault())) == true) {
                resultList.add(foodModel)
            }
        }
        foodListViewModel.mutableLiveDataFoodList.value = resultList
    }

    override fun onDestroyView() {
        adapter?.clearDisposables()
        adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}
