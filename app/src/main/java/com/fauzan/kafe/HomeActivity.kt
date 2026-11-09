package com.fauzan.kafe

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.andremion.counterfab.CounterFab
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Database.CartDataSource
import com.fauzan.kafe.Database.CartDatabase
import com.fauzan.kafe.Database.LocalCartDataSource
import com.fauzan.kafe.EventBus.BestDealItemClick
import com.fauzan.kafe.EventBus.CategoryClick
import com.fauzan.kafe.EventBus.CounterCartEvent
import com.fauzan.kafe.EventBus.FoodItemClick
import com.fauzan.kafe.EventBus.HideFABCart
import com.fauzan.kafe.EventBus.MenuItemBack
import com.fauzan.kafe.EventBus.PopularCategoryClick
import com.fauzan.kafe.Model.CategoryModel
import com.fauzan.kafe.Model.FoodModel
import com.fauzan.kafe.databinding.ActivityHomeBinding
import com.fauzan.kafe.domain.session.ClientSessionManager
import com.fauzan.kafe.ui.location.Location
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var mAppBarConfiguration: AppBarConfiguration
    private lateinit var drawer: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var cartDataSource: CartDataSource
    private var dialog: android.app.AlertDialog? = null
    private var menuClickId = -1
    private var fab: CounterFab? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        countCartItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(true).build()
        fab = binding.appBarHome.fab
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDAO())

        val toolbar = binding.appBarHome.toolbar
        setSupportActionBar(toolbar)

        fab?.setOnClickListener {
            navController.navigate(R.id.nav_cart)
        }

        drawer = binding.drawerLayout
        val navigationView = binding.navView

        mAppBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail,
            R.id.nav_view_orders, R.id.nav_location, R.id.nav_cart, R.id.nav_food_list
        ).setOpenableLayout(drawer).build()

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navController)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.bringToFront()

        val headerView = navigationView.getHeaderView(0)
        val txtUser = headerView.findViewById<TextView>(R.id.txt_user)
        Common.currentUser?.name?.let { userName ->
            Common.setSpanString("Hey, ", userName, txtUser)
        }

        countCartItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navCtrl = Navigation.findNavController(this, R.id.nav_host_fragment)
        return NavigationUI.navigateUp(navCtrl, mAppBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true
        drawer.closeDrawers()
        val itemId = menuItem.itemId
        when (itemId) {
            R.id.nav_home -> {
                if (itemId != menuClickId) navController.navigate(R.id.nav_home)
            }
            R.id.nav_menu -> {
                if (itemId != menuClickId) navController.navigate(R.id.nav_menu)
            }
            R.id.nav_cart -> {
                if (itemId != menuClickId) navController.navigate(R.id.nav_cart)
            }
            R.id.nav_view_orders -> {
                if (itemId != menuClickId) navController.navigate(R.id.nav_view_orders)
            }
            R.id.nav_location -> {
                location()
            }
            R.id.nav_sign_out -> {
                signOut()
            }
        }
        menuClickId = menuItem.itemId
        return true
    }

    fun location() {
        startActivity(Intent(this@HomeActivity, Location::class.java))
    }

    private fun signOut() {
        AlertDialog.Builder(this)
            .setTitle("Signout")
            .setMessage("Do you really want to sign out?")
            .setNegativeButton("CANCEL") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setPositiveButton("OK") { _, _ ->
                Common.selectedFood = null
                Common.categorySelected = null
                Common.currentUser = null
                ClientSessionManager.getInstance().clearSession()
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .create()
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        compositeDisposable.clear()
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event: CategoryClick?) {
        if (event != null && event.isSuccess) {
            EventBus.getDefault().removeStickyEvent(event)
            navController.navigate(R.id.nav_food_list)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onFoodItemClick(event: FoodItemClick?) {
        if (event != null && event.isSuccess) {
            EventBus.getDefault().removeStickyEvent(event)
            navController.navigate(R.id.nav_food_detail)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onHideFABEvent(event: HideFABCart) {
        if (event.isHidden) {
            fab?.hide()
        } else {
            fab?.show()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCartCounter(event: CounterCartEvent?) {
        if (event != null && event.isSuccess) {
            EventBus.getDefault().removeStickyEvent(event)
            countCartItem()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBestDealItemClick(event: BestDealItemClick?) {
        val bestDeal = event?.bestDealModel ?: return
        EventBus.getDefault().removeStickyEvent(event)
        dialog?.show()

        FirebaseDatabase.getInstance()
            .getReference("Category")
            .child(bestDeal.menu_id ?: "")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Common.categorySelected = dataSnapshot.getValue(CategoryModel::class.java)
                        Common.categorySelected?.menu_id = dataSnapshot.key

                        FirebaseDatabase.getInstance()
                            .getReference("Category")
                            .child(bestDeal.menu_id ?: "")
                            .child("foods")
                            .orderByChild("id")
                            .equalTo(bestDeal.food_id)
                            .limitToLast(1)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapShot in snapshot.children) {
                                            Common.selectedFood = itemSnapShot.getValue(FoodModel::class.java)
                                            Common.selectedFood?.key = itemSnapShot.key
                                        }
                                        navController.navigate(R.id.nav_food_detail)
                                    } else {
                                        Toast.makeText(this@HomeActivity, "Item doesn't exists!", Toast.LENGTH_SHORT).show()
                                    }
                                    dialog?.dismiss()
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    dialog?.dismiss()
                                    Toast.makeText(this@HomeActivity, databaseError.message, Toast.LENGTH_SHORT).show()
                                }
                            })
                    } else {
                        dialog?.dismiss()
                        Toast.makeText(this@HomeActivity, "Item doesn't exists!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    dialog?.dismiss()
                    Toast.makeText(this@HomeActivity, databaseError.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPopularItemClick(event: PopularCategoryClick?) {
        val popularCategory = event?.popularCategoryModel ?: return
        EventBus.getDefault().removeStickyEvent(event)
        dialog?.show()

        FirebaseDatabase.getInstance()
            .getReference("Category")
            .child(popularCategory.menu_id ?: "")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Common.categorySelected = dataSnapshot.getValue(CategoryModel::class.java)
                        Common.categorySelected?.menu_id = dataSnapshot.key

                        FirebaseDatabase.getInstance()
                            .getReference("Category")
                            .child(popularCategory.menu_id ?: "")
                            .child("foods")
                            .orderByChild("id")
                            .equalTo(popularCategory.food_id)
                            .limitToLast(1)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapShot in snapshot.children) {
                                            Common.selectedFood = itemSnapShot.getValue(FoodModel::class.java)
                                            Common.selectedFood?.key = itemSnapShot.key
                                        }
                                        navController.navigate(R.id.nav_food_detail)
                                    } else {
                                        Toast.makeText(this@HomeActivity, "Item doesn't exists!", Toast.LENGTH_SHORT).show()
                                    }
                                    dialog?.dismiss()
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    dialog?.dismiss()
                                    Toast.makeText(this@HomeActivity, databaseError.message, Toast.LENGTH_SHORT).show()
                                }
                            })
                    } else {
                        dialog?.dismiss()
                        Toast.makeText(this@HomeActivity, "Item doesn't exists!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    dialog?.dismiss()
                    Toast.makeText(this@HomeActivity, databaseError.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun countCartItem() {
        val uid = Common.currentUser?.uid ?: return
        cartDataSource.countItemInCart(uid)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Int> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(count: Int) {
                    fab?.count = count
                }

                override fun onError(e: Throwable) {
                    if (e.message?.contains("Query returned empty") != true) {
                        Toast.makeText(this@HomeActivity, "[COUNT CART]${e.message}", Toast.LENGTH_SHORT).show()
                    } else {
                        fab?.count = 0
                    }
                }
            })
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun countCartAgain(event: CounterCartEvent) {
        if (event.isSuccess) {
            countCartItem()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMenuItemBack(event: MenuItemBack) {
        menuClickId = -1
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }
    }
}
