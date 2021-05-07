package com.hypertrack.android.utils

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.hypertrack.android.decodeBase64Bitmap
import com.hypertrack.android.models.Address
import com.hypertrack.android.toBase64
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.logistics.android.github.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class OsUtilsProvider(private val context: Context, private val crashReportsProvider: CrashReportsProvider) {

    val screenDensity: Float
        get() = Resources.getSystem().displayMetrics.density

    val cacheDir: File
        get() = MyApplication.context.cacheDir

    fun getCurrentTimestamp(): String {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        df.timeZone = TimeZone.getTimeZone("UTC")
        return df.format(Date())
    }

    fun getFineDateTimeString(): String {
        val df = SimpleDateFormat("MMM d, h:mm", Locale.ENGLISH)
        df.timeZone = TimeZone.getDefault()

        val postfixFmt = SimpleDateFormat("a", Locale.ENGLISH)
        postfixFmt.timeZone = TimeZone.getDefault()

        val now = Date()
        return "${df.format(now)}${postfixFmt.format(now).toLowerCase(Locale.ENGLISH)}"
    }

    fun getAddressFromCoordinates(latitude: Double?, longitude: Double?): Address {
        if (latitude == null || longitude == null) {
            return Address(
                street = "",
                postalCode = null,
                city = null,
                country = null
            )
        }
        try {
            val coder = Geocoder(context)
            val address = coder.getFromLocation(latitude, longitude, 1)?.get(0)
            address?.let {
                return Address(
                    street = address.thoroughfare ?: stubStreet(latitude, longitude),
                    postalCode = address.postalCode,
                    city = address.locality,
                    country = address.countryName
                )
            }
        } catch (t: Throwable) {
            when (t) {
                is java.io.IOException -> Log.w(TAG, "Can't get the address", t)
                else -> crashReportsProvider.logException(t)
            }
        }
        return Address(
            street = stubStreet(latitude, longitude),
            postalCode = null,
            city = null,
            country = null
        )
    }

    fun getPlaceFromCoordinates(latitude: Double?, longitude: Double?): android.location.Address? {
        if (latitude == null || longitude == null) {
            return null
        }
        return try {
            Geocoder(context).let { it.getFromLocation(latitude, longitude, 1)?.get(0) }
        } catch (t: Throwable) {
            null
        }
    }

    fun getLocalDate(): LocalDate = LocalDate.now()

    fun getTimeZoneId(): ZoneId = ZoneId.systemDefault()

    private fun stubStreet(latitude: Double, longitude: Double) =
        context.getString(R.string.unknown_location_at) + "($latitude, $longitude)"

    fun getString(resId: Int): String = context.getString(resId)

    fun getClipboardContents(): String? {
        val manager =
            MyApplication.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        Log.e(TAG, manager.hasPrimaryClip().toString())
//        Log.e(TAG, manager.primaryClip?.getItemAt(0)?.text.toString())
//        Log.e(TAG, manager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN).toString())
        if (manager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true) {
            return manager.primaryClip?.getItemAt(0)?.text?.toString()
        } else {
            return null
        }
    }

    fun copyToClipboard(str: String) {
        val manager =
            MyApplication.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(str, str))
        MyApplication.context.let {
            Toast.makeText(it, it.getString(R.string.copied_to_clipboard), LENGTH_SHORT).show()
        }
    }

    fun stringFromResource(@StringRes res: Int): String {
        return MyApplication.context.getString(res)
    }

    @Throws(IOException::class)
    fun createTakePictureIntent(context: Context, file: File): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .also { takePictureIntent ->
                file.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        context,
                        "com.hypertrack.logistics.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }
            }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp = "${Date().time}"
        val storageDir: File = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        return bitmap.toBase64()
    }

    fun decodeBase64Bitmap(base64thumbnail: String): Bitmap {
        return base64thumbnail.decodeBase64Bitmap()
    }

    companion object {
        const val TAG = "OsUtilsProvider"
    }
}