package com.example.gesturepassword

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.gesturepassword.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lockView.setOnLockOverListener {
            Log.d(TAG, "onCreate:  ==> is callback")
            it.forEach { point ->
                Log.d(TAG, "onCreate: point ==> $point")
            }
        }
    }
}