package com.fauzan.kafe.ui.location

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fauzan.kafe.R

class Location : AppCompatActivity() {

    private val googleMapPackage = "com.google.android.apps.maps"
    private val kafePertama = "-6.160623, 106.905426"
    private val kafeKedua = "-6.150065, 106.834324"
    private val kafeKetiga = "-6.159800, 106.855146"
    private val kafeKeempat = "-6.118393, 106.880693"
    private val kafeKelima = "-6.232477, 106.796236"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        findViewById<Button>(R.id.btn_navigasi).setOnClickListener {
            launchNavigation(kafePertama)
        }
        findViewById<Button>(R.id.btn_navigasi_2).setOnClickListener {
            launchNavigation(kafeKedua)
        }
        findViewById<Button>(R.id.btn_navigasi_3).setOnClickListener {
            launchNavigation(kafeKetiga)
        }
        findViewById<Button>(R.id.btn_navigasi_4).setOnClickListener {
            launchNavigation(kafeKeempat)
        }
        findViewById<Button>(R.id.btn_navigasi_5).setOnClickListener {
            launchNavigation(kafeKelima)
        }
    }

    private fun launchNavigation(coordinates: String) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$coordinates")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage(googleMapPackage)

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Google Maps Belum Terinstal. Install Terlebih dahulu.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
