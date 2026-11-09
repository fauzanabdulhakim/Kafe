package com.fauzan.kafe.ui.cart

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.braintreepayments.api.models.PaymentMethodNonce
import com.fauzan.kafe.Adapter.MyCartAdapter
import com.fauzan.kafe.Callback.ILoadTimeFromFirebaseListener
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Common.MySwipeHelper
import com.fauzan.kafe.Database.CartDataSource
import com.fauzan.kafe.Database.CartDatabase
import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.Database.LocalCartDataSource
import com.fauzan.kafe.EventBus.CounterCartEvent
import com.fauzan.kafe.EventBus.HideFABCart
import com.fauzan.kafe.EventBus.MenuItemBack
import com.fauzan.kafe.EventBus.UpdateItemInCart
import com.fauzan.kafe.Model.OrderModel
import com.fauzan.kafe.R
import com.fauzan.kafe.Remote.ICloudFunction
import com.fauzan.kafe.Remote.RetrofitCloudClient
import com.fauzan.kafe.databinding.FragmentCartBinding
import com.fauzan.kafe.databinding.LayoutPlaceOrderBinding
import com.fauzan.kafe.domain.calculator.PriceCalculator
import com.fauzan.kafe.domain.usecase.CalculateCartTotalUseCase
import com.fauzan.kafe.domain.usecase.ValidateOrderPlacementUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.util.Locale

/**
 * Fragment rendering cart items, totals, order submission, and checkout options.
 * Migrated to Android Jetpack ViewBinding with nullified bindings in onDestroyView to eliminate memory leaks.
 * Integrates CalculateCartTotalUseCase for domain pricing calculations.
 */
class CartFragment : Fragment(), ILoadTimeFromFirebaseListener {

    private val compositeDisposable = CompositeDisposable()
    private val calculateCartTotalUseCase = CalculateCartTotalUseCase(PriceCalculator())
    private val validateOrderPlacementUseCase = ValidateOrderPlacementUseCase(calculateCartTotalUseCase)

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private var recyclerViewState: Parcelable? = null
    private lateinit var cartDataSource: CartDataSource

    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null

    private var address: String? = null
    private var comment: String? = null

    private lateinit var cloudFunction: ICloudFunction
    private val listener: ILoadTimeFromFirebaseListener = this
    private var adapter: MyCartAdapter? = null
    private lateinit var cartViewModel: CartViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cartViewModel = ViewModelProvider(this).get(CartViewModel::class.java)
        _binding = FragmentCartBinding.inflate(inflater, container, false)

        cloudFunction = RetrofitCloudClient.getInstance().create(ICloudFunction::class.java)

        context?.let { ctx ->
            cartViewModel.initCartDataSource(ctx)
        }

        initViews()
        initLocation()

