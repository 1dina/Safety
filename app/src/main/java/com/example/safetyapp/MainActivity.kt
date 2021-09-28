package com.example.safetyapp

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.safetyapp.model.JWS
import com.example.safetyapp.model.JWSRequest
import com.example.safetyapp.model.Response
import com.example.safetyapp.network.RetrofitInterface
import com.example.safetyapp.utilities.Util
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi.AttestationResponse
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

private var mResult: String? = null
private lateinit var mGoogleApiClient: GoogleApiClient
private val mProgress: ProgressBar? = null
private val txtStatus: TextView? = null


class MainActivity : AppCompatActivity() , GoogleApiClient.ConnectionCallbacks {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initClient()
//        if (
//            GoogleApiAvailability.getInstance()
//                .isGooglePlayServicesAvailable(this, 13000000) ==
//            ConnectionResult.SUCCESS
//        ) {
//            // The SafetyNet Attestation API is available.
//
//            findViewById<Button>(R.id.button).setOnClickListener {
//                checkSafetyNet()
//
//            }
//
//
//        } else {
//            // Prompt user to update Google Play Services.
//            Toast.makeText(this, "Not Support", Toast.LENGTH_SHORT).show()
//        }


    }
    fun checkSafetyNet(){
        val client = SafetyNet.getClient(this)
        client.attest(
            "R2Rra24fVm5xa2Mg".toByteArray(),
            "AIzaSyACnNkimv5cAJROPmur8FByruhAWvWH8C8"
        )

            .addOnSuccessListener(this, mSuccessListener)
            .addOnFailureListener(this) {
                Log.d("TAG", "NO INTERNET")
            }


    }
    private val mSuccessListener =
        OnSuccessListener<AttestationResponse> { attestationResponse -> /*
                           Successfully communicated with SafetyNet API.
                           Use result.getJwsResult() to get the signed result data. See the server
                           component of this sample for details on how to verify and parse this result.
                           */
            mResult = attestationResponse.jwsResult

            Log.d(
                "TAG",
                "Success! SafetyNet result:\n$mResult\n"
            )

            /*
                               TODO(developer): Forward this result to your server together with
                               the nonce for verification.
                               You can also parse the JwsResult locally to confirm that the API
                               returned a response by checking for an 'error' field first and before
                               retrying the request with an exponential backoff.

                               NOTE: Do NOT rely on a local, client-side only check for security, you
                               must verify the response on a remote server!
                              */
        }


    private fun initClient() {
    //    mProgress!!.visibility = View.VISIBLE
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(SafetyNet.API)
            .addConnectionCallbacks(this)
            .build()
        mGoogleApiClient.connect()
    }

    override fun onConnected(bundle: Bundle?) {
        startVerification()
    }

    override fun onConnectionSuspended(i: Int) {}

    private fun startVerification() {
        val nonce = getRequestNonce()
        SafetyNet.getClient(this).attest(nonce, getString(R.string.api_key))
            .addOnSuccessListener(
                this
            ) { response -> // Indicates communication with the service was successful.
                // Use response.getJwsResult() to get the result data.
                val jwsResult = response.jwsResult
                verifyOnline(jwsResult)
            }
            .addOnFailureListener(this) { e ->
             //   mProgress!!.visibility = View.GONE
                // An error occurred while communicating with the service.
                val error: String
                error = if (e is ApiException) {
                    // An error with the Google Play services API contains some
                    // additional details.
                    // You can retrieve the status code using the
                    // apiException.getStatusCode() method.
                    e.localizedMessage
                } else {
                    // A different, unknown type of error occurred.
                    e.localizedMessage
                }
                Util.showAlert(this@MainActivity, "Verification",
                    "Unable to perform operation due to :$error", "Okay",
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                        finish()
                    }, DialogInterface.OnKeyListener { arg0, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                        }
                        true
                    })
            }
    }

    private fun verifyOnline(jws: String) {
        var retrofit: Retrofit? = null
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(getString(R.string.base_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val retrofitInterface: RetrofitInterface =
                retrofit.create(RetrofitInterface::class.java)
            val jwsRequest = JWSRequest()
            jwsRequest.setSignedAttestation(jws)
            val responseCall: Call<Response> =
                retrofitInterface.getResult(jwsRequest, getString(R.string.api_key))
            responseCall.enqueue(object : Callback<Response> {
                override fun onResponse(
                    call: Call<Response>,
                    response: retrofit2.Response<Response>
                ) {
                    val result: Boolean = response.body()?.isValidSignature()!!
                    if (result) {
                        decodeJWS(jws)
                    } else {
                        Util.showAlert(this@MainActivity,
                            "Verification Error!",
                            "Unable to perform operation due to invalid signature",
                            "Okay",
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                finish()
                            },
                            DialogInterface.OnKeyListener { arg0, keyCode, event ->
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                }
                                true
                            })
                    }
                }

                override fun onFailure(call: Call<Response>, t: Throwable) {
                   // mProgress!!.visibility = View.GONE
                    Util.showAlert(this@MainActivity,
                        "Verification",
                        "Something went wrong : " + t.localizedMessage,
                        "Okay",
                        DialogInterface.OnClickListener { dialog, which ->
                            dialog.dismiss()
                            finish()
                        },
                        DialogInterface.OnKeyListener { arg0, keyCode, event ->
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                            }
                            true
                        })
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decodeJWS(jwsString: String) {
        val Result = findViewById<TextView>(R.id.text)
        val json = Base64.decode(jwsString.split("[.]".toRegex()).toTypedArray()[1], Base64.DEFAULT)
        val text = String(json, StandardCharsets.UTF_8)
        val gson = Gson()
        val jws: JWS = gson.fromJson(text, JWS::class.java)
        displayResults(jws.isBasicIntegrity(), jws.isCtsProfileMatch())
        if (jws.isBasicIntegrity){
            Result.setText("YOUR DEVICE IS SAFE !!")

        }else{
            Result.setText("YOUR DEVICE IS ROOTED !!")
        }
    }

    private fun getRequestNonce(): ByteArray? {
        val data = System.currentTimeMillis().toString()
        val byteStream = ByteArrayOutputStream()
        val bytes = ByteArray(24)
        val random = Random()
        random.nextBytes(bytes)
        try {
            byteStream.write(bytes)
            byteStream.write(data.toByteArray())
        } catch (e: IOException) {
            return null
        }
        return byteStream.toByteArray()
    }

    private fun displayResults(integrity: Boolean, cts: Boolean) {
      //  mProgress!!.visibility = View.GONE
        if (integrity && cts) {
           // txtStatus!!.visibility = View.VISIBLE
            Util.showAlert(this@MainActivity, "Verification", "Your Device is verified", "Okay",
                DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() },
                DialogInterface.OnKeyListener { arg0, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                    }
                    true
                })
        } else {
            Util.showAlert(this@MainActivity,
                "Verification",
                "Your device compatibility test check failed, Probably your device is tampered",
                "Okay",
                DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                    finish()
                },
                DialogInterface.OnKeyListener { arg0, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                    }
                    true
                })
        }
    }


   }

