package com.fauzan.kafe.ui.view_orders;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidwidgets.formatedittext.widgets.FormatEditText;
import com.fauzan.kafe.Adapter.MyOrdersAdapter;
import com.fauzan.kafe.Callback.ILoadOrderCallbackListener;
import com.fauzan.kafe.Common.Common;
import com.fauzan.kafe.Common.MySwipeHelper;
import com.fauzan.kafe.Database.CartItem;
import com.fauzan.kafe.EventBus.CounterCartEvent;
import com.fauzan.kafe.Model.OrderModel;
import com.fauzan.kafe.Model.RefundRequestModel;
import com.fauzan.kafe.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ViewOrdersFragment extends Fragment implements ILoadOrderCallbackListener {

    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;

    AlertDialog dialog;

    private Unbinder unbinder;

    private ViewOrdersViewModel viewOrdersViewModel;

    private ILoadOrderCallbackListener listener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewOrdersViewModel =
                ViewModelProviders.of(this).get(ViewOrdersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_order, container, false);
        unbinder = ButterKnife.bind(this,root);

        initViews(root);
        loadOrdersFromFirebase();

        viewOrdersViewModel.getMutableLiveDataOrderList().observe(this,orderList -> {
            Collections.reverse(orderList);
            MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(),orderList);
            recycler_orders.setAdapter(adapter);
        });

        return root;
    }

    private void loadOrdersFromFirebase() {
        List<OrderModel> orderList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser.getUid())
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapShot:dataSnapshot.getChildren())
                        {
                            OrderModel order = orderSnapShot.getValue(OrderModel.class);
                            order.setOrderNumber(orderSnapShot.getKey()); // Remember set it
                            orderList.add(order);
                        }
                        listener.onLoadOrderSuccess(orderList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadOrderFailed(databaseError.getMessage());
                    }
                });
    }

    private void initViews(View root) {
        listener = this;

        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();

        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(),recycler_orders,250) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Cancel Order",30,0, Color.parseColor("#FF3c30"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter)recycler_orders.getAdapter()).getItemAtPosition(pos);
                            if (orderModel.getOrderStatus() == 0)
                            {
                                if (orderModel.isCod())
                                {
                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("Cancel Order")
                                            .setMessage("Do you really want to cancel this order?")
                                            .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("YES", (dialogInterface, i) -> {

                                                Map<String,Object> update_data = new HashMap<>();
                                                update_data.put("orderStatus", -1); //Cancel order
                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.ORDER_REF)
                                                        .child(orderModel.getOrderNumber())
                                                        .updateChildren(update_data)
                                                        .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                        .addOnSuccessListener(aVoid -> {
                                                            orderModel.setOrderStatus(-1); // Local update
                                                            ((MyOrdersAdapter)recycler_orders.getAdapter()).setItemAtPosition(pos,orderModel);
                                                            recycler_orders.getAdapter().notifyItemChanged(pos);
                                                            Toast.makeText(getContext(), "Cancel order succesfully!", Toast.LENGTH_SHORT).show();
                                                        });
                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                                else // Not COD
                                {
                                    View layout_refund_request = LayoutInflater.from(getContext())
                                            .inflate(R.layout.layout_refund_request,null);
                                    EditText edt_name = (EditText)layout_refund_request.findViewById(R.id.edt_card_name);
                                    FormatEditText edt_card_number = (FormatEditText)layout_refund_request.findViewById(R.id.edt_card_number);
                                    FormatEditText edt_card_exp = (FormatEditText)layout_refund_request.findViewById(R.id.edt_exp);

                                    //Format credit card
                                    edt_card_number.setFormat("---- ---- ---- ----");
                                    edt_card_exp.setFormat("--/--");

                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("Cancel Order")
                                            .setMessage("Do you really want to cancel this order?")
                                            .setView(layout_refund_request)
                                            .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("YES", (dialogInterface, i) -> {

                                                RefundRequestModel refundRequestModel = new RefundRequestModel();
                                                refundRequestModel.setName(Common.currentUser.getName());
                                                refundRequestModel.setPhone(Common.currentUser.getPhone());
                                                refundRequestModel.setCardName(edt_name.getText().toString());
                                                refundRequestModel.setCardNumber(edt_card_number.getText().toString());
                                                refundRequestModel.setCardExp(edt_card_exp.getText().toString());
                                                refundRequestModel.setAmount(orderModel.getFinalPayment());

                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.REQUEST_REFUND_MODEL)
                                                        .child(orderModel.getOrderNumber())
                                                        .setValue(refundRequestModel)
                                                        .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                        .addOnSuccessListener(aVoid -> {

                                                            //Update Firebase
                                                            Map<String,Object> update_data = new HashMap<>();
                                                            update_data.put("orderStatus", -1); //Cancel order
                                                            FirebaseDatabase.getInstance()
                                                                    .getReference(Common.ORDER_REF)
                                                                    .child(orderModel.getOrderNumber())
                                                                    .updateChildren(update_data)
                                                                    .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                                    .addOnSuccessListener(a -> {
                                                                        orderModel.setOrderStatus(-1); // Local update
                                                                        ((MyOrdersAdapter)recycler_orders.getAdapter()).setItemAtPosition(pos,orderModel);
                                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
                                                                        Toast.makeText(getContext(), "Cancel order succesfully!", Toast.LENGTH_SHORT).show();
                                                                    });

                                                        });
                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                }

                            }
                            else
                            {
                                Toast.makeText(getContext(), new StringBuilder("Your order was changed to ")
                                        .append(Common.convertStatusToText(orderModel.getOrderStatus()))
                                        .append(", so you can't cancel it!"), Toast.LENGTH_SHORT).show();
                            }

                        }));
            }
        };

    }

    @Override
    public void onLoadOrderSuccess(List<OrderModel> orderList) {
        dialog.dismiss();
        viewOrdersViewModel.setMutableLiveDataOrderList(orderList);
    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}