package com.unipi.p17024.qrscanner

import android.app.Activity
import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.unipi.p17024.qrscanner.databinding.ActivityMainBinding
import com.unipi.p17024.qrscanner.databinding.ActivityMessageBinding

class MessageActivity : Activity() {
    private lateinit var binding: ActivityMessageBinding

    private lateinit var databaseRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://smart-e-tickets-android-wearos-default-rtdb.firebaseio.com/")

        val token = intent.getStringExtra("token").toString()
        val userID = intent.getStringExtra("userID").toString()
        val invalid = intent.getStringExtra("invalid").toString()

        databaseRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("Clients").child(userID).child("Valid Subscription").value.toString() == "yes"){
                    binding.textMessage.text = "Valid"
                    binding.imageView.setImageResource(R.drawable.green_check)
                    //binding.textName.text = snapshot.child("Clients").child(userID).child("Name").value.toString()
                    //binding.textSurname.text = snapshot.child("Clients").child(userID).child("Surname").value.toString()

                    // Delete token child after elapsed time
                    Handler(Looper.getMainLooper()).postDelayed({
                        databaseRef.child("Tokens").child(token).removeValue()
                    }, 20000) //20 seconds
                }
                else if(invalid == "yes"){
                    binding.textMessage.text = "No such user exists"
                    binding.imageView.setImageResource(R.drawable.imageedit_3_2234035677_r)
                }
                else{
                    binding.textMessage.text = "No valid subscription"
                    binding.imageView.setImageResource(R.drawable.imageedit_3_2234035677_y)
                    //binding.textName.text = snapshot.child("Clients").child(userID).child("Name").value.toString()
                    //binding.textSurname.text = snapshot.child("Clients").child(userID).child("Surname").value.toString()


                    // Delete token child after elapsed time
                    Handler(Looper.getMainLooper()).postDelayed({
                        databaseRef.child("Tokens").child(token).removeValue()
                    }, 20000) //20 seconds
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
                Toast.makeText(this@MessageActivity, "Database access denied. Please try again with another QR",
                    Toast.LENGTH_LONG).show()
            }
        })

    }
}