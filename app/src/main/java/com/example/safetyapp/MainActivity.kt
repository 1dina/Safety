package com.example.safetyapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi





class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (
            GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this, 13000000) ==
                ConnectionResult.SUCCESS) {
            // The SafetyNet Attestation API is available.
            findViewById<Button>(R.id.button).setOnClickListener {
                val client = SafetyNet.getClient(this)
                client.attest(
                    "R2Rra24fVm5xa2Mg".toByteArray(),
                    "AIzaSyACnNkimv5cAJROPmur8FByruhAWvWH8C8"
                )

                    .addOnSuccessListener(
                        this
                    ) { response -> // Indicates communication with the service was successful.
                        // Use response.getJwsResult() to get the result data.
                        val jwsResult = response.jwsResult
                        Log.d("TAG","${jwsResult}")
                    }
                    .addOnFailureListener(this) {
                          Log.d("TAG", "NO INTERNET")
                    }



            }

        } else {
            // Prompt user to update Google Play Services.
            Toast.makeText(this, "Not Support", Toast.LENGTH_SHORT).show()
        }




    }

}

