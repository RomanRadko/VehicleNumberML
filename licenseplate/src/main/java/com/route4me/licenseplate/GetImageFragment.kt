package com.route4me.licenseplate

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.annotation.KeepName
import com.route4me.licenseplate.common.VisionImageProcessor
import com.route4me.licenseplate.preferences.RecognitionPreferences
import com.route4me.licenseplate.textrecognition.TextRecognitionProcessor
import kotlinx.android.synthetic.main.get_image_layout.*
import kotlinx.android.synthetic.main.license_plate_layout.*
import kotlinx.android.synthetic.main.license_plate_layout.view.*
import java.io.IOException
import java.util.*
import kotlin.math.max

/** Fragment demonstrating different image detector features with a still image from camera.  */
@KeepName
class GetImageFragment : Fragment() {

    companion object {

        private const val TAG = "GetImageFragment"
        const val SHOULD_DRAW_OVERLAY = "SHOULD_DRAW_OVERLAY"
        const val CAR_PLATE_NUMBER = "CAR_PLATE_NUMBER"
        private const val SIZE_PREVIEW = "w:max" // Available on-screen width.
        private const val SIZE_1024_768 = "w:1024" // ~1024*768 in a normal ratio
        private const val SIZE_640_480 = "w:640" // ~640*480 in a normal ratio

        private const val KEY_IMAGE_URI = "KEY_IMAGE_URI"
        private const val KEY_IMAGE_MAX_WIDTH = "KEY_IMAGE_MAX_WIDTH"
        private const val KEY_IMAGE_MAX_HEIGHT = "KEY_IMAGE_MAX_HEIGHT"
        private const val KEY_SELECTED_SIZE = "KEY_SELECTED_SIZE"

        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
        private const val PERMISSION_REQUESTS = 1

        fun newInstance(): GetImageFragment {
            return GetImageFragment()
        }
    }

    private var selectedSize: String = SIZE_PREVIEW
    private var isLandScape: Boolean = false
    private var imageUri: Uri? = null

    // Max width (portrait mode)
    private var imageMaxWidth = 0

    // Max height (portrait mode)
    private var imageMaxHeight = 0
    private var imageProcessor: VisionImageProcessor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecognitionPreferences(context!!).showTextOverlay = arguments!!.getBoolean(
            SHOULD_DRAW_OVERLAY
        )
        if (previewPane == null) {
            Log.d(TAG, "Preview is null")
        }
        if (previewOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        createImageProcessor()

        isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        savedInstanceState?.let {
            imageUri = it.getParcelable(KEY_IMAGE_URI)
            imageMaxWidth = it.getInt(KEY_IMAGE_MAX_WIDTH)
            imageMaxHeight = it.getInt(KEY_IMAGE_MAX_HEIGHT)
            selectedSize = it.getString(KEY_SELECTED_SIZE, "")

            imageUri?.let { _ ->
                tryReloadAndDetectInImage()
            }
        }
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        } else {
            startCameraIntentForResult()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.get_image_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        licensePlateView.yesBtn.setOnClickListener {
            val launchIntent: Intent =
                activity!!.packageManager.getLaunchIntentForPackage("com.route4me.vehiclenum")!!
            launchIntent.putExtra(CAR_PLATE_NUMBER, licenseNumber.text)
            startActivity(launchIntent)
        }
        licensePlateView.noBtn.setOnClickListener {
            startCameraIntentForResult()
        }
        licensePlateView.recaptureBtn.setOnClickListener {
            startCameraIntentForResult()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        createImageProcessor()
        tryReloadAndDetectInImage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        with(outState) {
            putParcelable(KEY_IMAGE_URI, imageUri)
            putInt(KEY_IMAGE_MAX_WIDTH, imageMaxWidth)
            putInt(KEY_IMAGE_MAX_HEIGHT, imageMaxHeight)
            putString(KEY_SELECTED_SIZE, selectedSize)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (allPermissionsGranted()) {
            startCameraIntentForResult()
        }
    }

    private fun startCameraIntentForResult() {
        // Clean up last time's image
        imageUri = null
        previewPane?.setImageBitmap(null)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(activity!!.packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri = activity!!.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) {
                return
            }

            // Clear the overlay first
            previewOverlay?.clear()

            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(activity!!.contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(activity!!.contentResolver, imageUri!!)
                ImageDecoder.decodeBitmap(source)
            }

            // Get the dimensions of the View
            val targetedSize = getTargetedWidthHeight()

            val targetWidth = targetedSize.first
            val maxHeight = targetedSize.second

            // Determine how much to scale down the image
            val scaleFactor = max(
                imageBitmap.width.toFloat() / targetWidth.toFloat(),
                imageBitmap.height.toFloat() / maxHeight.toFloat()
            )

            val resizedBitmap = Bitmap.createScaledBitmap(
                imageBitmap,
                (imageBitmap.width / scaleFactor).toInt(),
                (imageBitmap.height / scaleFactor).toInt(),
                true
            )

            previewPane?.setImageBitmap(resizedBitmap)
            resizedBitmap?.let {
                imageProcessor?.process(it, previewOverlay)
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Error retrieving saved image", exception)
        }
    }

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxWidth(): Int {
        if (imageMaxWidth == 0) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            imageMaxWidth = if (isLandScape) {
                (previewPane.parent as View).height - licensePlateView.height / 2
            } else {
                (previewPane.parent as View).width
            }
        }

        return imageMaxWidth
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxHeight(): Int {
        if (imageMaxHeight == 0) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            imageMaxHeight = if (isLandScape) {
                (previewPane.parent as View).width
            } else {
                (previewPane.parent as View).height - licensePlateView.height / 2
            }
        }

        return imageMaxHeight
    }

    // Gets the targeted width / height.
    private fun getTargetedWidthHeight(): Pair<Int, Int> {
        val targetWidth: Int
        val targetHeight: Int

        when (selectedSize) {
            SIZE_PREVIEW -> {
                val maxWidthForPortraitMode = getImageMaxWidth()
                val maxHeightForPortraitMode = getImageMaxHeight()
                targetWidth = if (isLandScape) maxHeightForPortraitMode else maxWidthForPortraitMode
                targetHeight =
                    if (isLandScape) maxWidthForPortraitMode else maxHeightForPortraitMode
            }
            SIZE_640_480 -> {
                targetWidth = if (isLandScape) 640 else 480
                targetHeight = if (isLandScape) 480 else 640
            }
            SIZE_1024_768 -> {
                targetWidth = if (isLandScape) 1024 else 768
                targetHeight = if (isLandScape) 768 else 1024
            }
            else -> throw IllegalStateException("Unknown size")
        }

        return Pair(targetWidth, targetHeight)
    }

    private fun createImageProcessor() {
        imageProcessor = TextRecognitionProcessor(context!!) {
            licensePlateView.visibility = View.VISIBLE
            licensePlateView.bindData(it!!)
        }
    }

    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = activity!!.packageManager
                .getPackageInfo(activity!!.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(context!!, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(context!!, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            requestPermissions(allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

}