        cartViewModel.getMutableLiveDataCartItems().observe(viewLifecycleOwner) { cartItems ->
            val b = _binding ?: return@observe
            if (cartItems.isNullOrEmpty()) {
                b.recyclerCart.visibility = View.GONE
                b.groupPlaceHolder.visibility = View.GONE
                b.txtEmptyCart.visibility = View.VISIBLE
            } else {
                b.recyclerCart.visibility = View.VISIBLE
                b.groupPlaceHolder.visibility = View.VISIBLE
                b.txtEmptyCart.visibility = View.GONE

                context?.let { ctx ->
                    adapter = MyCartAdapter(ctx, cartItems)
                    b.recyclerCart.adapter = adapter
                }

                val summary = calculateCartTotalUseCase.invoke(cartItems, 0.0)
                b.txtTotalPrice.text = "Total: Rp${Common.formatPrice(summary.finalTotal)}"
            }
        }

        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun initLocation() {
        val ctx = context ?: return
        buildLocationRequest()
        buildLocationCallback()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx)
        locationRequest?.let { req ->
            locationCallback?.let { cb ->
                fusedLocationProviderClient?.requestLocationUpdates(req, cb, Looper.getMainLooper())
            }
        }
    }

    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 3000
            smallestDisplacement = 10f
        }
    }

    private fun initViews() {
        setHasOptionsMenu(true)

        val ctx = context ?: return
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(ctx).cartDAO())

        EventBus.getDefault().postSticky(HideFABCart(isHidden = true))

        binding.recyclerCart.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(ctx)
        binding.recyclerCart.layoutManager = layoutManager
        binding.recyclerCart.addItemDecoration(DividerItemDecoration(ctx, layoutManager.orientation))

        object : MySwipeHelper(ctx, binding.recyclerCart, 200) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buf: MutableList<MyButton>
            ) {
                buf.add(
                    MyButton(ctx, "Delete", 30, 0, Color.parseColor("#FF3c30")) { pos ->
                        val adp = adapter ?: return@MyButton
                        val cartItem = adp.getItemAtPosition(pos)
                        compositeDisposable.add(
                            cartDataSource.deleteCartItem(cartItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    adp.notifyItemRemoved(pos)
                                    sumAllItemInCart()
                                    EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                                    Toast.makeText(context, "Delete item from Cart successful!", Toast.LENGTH_SHORT).show()
                                }, { e ->
                                    Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                                })
                        )
                    }
                )
            }
        }

        binding.btnPlaceOrder.setOnClickListener { onPlaceOrderClick() }

        sumAllItemInCart()
    }

    @SuppressLint("MissingPermission")
    private fun onPlaceOrderClick() {
        val ctx = context ?: return

        val builder = AlertDialog.Builder(ctx)
        builder.setTitle("One more step!")

        val dialogBinding = LayoutPlaceOrderBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        Common.currentUser?.let { user ->
            dialogBinding.edtAddress.setText(user.address)
        }

        dialogBinding.rdiHomeAddress.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Common.currentUser != null) {
                dialogBinding.edtAddress.setText(Common.currentUser?.address)
                dialogBinding.txtAddressDetail.visibility = View.GONE
            }
        }

        dialogBinding.rdiOtherAddress.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dialogBinding.edtAddress.setText("")
                dialogBinding.edtAddress.hint = "Enter your address"
                dialogBinding.txtAddressDetail.visibility = View.GONE
            }
        }

        dialogBinding.rdiShipThisAddress.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && fusedLocationProviderClient != null) {
                fusedLocationProviderClient?.lastLocation
                    ?.addOnFailureListener { e ->
                        Toast.makeText(ctx, "${e.message}", Toast.LENGTH_SHORT).show()
                        dialogBinding.txtAddressDetail.visibility = View.GONE
                    }
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result != null) {
                            val loc = task.result
                            val coordinates = "${loc.latitude}/${loc.longitude}"
                            val singleAddress = Single.just(getAddressFromLatLng(loc.latitude, loc.longitude))

                            compositeDisposable.add(
                                singleAddress.subscribeWith(object : DisposableSingleObserver<String>() {
                                    override fun onSuccess(s: String) {
                                        dialogBinding.edtAddress.setText(coordinates)
                                        dialogBinding.txtAddressDetail.text = s
                                        dialogBinding.txtAddressDetail.visibility = View.VISIBLE
                                    }

                                    override fun onError(e: Throwable) {
                                        dialogBinding.edtAddress.setText(coordinates)
                                        dialogBinding.txtAddressDetail.text = e.message
                                        dialogBinding.txtAddressDetail.visibility = View.VISIBLE
                                    }
                                })
                            )
                        }
                    }
            }
        }

        builder.setNegativeButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("YES") { _, _ ->
            if (dialogBinding.rdiCod.isChecked) {
                paymentCOD(dialogBinding.edtAddress.text.toString(), dialogBinding.edtComment.text.toString())
            } else if (dialogBinding.rdiBraintree.isChecked) {
                address = dialogBinding.edtAddress.text.toString()
                comment = dialogBinding.edtComment.text.toString()
                if (!TextUtils.isEmpty(Common.currentToken)) {
                    val dropInRequest = DropInRequest().clientToken(Common.currentToken)
                    startActivityForResult(dropInRequest.getIntent(ctx), REQUEST_BRAINTREE_CODE)
                }
            }
        }

        builder.create().show()
    }

    private fun paymentCOD(shippingAddress: String, orderComment: String) {
        val user = Common.currentUser ?: return

        compositeDisposable.add(
            cartDataSource.getAllCart(user.uid ?: "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItems ->
                    val summary = calculateCartTotalUseCase.invoke(cartItems, 0.0)
                    val totalPrice = summary.finalTotal

                    val order = OrderModel(
                        userId = user.uid,
                        userName = user.name,
                        userPhone = user.phone,
                        shippingAddress = shippingAddress,
                        comment = orderComment,
                        lat = currentLocation?.latitude ?: 0.1,
                        lng = currentLocation?.longitude ?: -0.1,
                        cartItemList = cartItems.toMutableList(),
                        totalPayment = totalPrice,
                        discount = 0,
                        finalPayment = totalPrice,
                        isCod = true,
                        transactionId = "Cash On Delivery"
                    )

                    val validationResult = validateOrderPlacementUseCase(order)
                    if (validationResult is ValidateOrderPlacementUseCase.ValidationResult.Invalid) {
                        Toast.makeText(context, validationResult.reason, Toast.LENGTH_LONG).show()
                        return@subscribe
                    }

                    syncLocalTimeWithGlobaltime(order)
                }, { throwable ->
                    Toast.makeText(context, "${throwable.message}", Toast.LENGTH_SHORT).show()
                })
        )
    }

    private fun syncLocalTimeWithGlobaltime(order: OrderModel) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val offsetVal = dataSnapshot.getValue(Long::class.java)
                val offset = offsetVal ?: 0L
                val estimatedServerTimeMs = System.currentTimeMillis() + offset

                listener.onLoadTimeSucces(order, estimatedServerTimeMs)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onLoadTimeFailed(databaseError.message)
            }
        })
    }

    private fun writeOrderToFirebase(order: OrderModel) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener { e ->
                Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                val uid = Common.currentUser?.uid
                if (uid != null) {
                    compositeDisposable.add(
                        cartDataSource.cleanCart(uid)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                Toast.makeText(context, "Order placed Successfully!", Toast.LENGTH_SHORT).show()
                                EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                            }, { throwable ->
                                Toast.makeText(context, "${throwable.message}", Toast.LENGTH_SHORT).show()
                            })
                    )
                }
            }
    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        val ctx = context ?: return "Address not found"
        val geocoder = Geocoder(ctx, Locale.getDefault())
        var result = "Address not found"
        try {
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addressList.isNullOrEmpty()) {
                result = addressList[0].getAddressLine(0) ?: "Address not found"
            }
        } catch (e: IOException) {
            result = e.message ?: "Error resolving address"
        }
        return result
    }

    private fun sumAllItemInCart() {
        val uid = Common.currentUser?.uid ?: return
        compositeDisposable.add(
            cartDataSource.getAllCart(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItems ->
                    _binding?.let { b ->
                        val summary = calculateCartTotalUseCase.invoke(cartItems, 0.0)
                        b.txtTotalPrice.text = "Total: Rp${Common.formatPrice(summary.finalTotal)}"
                    }
                }, { throwable ->
                    if (throwable.message?.contains("Query returned empty") != true) {
                        Toast.makeText(context, "${throwable.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        )
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_settings)?.isVisible = false
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.cart_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_cart) {
            val uid = Common.currentUser?.uid
            if (uid != null) {
                compositeDisposable.add(
                    cartDataSource.cleanCart(uid)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Toast.makeText(context, "Clear Cart Success", Toast.LENGTH_SHORT).show()
                            EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                        }, { throwable ->
                            Toast.makeText(context, "${throwable.message}", Toast.LENGTH_SHORT).show()
                        })
                )
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        EventBus.getDefault().postSticky(HideFABCart(isHidden = false))
        cartViewModel.onStop()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        locationCallback?.let { cb ->
            fusedLocationProviderClient?.removeLocationUpdates(cb)
        }
        compositeDisposable.clear()
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        locationRequest?.let { req ->
            locationCallback?.let { cb ->
                fusedLocationProviderClient?.requestLocationUpdates(req, cb, Looper.getMainLooper())
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateItemInCartEvent(event: UpdateItemInCart?) {
        val item = event?.cartItem
        val b = _binding
        if (item != null && b != null) {
            recyclerViewState = b.recyclerCart.layoutManager?.onSaveInstanceState()
            compositeDisposable.add(
                cartDataSource.updateCartItems(item)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        sumAllItemInCart()
                        recyclerViewState?.let { st ->
                            _binding?.recyclerCart?.layoutManager?.onRestoreInstanceState(st)
                        }
                    }, { throwable ->
                        Toast.makeText(context, "[UPDATE CART]${throwable.message}", Toast.LENGTH_SHORT).show()
                    })
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BRAINTREE_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result: DropInResult? = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT)
            val nonce: PaymentMethodNonce? = result?.paymentMethodNonce

            val user = Common.currentUser
            if (nonce == null || user == null) return

            compositeDisposable.add(
                cartDataSource.getAllCart(user.uid ?: "")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ cartItems ->
                        val summary = calculateCartTotalUseCase.invoke(cartItems, 0.0)
                        val totalPrice = summary.finalTotal

                        val preValidationOrder = OrderModel(
                            userId = user.uid,
                            userName = user.name,
                            userPhone = user.phone,
                            shippingAddress = address,
                            comment = comment,
                            lat = currentLocation?.latitude ?: 0.1,
                            lng = currentLocation?.longitude ?: -0.1,
                            cartItemList = cartItems.toMutableList(),
                            totalPayment = totalPrice,
                            discount = 0,
                            finalPayment = totalPrice,
                            isCod = false,
                            transactionId = "TEMP_AUTH_ID"
                        )

                        val validationResult = validateOrderPlacementUseCase(preValidationOrder)
                        if (validationResult is ValidateOrderPlacementUseCase.ValidationResult.Invalid) {
                            Toast.makeText(context, validationResult.reason, Toast.LENGTH_LONG).show()
                            return@subscribe
                        }

                        val headers = HashMap<String, String>()
                        headers["Authorization"] = Common.buildToken(Common.authorizeKey)

                        compositeDisposable.add(
                            cloudFunction.submitPayment(headers, totalPrice, nonce.nonce)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ braintreeTransaction ->
                                    if (braintreeTransaction != null && braintreeTransaction.isSuccess) {
                                        val order = OrderModel(
                                            userId = user.uid,
                                            userName = user.name,
                                            userPhone = user.phone,
                                            shippingAddress = address,
                                            comment = comment,
                                            lat = currentLocation?.latitude ?: 0.1,
                                            lng = currentLocation?.longitude ?: -0.1,
                                            cartItemList = cartItems.toMutableList(),
                                            totalPayment = totalPrice,
                                            discount = 0,
                                            finalPayment = totalPrice,
                                            isCod = false,
                                            transactionId = braintreeTransaction.transaction?.id ?: ""
                                        )

                                        syncLocalTimeWithGlobaltime(order)
                                    }
                                }, { throwable ->
                                    Toast.makeText(context, "${throwable.message}", Toast.LENGTH_SHORT).show()
                                })
                        )
                    }, { throwable ->
                        Toast.makeText(context, "${throwable.message}", Toast.LENGTH_SHORT).show()
                    })
            )
        }
    }

    override fun onLoadTimeSucces(order: OrderModel, estimateTimeInMs: Long) {
        order.createDate = estimateTimeInMs
        order.orderStatus = 0
        writeOrderToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_BRAINTREE_CODE = 7777
    }
}
