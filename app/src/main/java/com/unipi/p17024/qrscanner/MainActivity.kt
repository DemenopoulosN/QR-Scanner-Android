package com.unipi.p17024.qrscanner

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.unipi.p17024.qrscanner.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding

    private var codeScanner: CodeScanner? = null

    private lateinit var databaseRef: DatabaseReference
    private lateinit var tokensRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-e-tickets-android-wearos-default-rtdb.firebaseio.com/")
        tokensRef = databaseRef.child("Tokens")

        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initiateScan()
        } else {
            // request for Camera permission
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1)
        }
    }

    // this function is called if the permission dialog is dismissed
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                initiateScan()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateScan() {
        val scannerView = binding.codeScannerView
        codeScanner = CodeScanner(this, scannerView)

        // The default values
        codeScanner?.camera = CodeScanner.CAMERA_BACK
        codeScanner?.formats = CodeScanner.ALL_FORMATS

        codeScanner?.apply {
            isAutoFocusEnabled = true
            isFlashEnabled = false
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
        }

        // this is called after the code is captured
        codeScanner!!.decodeCallback = DecodeCallback {
            runOnUiThread {

                // Read from the database
                tokensRef.addValueEventListener(object: ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists() && snapshot.hasChild(it.text.toString())){
                            val timestamp:Long  = snapshot.child(it.text.toString()).child("timestamp").value as Long
                            if ( timestamp > (System.currentTimeMillis() - 30000)){ //30seconds
                                val userId = snapshot.child(it.text.toString()).child("userID").value

                                //starting MessageActivity
                                val intent = Intent(this@MainActivity, MessageActivity::class.java)
                                intent.putExtra("token", it.text.toString())
                                intent.putExtra("userID", userId.toString())
                                intent.putExtra("invalid", "no")
                                startActivity(intent)
                            }
                            else{
                                Toast.makeText(this@MainActivity, "Database access denied. Please try again with another QR",Toast.LENGTH_SHORT).show()

                                val mIntent = intent
                                finish()
                                startActivity(mIntent)
                            }

                        }
                        else{
                            //Snackbar.make(scannerView, "There is no such user", 5000).show()

                            //starting MessageActivity
                            val intent = Intent(this@MainActivity, MessageActivity::class.java)
                            intent.putExtra("token", "null")
                            intent.putExtra("userID", "null")
                            intent.putExtra("invalid", "yes")
                            startActivity(intent)
                        }

                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Failed to read value.", error.toException())
                        Toast.makeText(this@MainActivity, "Database access denied. Please try again with another QR",Toast.LENGTH_LONG).show()

                        val mIntent = intent
                        finish()
                        startActivity(mIntent)
                    }
                })


                //Snackbar.make(scannerView, "Scan result: ${it.text}", 5000).show()
            }
        }

        // Whenever an error occurs when decoding
        codeScanner?.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(
                    this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Restart scan process when the scanner is tapped
        scannerView.setOnClickListener {
            codeScanner!!.startPreview()
        }
    }

    // when the app resumes
    override fun onResume() {
        super.onResume()
        codeScanner?.startPreview()
    }

    // just before the app is paused
    override fun onPause() {
        codeScanner?.releaseResources()
        super.onPause()
    }
}