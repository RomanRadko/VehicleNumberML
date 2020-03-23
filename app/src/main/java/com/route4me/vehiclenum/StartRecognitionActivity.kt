package com.route4me.vehiclenum

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.route4me.licenseplate.StillImageActivity
import com.route4me.licenseplate.StillImageActivity.Companion.SHOULD_DRAW_OVERLAY
import kotlinx.android.synthetic.main.start_recognition_activity.*
import java.util.*

class StartRecognitionActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContentView(R.layout.start_recognition_activity)

        startRecognitionBtn.setOnClickListener {
            val intent = Intent(this, StillImageActivity::class.java)
            intent.putExtra(SHOULD_DRAW_OVERLAY, true)
            startActivity(intent)
        }

        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
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
                if (!isPermissionGranted(this, it)) {
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
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    companion object {
        private const val TAG = "StartRecognitionAct"
        private const val PERMISSION_REQUESTS = 1
    }
}
