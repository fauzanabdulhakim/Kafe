package com.fauzan.kafe.ui.fooddetail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Database.CartDataSource
import com.fauzan.kafe.Database.CartDatabase
import com.fauzan.kafe.Database.CartItem
import com.fauzan.kafe.Database.LocalCartDataSource
import com.fauzan.kafe.EventBus.CounterCartEvent
import com.fauzan.kafe.EventBus.MenuItemBack
import com.fauzan.kafe.Model.AddonModel
import com.fauzan.kafe.Model.CommentModel
import com.fauzan.kafe.Model.FoodModel
import com.fauzan.kafe.Model.SizeModel
import com.fauzan.kafe.R
import com.fauzan.kafe.databinding.FragmentFoodDetailBinding
import com.fauzan.kafe.databinding.LayoutAddonDisplayBinding
import com.fauzan.kafe.databinding.LayoutRatingBinding
import com.fauzan.kafe.ui.comments.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.util.Locale

/**
 * Fragment rendering food details, size selection, addons, and cart placement.
 * Migrated to Android Jetpack ViewBinding with nullified bindings in onDestroyView to prevent memory leaks.
 */
class FoodDetailFragment : Fragment(), TextWatcher {

    private lateinit var foodDetailViewModel: FoodDetailViewModel
    private var _binding: FragmentFoodDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartDataSource: CartDataSource
    private val compositeDisposable = CompositeDisposable()

    private var waitingDialog: android.app.AlertDialog? = null
    private var addonBottomSheetDialog: BottomSheetDialog? = null

    private var chipGroupAddon: ChipGroup? = null
    private var edtSearch: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        foodDetailViewModel = ViewModelProvider(this).get(FoodDetailViewModel::class.java)
        _binding = FragmentFoodDetailBinding.inflate(inflater, container, false)

        initViews()

        foodDetailViewModel.mutableLiveDataFood.observe(viewLifecycleOwner) { foodModel ->
            if (foodModel != null && _binding != null) {
                displayInfo(foodModel)
            }
        }

        foodDetailViewModel.mutableLiveDataComment.observe(viewLifecycleOwner) { commentModel ->
            if (commentModel != null) {
                submitRatingToFireBase(commentModel)
            }
        }

