package com.fauzan.kafe.ui.view_orders

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fauzan.kafe.Adapter.MyOrdersAdapter
import com.fauzan.kafe.Callback.ILoadOrderCallbackListener
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Common.MySwipeHelper
import com.fauzan.kafe.Model.OrderModel
import com.fauzan.kafe.Model.RefundRequestModel
import com.fauzan.kafe.R
import com.fauzan.kafe.databinding.FragmentViewOrderBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import java.util.Collections

/**
 * Customer order history screen with real-time order status tracking and PCI DSS compliant cancellations.
 * Migrated to Android Jetpack ViewBinding with nullified bindings in onDestroyView.
 */
class ViewOrdersFragment : Fragment(), ILoadOrderCallbackListener {

    private var _binding: FragmentViewOrderBinding? = null
    private val binding get() = _binding!!
    private var dialog: AlertDialog? = null
    private lateinit var viewOrdersViewModel: ViewOrdersViewModel
    private val listener: ILoadOrderCallbackListener = this

    private var orderQueryRef: DatabaseReference? = null
    private var activeOrderQuery: Query? = null
    private var orderValueEventListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewOrdersViewModel = ViewModelProvider(this).get(ViewOrdersViewModel::class.java)
        _binding = FragmentViewOrderBinding.inflate(inflater, container, false)

        initViews()
        attachRealtimeOrderListener()

        viewOrdersViewModel.mutableLiveDataOrderList.observe(viewLifecycleOwner) { orderList ->
            if (_binding != null && context != null && orderList != null) {
                Collections.reverse(orderList)
                val adapter = MyOrdersAdapter(requireContext(), orderList.toMutableList())
                binding.recyclerOrders.adapter = adapter
            }
        }

        return binding.root
    }

    /**
     * Listens to real-time order updates so customer immediately observes status changes made by kitchen.
     */
    private fun attachRealtimeOrderListener() {
        val uid = Common.currentUser?.uid ?: return
        dialog?.show()

        orderQueryRef = FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
        orderValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val orderList = ArrayList<OrderModel>()
                for (orderSnapShot in dataSnapshot.children) {
                    val order = orderSnapShot.getValue(OrderModel::class.java)
                    if (order != null && uid == order.userId) {
                        order.orderNumber = orderSnapShot.key
                        orderList.add(order)
                    }
                }
                listener.onLoadOrderSuccess(orderList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onLoadOrderFailed(databaseError.message)
            }
        }

        val query = orderQueryRef?.orderByChild("userId")
            ?.equalTo(uid)
            ?.limitToLast(100)
        activeOrderQuery = query
        query?.addValueEventListener(orderValueEventListener!!)
    }

    private fun initViews() {
        val ctx = context ?: return
        dialog = SpotsDialog.Builder().setCancelable(true).setContext(ctx).build()

        binding.recyclerOrders.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(ctx)
        binding.recyclerOrders.layoutManager = layoutManager
        binding.recyclerOrders.addItemDecoration(DividerItemDecoration(ctx, layoutManager.orientation))

        object : MySwipeHelper(ctx, binding.recyclerOrders, 250) {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buf: MutableList<MyButton>
            ) {
                buf.add(
                    MyButton(ctx, "Cancel Order", 30, 0, Color.parseColor("#FF3c30")) { pos ->
                        val adapter = binding.recyclerOrders.adapter as? MyOrdersAdapter ?: return@MyButton
                        val orderModel = adapter.getItemAtPosition(pos)
                        if (orderModel.orderStatus == 0) {
                            if (orderModel.isCod) {
                                val builder = androidx.appcompat.app.AlertDialog.Builder(ctx)
                                builder.setTitle("Cancel Order")
                                    .setMessage("Do you really want to cancel this order?")
                                    .setNegativeButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
                                    .setPositiveButton("YES") { _, _ ->
                                        val orderNum = orderModel.orderNumber ?: return@setPositiveButton
                                        val updateData = HashMap<String, Any>()
                                        updateData["orderStatus"] = -1
                                        FirebaseDatabase.getInstance()
                                            .getReference(Common.ORDER_REF)
                                            .child(orderNum)
                                            .updateChildren(updateData)
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnSuccessListener {
                                                orderModel.orderStatus = -1
                                                adapter.setItemAtPosition(pos, orderModel)
                                                adapter.notifyItemChanged(pos)
                                                Toast.makeText(context, "Cancel order successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                builder.create().show()
                            } else {
                                val layoutRefund = LayoutInflater.from(ctx).inflate(R.layout.layout_refund_request, null)
                                val txtRefundAmount = layoutRefund.findViewById<TextView>(R.id.txt_refund_amount)
                                val edtReason = layoutRefund.findViewById<EditText>(R.id.edt_refund_reason)

                                txtRefundAmount.text = "Refund Amount: ${Common.formatPrice(orderModel.finalPayment)}"

                                val builder = androidx.appcompat.app.AlertDialog.Builder(ctx)
                                builder.setTitle("Cancel Order & Request Refund")
                                    .setMessage("Please provide a reason for cancelling this order:")
                                    .setView(layoutRefund)
                                    .setNegativeButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
                                    .setPositiveButton("YES") { _, _ ->
                                        var reason = edtReason?.text?.toString()?.trim() ?: ""
                                        if (TextUtils.isEmpty(reason)) {
                                            reason = "Customer cancelled order"
                                        }

                                        val refundRequestModel = RefundRequestModel(
                                            orderNumber = orderModel.orderNumber,
                                            name = Common.currentUser?.name ?: orderModel.userName,
                                            phone = Common.currentUser?.phone ?: orderModel.userPhone,
                                            amount = orderModel.finalPayment,
                                            reason = reason,
                                            transactionId = orderModel.transactionId,
                                            status = "PENDING"
                                        )

                                        val orderNum = orderModel.orderNumber ?: return@setPositiveButton
                                        FirebaseDatabase.getInstance()
                                            .getReference(Common.REQUEST_REFUND_MODEL)
                                            .child(orderNum)
                                            .setValue(refundRequestModel)
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnSuccessListener {
                                                val updateData = HashMap<String, Any>()
                                                updateData["orderStatus"] = -1
                                                FirebaseDatabase.getInstance()
                                                    .getReference(Common.ORDER_REF)
                                                    .child(orderNum)
                                                    .updateChildren(updateData)
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addOnSuccessListener {
                                                        orderModel.orderStatus = -1
                                                        adapter.setItemAtPosition(pos, orderModel)
                                                        adapter.notifyItemChanged(pos)
                                                        Toast.makeText(context, "Cancel order successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                    }
                                builder.create().show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Your order was changed to ${Common.convertStatusToText(orderModel.orderStatus)}, so you can't cancel it!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    override fun onLoadOrderSuccess(orderList: List<OrderModel>) {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        viewOrdersViewModel.setMutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        if (orderValueEventListener != null) {
            activeOrderQuery?.removeEventListener(orderValueEventListener!!)
            orderQueryRef?.removeEventListener(orderValueEventListener!!)
        }
        activeOrderQuery = null
        super.onStop()
    }

    override fun onDestroyView() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        dialog = null
        _binding = null
        super.onDestroyView()
    }
}
