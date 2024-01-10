package com.example.janbarktask.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.janbarktask.utils.Constants.AD_UNIT_NATIVE_ID
import com.example.janbarktask.R
import com.example.janbarktask.databinding.ItemRecyclerviewBinding
import com.example.janbarktask.databinding.PopMenuLayoutBinding
import com.example.janbarktask.interfaces.AdListener
import com.example.janbarktask.interfaces.MenuClickListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class ScreenshotsAdapter(
    private val screenshots: MutableList<Uri>,
    private val itemMenuClickListener: MenuClickListener,
    private val adListener: AdListener
) :RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val VIEW_TYPE_SCREENSHOT = 0
        const val VIEW_TYPE_AD = 1
    }

    override fun getItemViewType(position: Int): Int {
        // Show an ad after every 6 items (after every two rows)
        return if ((position + 1) % 7 == 0) VIEW_TYPE_AD else VIEW_TYPE_SCREENSHOT
    }

    inner class ViewHolder(private val binding: ItemRecyclerviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context = binding.root.context
        fun bind(screenshotUri: Uri, position: Int) {
            binding.apply {
                Glide.with(itemImage.context)
                    .load(screenshotUri)
                    .into(itemImage)

                itemImage.setOnClickListener(View.OnClickListener {
                    adListener.showAd()
                })


                menuButton.setOnClickListener {
                    showMenuDialog(it, screenshotUri,position)
                }


            }
        }
    }

    inner class AdViewHolder(private val adView: NativeAdView) : RecyclerView.ViewHolder(adView) {
        fun bind() {
            // Load the native ad
            val adLoader = AdLoader.Builder(adView.context, AD_UNIT_NATIVE_ID)
                .forNativeAd { nativeAd ->
                    // Assuming you have a method to populate the ad view
                    populateNativeAdView(nativeAd, adView)
                }
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }}

    private fun showMenuDialog(anchorView: View, screenshotUri: Uri,position: Int) {
        // Inflate the layout using View Binding
        val binding = PopMenuLayoutBinding.inflate(LayoutInflater.from(anchorView.context))

        // Create a PopupWindow with the binding root
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // Focusable
        )

        // Set Click Listeners for each option in the layout using View Binding
        binding.mTextViewOpenImage.setOnClickListener {
            //openImage(anchorView.context, screenshotUri)
            itemMenuClickListener.openImageListener(position,screenshotUri )
            popupWindow.dismiss()
        }
        binding.mTextViewShareImage.setOnClickListener {
            itemMenuClickListener.shareImageListener(position,screenshotUri)
            popupWindow.dismiss()
        }
        binding.mTextViewDeleteImage.setOnClickListener {
            itemMenuClickListener.deleteImageListener(position,screenshotUri )
            popupWindow.dismiss()
        }

        // Show the PopupWindow
        popupWindow.showAsDropDown(anchorView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            // Inflate the ad view
            val adViewBinding = LayoutInflater.from(parent.context).inflate(R.layout.admob_native_fifty, parent, false)
            AdViewHolder(adViewBinding as NativeAdView)
        } else {
            // Inflate the screenshot view
            val screenshotBinding = ItemRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ViewHolder(screenshotBinding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_AD) {
            // Bind ad view
            (holder as AdViewHolder).bind()
        } else {
            // Calculate the actual position of the screenshot
            val screenshotPosition = position - (position / 3)
            if (screenshotPosition < screenshots.size) {
                val screenshotUri = screenshots[screenshotPosition]
                (holder as ViewHolder).bind(screenshotUri, screenshotPosition)
            }
        }
    }



    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Populate the adView with the content of the nativeAd
        // Bind the ad elements with the adView's components
        adView.findViewById<TextView>(R.id.ad_headline).text = nativeAd.headline
        adView.findViewById<ImageView>(R.id.ad_app_icon).setImageDrawable(nativeAd.icon?.drawable)
        adView.findViewById<Button>(R.id.ad_call_to_action).text = nativeAd.callToAction

        // Register the NativeAdView
        adView.setNativeAd(nativeAd)
    }

    override fun getItemCount(): Int {
        // Calculate total items including ads
        val totalContentItems = screenshots.size
        val additionalAds = (totalContentItems + 5) / 6
        return totalContentItems + additionalAds
    }

    fun updateData(newData: List<Uri>) {
        screenshots.clear()
        screenshots.addAll(newData)
        notifyDataSetChanged()
    }
}



