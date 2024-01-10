package com.example.janbarktask.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.janbarktask.databinding.ActivityImageViewBinding

class ImageViewActivity : AppCompatActivity() {
    private lateinit var imageViewBinding: ActivityImageViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageViewBinding = ActivityImageViewBinding.inflate(layoutInflater)
        setContentView(imageViewBinding.root)

       supportActionBar?.hide() // Hide the action bar
        // Fullscreen mode for Android R (API 30) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // Retrieve data from intent
        val screenshotUriString = intent.getStringExtra("screenshotUri")
        val position = intent.getIntExtra("position", -1) // Default to -1 if not found

        if (screenshotUriString != null) {
            val screenshotUri = Uri.parse(screenshotUriString)
            Glide.with(this).load(screenshotUri).into(imageViewBinding.selectedImageView)
        }

        imageViewBinding.backButton.setOnClickListener(View.OnClickListener { finish() })
        imageViewBinding.lLDeleteBtn.setOnClickListener(View.OnClickListener {  })
    }
}