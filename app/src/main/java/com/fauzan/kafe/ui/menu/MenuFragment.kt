package com.fauzan.kafe.ui.menu

import android.app.AlertDialog
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
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fauzan.kafe.Adapter.MyCategoriesAdapter
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Common.SpacesItemDecoration
import com.fauzan.kafe.EventBus.MenuItemBack
import com.fauzan.kafe.Model.CategoryModel
import com.fauzan.kafe.R
import com.fauzan.kafe.databinding.FragmentMenuBinding
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.Locale

/**
 * Menu category browser screen migrated to Android Jetpack ViewBinding.
 * Eliminates ButterKnife and ensures clean lifecycle nullification in onDestroyView.
 */
class MenuFragment : Fragment() {

    private lateinit var menuViewModel: MenuViewModel
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private var dialog: AlertDialog? = null
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyCategoriesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        menuViewModel = ViewModelProvider(this).get(MenuViewModel::class.java)
        _binding = FragmentMenuBinding.inflate(inflater, container, false)

        initViews()

        menuViewModel.messageError.observe(viewLifecycleOwner) { s ->
            context?.let { Toast.makeText(it, s, Toast.LENGTH_SHORT).show() }
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        }

        menuViewModel.categoryListMultable.observe(viewLifecycleOwner) { categoryModelList ->
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
            }
            context?.let { ctx ->
                _binding?.let { b ->
                    adapter = MyCategoriesAdapter(ctx, categoryModelList)
                    b.recyclerMenu.adapter = adapter
                    layoutAnimationController?.let { b.recyclerMenu.layoutAnimation = it }
                }
            }
        }

        return binding.root
    }

    private fun initViews() {
        setHasOptionsMenu(true)

        val ctx = context ?: return
        dialog = SpotsDialog.Builder().setContext(ctx).setCancelable(true).build()
        dialog?.show()

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(ctx, R.anim.layout_item_from_left)

        val layoutManager = GridLayoutManager(ctx, 2)
        layoutManager.orientation = RecyclerView.VERTICAL
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val adp = adapter ?: return -1
                return when (adp.getItemViewType(position)) {
                    Common.DEFAULT_COLUMN_COUNT -> 1
                    Common.FULL_WIDTH_COLUMN -> 2
                    else -> -1
                }
            }
        }

        binding.recyclerMenu.layoutManager = layoutManager
        binding.recyclerMenu.addItemDecoration(SpacesItemDecoration(8))
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
                    menuViewModel.loadCategories()
                }
            }
        }
    }

    private fun startSearch(s: String) {
        val catList = adapter?.listCategory ?: return

        val resultList = ArrayList<CategoryModel>()
        for (categoryModel in catList) {
            if (categoryModel.name?.lowercase(Locale.getDefault())?.contains(s.lowercase(Locale.getDefault())) == true) {
                resultList.add(categoryModel)
            }
        }
        menuViewModel.categoryListMultable.value = resultList
    }

    override fun onDestroyView() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        dialog = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}