        return binding.root
    }

    private fun initViews() {
        context?.let { ctx ->
            cartDataSource = LocalCartDataSource(CartDatabase.getInstance(ctx).cartDAO())
            waitingDialog = SpotsDialog.Builder().setCancelable(true).setContext(ctx).build()

            addonBottomSheetDialog = BottomSheetDialog(ctx, R.style.DialogStyle)
            val addonDisplayBinding = LayoutAddonDisplayBinding.inflate(layoutInflater)
            chipGroupAddon = addonDisplayBinding.chipGroupAddon
            edtSearch = addonDisplayBinding.edtSearch
            addonBottomSheetDialog?.setContentView(addonDisplayBinding.root)

            addonBottomSheetDialog?.setOnDismissListener {
                displayUserSelectedAddon()
                calculateTotalPrice()
            }
        }

        binding.imgAddAddon.setOnClickListener { onAddonClick() }
        binding.btnCart.setOnClickListener { onCartItemAdd() }
        binding.btnRating.setOnClickListener { onRatingButtonClick() }
        binding.btnShowComment.setOnClickListener { onShowCommentButtonClick() }
    }

    private fun onAddonClick() {
        if (Common.selectedFood?.addon != null) {
            displayAddonList()
            addonBottomSheetDialog?.show()
        }
    }

    private fun onCartItemAdd() {
        val food = Common.selectedFood ?: return
        if (!food.isAvailable) {
            Toast.makeText(context, "Maaf, menu ${food.name} sedang habis (Out of Stock)", Toast.LENGTH_SHORT).show()
            return
        }
        val user = Common.currentUser ?: return

        val quantity = binding.numberButton.number.toIntOrNull() ?: 1
        val extraPrice = Common.calculateExtraPrice(food.userSelectedSize, food.userSelectedAddon)

        val foodAddonStr = if (food.userSelectedAddon != null) Gson().toJson(food.userSelectedAddon) else "Default"
        val foodSizeStr = if (food.userSelectedSize != null) Gson().toJson(food.userSelectedSize) else "Default"

        val cartItem = CartItem(
            uid = user.uid ?: "",
            userPhone = user.phone,
            foodId = food.id ?: "",
            foodName = food.name,
            foodImage = food.image,
            foodPrice = (food.price ?: 0L).toDouble(),
            foodQuantity = quantity,
            foodExtraPrice = extraPrice,
            foodAddon = foodAddonStr,
            foodSize = foodSizeStr
        )

        cartDataSource.getItemWithAllOptionsInCart(
            user.uid ?: "",
            cartItem.foodId,
            cartItem.foodSize,
            cartItem.foodAddon
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<CartItem> {
                override fun onSubscribe(d: Disposable) {
                    compositeDisposable.add(d)
                }

                override fun onSuccess(cartItemFromDB: CartItem) {
                    if (cartItemFromDB == cartItem) {
                        cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice
                        cartItemFromDB.foodAddon = cartItem.foodAddon
                        cartItemFromDB.foodSize = cartItem.foodSize
                        cartItemFromDB.foodQuantity += cartItem.foodQuantity

                        cartDataSource.updateCartItems(cartItemFromDB)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : SingleObserver<Int> {
                                override fun onSubscribe(d: Disposable) {
                                    compositeDisposable.add(d)
                                }

                                override fun onSuccess(integer: Int) {
                                    Toast.makeText(context, "Update Cart success", Toast.LENGTH_SHORT).show()
                                    EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                                }

                                override fun onError(e: Throwable) {
                                    Toast.makeText(context, "[UPDATE CART]${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    } else {
                        insertNewCartItem(cartItem)
                    }
                }

                override fun onError(e: Throwable) {
                    if (e.message?.contains("empty") == true) {
                        insertNewCartItem(cartItem)
                    } else {
                        Toast.makeText(context, "[GET CART]${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun insertNewCartItem(cartItem: CartItem) {
        compositeDisposable.add(
            cartDataSource.insertOrReplaceAll(cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(context, "Add to Cart success", Toast.LENGTH_SHORT).show()
                    EventBus.getDefault().postSticky(CounterCartEvent(isSuccess = true))
                }, { throwable ->
                    Toast.makeText(context, "[CART ERROR]${throwable.message}", Toast.LENGTH_SHORT).show()
                })
        )
    }

    private fun displayAddonList() {
        val addons = Common.selectedFood?.addon
        val chipGroup = chipGroupAddon
        if (!addons.isNullOrEmpty() && chipGroup != null) {
            chipGroup.clearCheck()
            chipGroup.removeAllViews()

            edtSearch?.addTextChangedListener(this)

            for (addonModel in addons) {
                val chip = layoutInflater.inflate(R.layout.layout_addon_item, null) as Chip
                chip.text = "${addonModel.name}(+Rp${addonModel.price})"
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (Common.selectedFood?.userSelectedAddon == null) {
                            Common.selectedFood?.userSelectedAddon = ArrayList()
                        }
                        Common.selectedFood?.userSelectedAddon?.add(addonModel)
                    }
                }
                chipGroup.addView(chip)
            }
        }
    }

    private fun onRatingButtonClick() {
        showDialogRating()
    }

    private fun onShowCommentButtonClick() {
        activity?.let {
            val commentFragment = CommentFragment.getInstance()
            commentFragment.show(it.supportFragmentManager, "CommentFragment")
        }
    }

    private fun showDialogRating() {
        val ctx = context ?: return

        val builder = AlertDialog.Builder(ctx)
        builder.setTitle("Rating Food")
        builder.setMessage("Please fill information")

        val ratingBinding = LayoutRatingBinding.inflate(layoutInflater)
        builder.setView(ratingBinding.root)

        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("OK") { _, _ ->
            val user = Common.currentUser
            if (user != null) {
                val serverTimesStamp = HashMap<String, Any>()
                serverTimesStamp["timeStamp"] = ServerValue.TIMESTAMP

                val commentModel = CommentModel(
                    ratingValue = ratingBinding.ratingBar.rating,
                    comment = ratingBinding.edtComment.text.toString(),
                    name = user.name,
                    uid = user.uid,
                    commentTimeStamp = serverTimesStamp
                )

                foodDetailViewModel.setCommentModel(commentModel)
            }
        }

        builder.create().show()
    }

    private fun displayUserSelectedAddon() {
        val selectedAddons = Common.selectedFood?.userSelectedAddon
        val bindingRef = _binding ?: return
        bindingRef.chipGroupUserSelectedAddon.removeAllViews()

        if (!selectedAddons.isNullOrEmpty()) {
            for (addonModel in selectedAddons) {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete_icon, null) as Chip
                chip.text = "${addonModel.name}(+Rp${addonModel.price})"
                chip.isClickable = false
                chip.setOnCloseIconClickListener { view ->
                    _binding?.chipGroupUserSelectedAddon?.removeView(view)
                    Common.selectedFood?.userSelectedAddon?.remove(addonModel)
                    calculateTotalPrice()
                }
                bindingRef.chipGroupUserSelectedAddon.addView(chip)
            }
        }
    }

    private fun submitRatingToFireBase(commentModel: CommentModel) {
        waitingDialog?.show()
        val food = Common.selectedFood
        if (food?.id != null) {
            FirebaseDatabase.getInstance()
                .getReference(Common.COMMENT_REF)
                .child(food.id!!)
                .push()
                .setValue(commentModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        addRatingToFood(commentModel.ratingValue)
                    }
                    if (waitingDialog?.isShowing == true) {
                        waitingDialog?.dismiss()
                    }
                }
        }
    }

    private fun addRatingToFood(ratingValue: Float) {
        val catSelected = Common.categorySelected ?: return
        val foodSelected = Common.selectedFood ?: return
        val catId = catSelected.menu_id ?: return
        val foodKey = foodSelected.key ?: return

        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(catId)
            .child("foods")
            .child(foodKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val foodModel = dataSnapshot.getValue(FoodModel::class.java)
                        if (foodModel != null) {
                            foodModel.key = foodKey

                            val currentRating = foodModel.ratingValue ?: 0.0
                            val currentCount = foodModel.ratingCount ?: 0L

                            val sumRating = currentRating + ratingValue
                            val newRatingCount = currentCount + 1

                            val updateData = HashMap<String, Any>()
                            updateData["ratingValue"] = sumRating
                            updateData["ratingCount"] = newRatingCount

                            foodModel.ratingValue = sumRating
                            foodModel.ratingCount = newRatingCount

                            dataSnapshot.ref
                                .updateChildren(updateData)
                                .addOnCompleteListener { task ->
                                    if (waitingDialog?.isShowing == true) {
                                        waitingDialog?.dismiss()
                                    }
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Thank you!", Toast.LENGTH_SHORT).show()
                                        Common.selectedFood = foodModel
                                        foodDetailViewModel.setFoodModel(foodModel)
                                    }
                                }
                        }
                    } else {
                        if (waitingDialog?.isShowing == true) {
                            waitingDialog?.dismiss()
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    if (waitingDialog?.isShowing == true) {
                        waitingDialog?.dismiss()
                    }
                    Toast.makeText(context, databaseError.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayInfo(foodModel: FoodModel) {
        val ctx = context ?: return
        val bindingRef = _binding ?: return

        Glide.with(ctx).load(foodModel.image).into(bindingRef.imgFood)
        bindingRef.foodName.text = foodModel.name
        bindingRef.foodDescription.text = foodModel.description
        bindingRef.foodPrice.text = foodModel.price.toString()

        val rVal = foodModel.ratingValue
        val rCount = foodModel.ratingCount
        if (rVal != null && rCount != null && rCount > 0) {
            bindingRef.ratingBar.rating = (rVal.toFloat() / rCount)
        }

        (activity as? AppCompatActivity)?.supportActionBar?.let { ab ->
            Common.selectedFood?.name?.let { ab.title = it }
        }

        // Setup size RadioButtons
        bindingRef.rdiGroupSize.removeAllViews()
        val sizes = foodModel.size
        if (!sizes.isNullOrEmpty()) {
            for (sizeModel in sizes) {
                val radioButton = RadioButton(ctx)
                radioButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        Common.selectedFood?.userSelectedSize = sizeModel
                        calculateTotalPrice()
                    }
                }
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
                radioButton.layoutParams = params
                radioButton.text = sizeModel.name
                radioButton.tag = sizeModel.price

                bindingRef.rdiGroupSize.addView(radioButton)
            }

            if (bindingRef.rdiGroupSize.childCount > 0) {
                val firstRadio = bindingRef.rdiGroupSize.getChildAt(0) as? RadioButton
                firstRadio?.isChecked = true
            }
        }

        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        val food = Common.selectedFood ?: return
        val bindingRef = _binding ?: return

        var totalPrice = (food.price ?: 0L).toDouble()

        food.userSelectedAddon?.let { addons ->
            for (addon in addons) {
                totalPrice += (addon.price ?: 0L).toDouble()
            }
        }

        food.userSelectedSize?.let { size ->
            totalPrice += (size.price ?: 0L).toDouble()
        }

        val quantity = bindingRef.numberButton.number.toIntOrNull() ?: 1
        val displayPrice = totalPrice * quantity
        bindingRef.foodPrice.text = Common.formatPrice(displayPrice)
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
        val chipGroup = chipGroupAddon ?: return
        val addons = Common.selectedFood?.addon ?: return

        chipGroup.clearCheck()
        chipGroup.removeAllViews()

        for (addonModel in addons) {
            if (addonModel.name?.lowercase(Locale.getDefault())?.contains(charSequence.toString().lowercase(Locale.getDefault())) == true) {
                val chip = layoutInflater.inflate(R.layout.layout_addon_item, null) as Chip
                chip.text = "${addonModel.name}(+Rp${addonModel.price})"
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (Common.selectedFood?.userSelectedAddon == null) {
                            Common.selectedFood?.userSelectedAddon = ArrayList()
                        }
                        Common.selectedFood?.userSelectedAddon?.add(addonModel)
                    }
                }
                chipGroup.addView(chip)
            }
        }
    }

    override fun afterTextChanged(s: Editable) {}

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroyView() {
        if (addonBottomSheetDialog?.isShowing == true) {
            addonBottomSheetDialog?.dismiss()
        }
        addonBottomSheetDialog = null

        if (waitingDialog?.isShowing == true) {
            waitingDialog?.dismiss()
        }
        waitingDialog = null

        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}
