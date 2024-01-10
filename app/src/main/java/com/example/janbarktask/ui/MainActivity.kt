package com.example.janbarktask.ui

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.janbarktask.utils.Constants.AD_UNIT_INTERSTITIAL_ID
import com.example.janbarktask.adapters.ScreenshotsAdapter
import com.example.janbarktask.databinding.ActivityMainBinding
import com.example.janbarktask.interfaces.AdListener
import com.example.janbarktask.interfaces.MenuClickListener
import com.example.janbarktask.utils.GoogleMobileAdsConsentManager
import com.example.janbarktask.utils.MyForegroundService
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), MenuClickListener, AdListener {
    companion object {
        var currentInstance: MainActivity? = null
        const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }


    private lateinit var adapter: ScreenshotsAdapter
    private lateinit var mainActivityBinding: ActivityMainBinding
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var adIsLoading: Boolean = false
    private var screenshots = mutableListOf<Uri>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityBinding = ActivityMainBinding.inflate(layoutInflater)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else {
            // For versions below Android 13, proceed without checking
            startYourForegroundService()
        }

         initViews()




        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(this)
        checkingConsent()
        if (googleMobileAdsConsentManager.canRequestAds) {
            initializeMobileAdsSdk()
        }

        setContentView(mainActivityBinding.root)
    }


    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check for permission on Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, start the service
                startYourForegroundService()
            } else {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            startYourForegroundService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted
                startYourForegroundService()
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun startYourForegroundService() {
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun setAdapterView() {
        lifecycleScope.launch {
             screenshots = loadScreenshotsFromStorage(this@MainActivity)
            if (screenshots.isNotEmpty()) {
                mainActivityBinding.emptyFolder.visibility= View.GONE
                adapter = ScreenshotsAdapter(screenshots, this@MainActivity, this@MainActivity)
                val layoutManager = GridLayoutManager(this@MainActivity, 3)

                // Adjust span size for different view types
                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter.getItemViewType(position)) {
                            ScreenshotsAdapter.VIEW_TYPE_AD -> 3 // Full width for ads
                            ScreenshotsAdapter.VIEW_TYPE_SCREENSHOT -> 1 // Default span for screenshots
                            else -> 1
                        }
                    }
                }
                mainActivityBinding.mRecyclerview.layoutManager = layoutManager
                mainActivityBinding.mRecyclerview.adapter = adapter

            } else {
                Toast.makeText(this@MainActivity, "Empty", Toast.LENGTH_SHORT).show()
                mainActivityBinding.emptyFolder.visibility= View.VISIBLE
            }

        }
    }

    private fun initViews() {
        mainActivityBinding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        setAdapterView()
    }

    private fun refreshData() {
        lifecycleScope.launch {
            // Load or reload your data
            val screenshots = loadScreenshotsFromStorage(this@MainActivity)
            adapter.updateData(screenshots)

            // Hide the refresh indicator
            mainActivityBinding.swipeRefreshLayout.isRefreshing = false
        }
    }
    private fun checkingConsent() {
        googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
            if (consentError != null) {
                // Consent not obtained in current session.
                Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }
            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                invalidateOptionsMenu()
            }
        }
    }


    private suspend fun loadScreenshotsFromStorage(context: Context): MutableList<Uri> =
        withContext(Dispatchers.IO) {
            val screenshots = mutableListOf<Uri>()
            val projection = arrayOf(MediaStore.Images.Media._ID)

            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.MIME_TYPE} = ?"
            } else {
                "${MediaStore.Images.Media.DATA} LIKE ?"
            }

            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf("Pictures/Janbark/", "image/jpeg") // Adjust MIME type if needed
            } else {
                arrayOf("%/Janbark/%.jpg")
            }

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    screenshots.add(contentUri)
                }
            }

            screenshots
        }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            intent.getStringExtra("action")?.let { action ->
                if (action == "take_screenshot") {
                    val rootView = window.decorView.findViewById<View>(android.R.id.content)
                    val screenshot = takeScreenshot(rootView)
                    if (screenshot != null) {
                        saveImageToGallery(this, screenshot, "Janbark")
                    } else {
                        Toast.makeText(this, "no ss", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }
    }


    private fun takeScreenshot(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    private fun saveImageToGallery(context: Context, bitmap: Bitmap, albumName: String): Uri? {
        val filename = "${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // For Android Q and above, save image to the media directory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$albumName")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            context.contentResolver.openOutputStream(it).use { outStream ->
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
        }

        return uri
    }


    override fun onResume() {
        super.onResume()
        currentInstance = this
    }

    override fun onPause() {
        super.onPause()
        currentInstance = null
    }

    override fun openImageListener(position: Int, screenshotUri: Uri) {
        val intent = Intent(this, ImageViewActivity::class.java).apply {
            // Pass the URI as a string; URIs are not always directly serializable or parcelable
            putExtra("screenshotUri", screenshotUri.toString())
            putExtra("position", position)
        }
        this.startActivity(intent)

    }

    override fun shareImageListener(position: Int, screenshotUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, screenshotUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        this.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }


    override fun deleteImageListener(position: Int, screenshotUri: Uri) {
        if (deleteImageFile(this, screenshotUri)) {
            // Update your adapter data set
            //yourDataList.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, adapter.itemCount)
        } else {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
        }
    }


    private fun deleteImageFile(context: Context, imageUri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above
            deleteImageUsingContentResolver(context, imageUri)
        } else {
            // For Android 9 and below
            deleteImageUsingFile(imageUri)
        }
    }

    private fun deleteImageUsingContentResolver(context: Context, imageUri: Uri): Boolean {
        return try {
            val deleteCount = context.contentResolver.delete(imageUri, null, null)
            deleteCount > 0
        } catch (e: Exception) {
            Log.e("DeleteImage", "Error deleting image", e)
            false
        }
    }

    private fun deleteImageUsingFile(imageUri: Uri): Boolean {
        return try {
            val file = File(imageUri.path)
            file.delete()
        } catch (e: Exception) {
            Log.e("DeleteImage", "Error deleting image", e)
            false
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) { initializationStatus ->
            // Load an ad.
            loadAd()
        }
    }

    private fun loadAd() {
        // Request a new ad if one isn't already loaded.
        if (adIsLoading || interstitialAd != null) {
            return
        }
        adIsLoading = true

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            AD_UNIT_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    interstitialAd = null
                    adIsLoading = false
                    val error =
                        "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "onAdFailedToLoad() with error $error",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ad
                    adIsLoading = false
                    //Toast.makeText(this@MainActivity, "onAdLoaded()", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    interstitialAd = null
                    loadAd() // Load next ad
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    interstitialAd = null
                    loadAd() // Attempt to load next ad
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            interstitialAd?.show(this)
        } else {
            Log.d(TAG, "Interstitial ad not loaded yet.")
            if (googleMobileAdsConsentManager.canRequestAds) {
                loadAd() // Load the ad if it's not loaded
            }
        }
    }


    override fun showAd() {
        showInterstitial()
    }


}