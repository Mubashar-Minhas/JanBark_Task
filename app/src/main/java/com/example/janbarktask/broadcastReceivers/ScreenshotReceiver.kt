package com.example.janbarktask.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import java.io.OutputStream


class ScreenshotReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Close the notification tray
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(closeIntent)

        /*val activity = MainActivity.currentInstance
        activity?.let { act ->
            val rootView = act.window.decorView.findViewById<View>(android.R.id.content)
            val screenshot = takeScreenshot(rootView)
            saveImageToGallery(context, screenshot, "Janbark")
        }*/
    }


    private fun saveImageToGallery(context: Context, bitmap: Bitmap, albumName: String): Uri? {
        val filename = "${System.currentTimeMillis()}.jpg"
        val write: (OutputStream) -> Boolean = {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$albumName")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use(write)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        }
        return uri
    }


    private fun takeScreenshot(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


}
