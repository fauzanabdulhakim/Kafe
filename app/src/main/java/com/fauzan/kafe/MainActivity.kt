package com.fauzan.kafe

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fauzan.kafe.Common.Common
import com.fauzan.kafe.Model.UserModel
import com.fauzan.kafe.Remote.ICloudFunction
import com.fauzan.kafe.Remote.RetrofitCloudClient
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private var listener: FirebaseAuth.AuthStateListener? = null
    private var dialog: AlertDialog? = null
    private val compositeDisposable = CompositeDisposable()
    private lateinit var cloudFunction: ICloudFunction
    private lateinit var userRef: DatabaseReference
    private lateinit var providers: List<AuthUI.IdpConfig>

    companion object {
        private const val APP_REQUEST_CODE = 7171
    }

    override fun onStart() {
        super.onStart()
        listener?.let { firebaseAuth.addAuthStateListener(it) }
    }

    override fun onStop() {
        listener?.let { firebaseAuth.removeAuthStateListener(it) }
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init() {
        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setCancelable(true).setContext(this).build()
        cloudFunction = RetrofitCloudClient.getInstance().create(ICloudFunction::class.java)

        listener = FirebaseAuth.AuthStateListener { auth ->
            Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        val user = auth.currentUser
                        if (user != null) {
                            checkUserFromFirebase(user)
                        } else {
                            phoneLogin()
                        }
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        Toast.makeText(
                            this@MainActivity,
                            "You must enable this permission to use app",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest,
                        token: PermissionToken
                    ) {
                    }
                }).check()
        }
    }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog?.show()
        userRef.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        FirebaseAuth.getInstance().currentUser
                            ?.getIdToken(true)
                            ?.addOnFailureListener { e ->
                                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnCompleteListener { tokenResultTask ->
                                Common.authorizeKey = tokenResultTask.result?.token ?: ""

                                val headers = HashMap<String, String>()
                                headers["Authorization"] = Common.buildToken(Common.authorizeKey)

                                compositeDisposable.add(
                                    cloudFunction.getToken(headers)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(
                                            { braintreeToken ->
                                                dialog?.dismiss()
                                                val userModel = dataSnapshot.getValue(UserModel::class.java)
                                                goToHomeActivity(userModel, braintreeToken.token)
                                            },
                                            { throwable ->
                                                dialog?.dismiss()
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    throwable.message,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                )
                            }
                    } else {
                        showRegisterDialog(user)
                        dialog?.dismiss()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    dialog?.dismiss()
                    Toast.makeText(this@MainActivity, databaseError.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Register")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)
        val edtName = itemView.findViewById<EditText>(R.id.edt_name)
        val edtAddress = itemView.findViewById<EditText>(R.id.edt_address)
        val edtPhone = itemView.findViewById<EditText>(R.id.edt_phone)

        edtPhone.setText(user.phoneNumber)

        builder.setView(itemView)
        builder.setNegativeButton("CANCEL") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        builder.setPositiveButton("REGISTER") { dialogInterface, _ ->
            val name = edtName.text.toString().trim()
            val address = edtAddress.text.toString().trim()

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            } else if (TextUtils.isEmpty(address)) {
                Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val userModel = UserModel(
                name = name,
                address = address,
                phone = edtPhone.text.toString().trim(),
                uid = user.uid
            )

            userRef.child(user.uid)
                .setValue(userModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        FirebaseAuth.getInstance().currentUser
                            ?.getIdToken(true)
                            ?.addOnFailureListener { e ->
                                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnCompleteListener { tokenResultTask ->
                                Common.authorizeKey = tokenResultTask.result?.token ?: ""
                                val headers = HashMap<String, String>()
                                headers["Authorization"] = Common.buildToken(Common.authorizeKey)
                                compositeDisposable.add(
                                    cloudFunction.getToken(headers)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(
                                            { braintreeToken ->
                                                dialogInterface.dismiss()
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Congratilation ! Register Success",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                goToHomeActivity(userModel, braintreeToken.token)
                                            },
                                            { throwable ->
                                                dialog?.dismiss()
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    throwable.message,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                )
                            }
                    }
                }
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun goToHomeActivity(userModel: UserModel?, token: String?) {
        FirebaseMessaging.getInstance().token
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                Common.currentUser = userModel
                Common.currentToken = token ?: ""
                Common.syncToSession()
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
            }.addOnCompleteListener { task ->
                Common.currentUser = userModel
                Common.currentToken = token ?: ""
                Common.syncToSession()
                task.result?.let { fcmToken ->
                    Common.updateToken(this@MainActivity, fcmToken)
                }
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
            }
    }

    private fun phoneLogin() {
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.cafe)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .build(),
            APP_REQUEST_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this, "Failed to sign in!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
