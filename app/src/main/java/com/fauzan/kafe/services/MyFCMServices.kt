package com.fauzan.kafe.services

import com.fauzan.kafe.Common.Common
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class MyFCMServices : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val dataRecv = remoteMessage.data
        if (dataRecv.isNotEmpty()) {
            Common.showNotification(
                this,
                Random().nextInt(),
                dataRecv[Common.NOTI_TITLE],
                dataRecv[Common.NOTI_CONTENT],
                null
            )
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Common.updateToken(this, s)
    }
}
