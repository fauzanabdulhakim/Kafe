package com.fauzan.kafe.Common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.fauzan.kafe.Model.AddonModel
import com.fauzan.kafe.Model.CategoryModel
import com.fauzan.kafe.Model.FoodModel
import com.fauzan.kafe.Model.SizeModel
import com.fauzan.kafe.Model.TokenModel
import com.fauzan.kafe.Model.UserModel
import com.fauzan.kafe.R
import com.fauzan.kafe.domain.calculator.PriceCalculator
import com.fauzan.kafe.domain.mapper.OrderStatusMapper
import com.fauzan.kafe.domain.session.ClientSessionManager
import com.google.firebase.database.FirebaseDatabase
import java.util.Random

/**
 * Common constants, utility bridges, and backward-compatible wrappers.
 * Business calculations and mutable session states are delegated to clean Domain components.
 */
object Common {
    const val USER_REFERENCES: String = "Users"
    const val POPULAR_CATEGORY_REF: String = "MostPopular"
    const val BEST_DEAL_REF: String = "BestDeals"
    const val DEFAULT_COLUMN_COUNT: Int = 0
    const val FULL_WIDTH_COLUMN: Int = 1
    const val CATEGORY_REF: String = "Category"
    const val COMMENT_REF: String = "Comments"
    const val ORDER_REF: String = "Order"
    const val NOTI_TITLE: String = "title"
    const val NOTI_CONTENT: String = "content"
    const val REQUEST_REFUND_MODEL: String = "RequestRefund"
    private const val TOKEN_REF: String = "Tokens"

    private val priceCalculator = PriceCalculator()
    private val orderStatusMapper = OrderStatusMapper()

    // Legacy fields maintained for backward compatibility
    @JvmField
    var currentUser: UserModel? = null
    @JvmField
    var categorySelected: CategoryModel? = null
    @JvmField
    var selectedFood: FoodModel? = null
    @JvmField
    var currentToken: String = ""
    @JvmField
    var authorizeKey: String = ""

    /**
     * Synchronizes legacy public static fields into the scoped ClientSessionManager.
     */
    @JvmStatic
    fun syncToSession() {
        val session = ClientSessionManager.getInstance()
        session.currentUser = currentUser
        session.selectedCategory = categorySelected
        session.selectedFood = selectedFood
        session.currentToken = currentToken
        session.authorizeKey = authorizeKey
    }

    @JvmStatic
    fun formatPrice(price: Double): String {
        return priceCalculator.formatPrice(price)
    }

    @JvmStatic
    fun calculateExtraPrice(userSelectedSize: SizeModel?, userSelectedAddon: List<AddonModel>?): Double {
        return priceCalculator.calculateExtraPrice(userSelectedSize, userSelectedAddon)
    }

    @JvmStatic
    fun setSpanString(welcome: String, name: String, textView: TextView) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val spannableString = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        spannableString.setSpan(boldSpan, 0, name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(spannableString)
        textView.setText(builder, TextView.BufferType.SPANNABLE)
    }

    @JvmStatic
    fun createOrderNumber(): String {
        return StringBuilder()
            .append(System.currentTimeMillis())
            .append(Math.abs(Random().nextInt()))
            .toString()
    }

    @JvmStatic
    fun buildToken(authorizeKey: String): String {
        return "Bearer $authorizeKey"
    }

    @JvmStatic
    fun getDateOfWeek(i: Int): String {
        return orderStatusMapper.getDateOfWeek(i)
    }

    @JvmStatic
    fun convertStatusToText(orderStatus: Int): String {
        return orderStatusMapper.convertStatusToText(orderStatus)
    }

    @JvmStatic
    fun showNotification(context: Context, id: Int, title: String?, content: String?, intent: Intent?) {
        var pendingIntent: PendingIntent? = null
        if (intent != null) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            pendingIntent = PendingIntent.getActivity(context, id, intent, flags)
        }
        val channelId = "com_fauzan_kafe"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId, "Kafe", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "Kafe"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_restaurant_menu_black_24dp))
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        val notification = builder.build()
        notificationManager.notify(id, notification)
    }

    @JvmStatic
    fun updateToken(context: Context, newToken: String) {
        val user = currentUser
        if (user?.uid != null) {
            FirebaseDatabase.getInstance()
                .getReference(TOKEN_REF)
                .child(user.uid!!)
                .setValue(TokenModel(user.phone, newToken))
                .addOnFailureListener { e ->
                    Toast.makeText(context, "${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
