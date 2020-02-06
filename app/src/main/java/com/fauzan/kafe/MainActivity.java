package com.fauzan.kafe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import com.fauzan.kafe.Common.Common;
import com.fauzan.kafe.Model.BraintreeToken;
import com.fauzan.kafe.Model.UserModel;
import com.fauzan.kafe.Remote.ICloudFunction;
import com.fauzan.kafe.Remote.RetrofitCloudClient;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealResponseBody;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171; //nomor berapapun
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ICloudFunction cloudFunction;
    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig> providers;

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener !=null)
            firebaseAuth.removeAuthStateListener(listener);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init(){
        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());
        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        cloudFunction = RetrofitCloudClient.getInstance().create(ICloudFunction.class);
        listener = firebaseAuth -> {

            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user !=null)
                            {
                                checkUserFromFirebase(user);

                            }
                            else {
                                phoneLogin();
                            }
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(MainActivity.this, "You must enable this permission to use app", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                        }
                    }).check();

        };
    }

    private void checkUserFromFirebase(FirebaseUser user) {
        dialog.show();
        userRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {

                            FirebaseAuth.getInstance().getCurrentUser()
                                    .getIdToken(true)
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnCompleteListener(tokenResultTask -> {
                                        Common.authorizeKey = tokenResultTask.getResult().getToken();

                                        Map<String,String> headers = new HashMap<>();
                                        headers.put("Authorization",Common.buildToken(Common.authorizeKey));

                                        compositeDisposable.add(cloudFunction.getToken(headers)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(braintreeToken -> {

                                                    dialog.dismiss();
                                                    UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                                    goToHomeActivity(userModel,braintreeToken.getToken());

                                                }, throwable -> {
                                                    dialog.dismiss();
                                                    Toast.makeText(MainActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                }));
                                    });

                        } else {
                            showRegisterDialog(user);
                            dialog.dismiss();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this,""+databaseError.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);
        EditText edt_name = (EditText)itemView.findViewById(R.id.edt_name);
        EditText edt_address = (EditText)itemView.findViewById(R.id.edt_address);
        EditText edt_phone = (EditText)itemView.findViewById(R.id.edt_phone);

        //set
        edt_phone.setText(user.getPhoneNumber());

        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("REGISTER", (dialogInterface, i) -> {
            if (TextUtils.isEmpty(edt_name.getText().toString())) {
                Toast.makeText(this,"Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(edt_address.getText().toString())) {
                Toast.makeText(this,"Please enter your address", Toast.LENGTH_SHORT).show();
                return;
            }

            UserModel userModel = new UserModel();
            userModel.setUid(user.getUid());
            userModel.setName(edt_name.getText().toString());
            userModel.setAddress(edt_address.getText().toString());
            userModel.setPhone(edt_phone.getText().toString());

            userRef.child(user.getUid())
                    .setValue(userModel)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            FirebaseAuth.getInstance().getCurrentUser()
                                    .getIdToken(true)
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnCompleteListener(tokenResultTask -> {
                                        Common.authorizeKey = tokenResultTask.getResult().getToken();
                                        Map<String,String> headers = new HashMap<>();
                                        headers.put("Authorization",Common.buildToken(Common.authorizeKey));
                                        compositeDisposable.add(cloudFunction.getToken(headers)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(braintreeToken -> {

                                                    dialogInterface.dismiss();
                                                    Toast.makeText(MainActivity.this,"Congratilation ! Register Success",Toast.LENGTH_SHORT).show();
                                                    goToHomeActivity(userModel,braintreeToken.getToken());

                                                }, throwable -> {
                                                    dialog.dismiss();
                                                    Toast.makeText(MainActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                }));
                                    });

                        }
                    });

        });

        builder.setView(itemView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void goToHomeActivity(UserModel userModel,String token) {

        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    Common.currentUser = userModel;  //Important , you need always assign value for it before use
                    Common.currentToken = token;
                    startActivity(new Intent(MainActivity.this,HomeActivity.class));
                    finish();
                }).addOnCompleteListener(task -> {

                    Common.currentUser = userModel;
                    Common.currentToken = token;
                    Common.updateToken(MainActivity.this,task.getResult().getToken());
                    startActivity(new Intent(MainActivity.this,HomeActivity.class));
                    finish();
                });




    }

    private void phoneLogin() {

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else
            {
                Toast.makeText(this,"Failed to sign in!",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
