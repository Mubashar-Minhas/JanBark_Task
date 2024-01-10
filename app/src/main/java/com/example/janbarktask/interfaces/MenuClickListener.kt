package com.example.janbarktask.interfaces

import android.net.Uri

interface MenuClickListener {
    fun openImageListener(position: Int, screenshotUri: Uri)

    fun shareImageListener(position: Int, screenshotUri: Uri)

    fun deleteImageListener(position: Int, screenshotUri: Uri)
}