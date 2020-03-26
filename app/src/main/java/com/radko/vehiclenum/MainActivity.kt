package com.radko.vehiclenum

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.radko.licenseplate.GetImageFragment
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        startRecognitionBtn.setOnClickListener {
            startRecognition()
        }
    }

    override fun onResume() {
        super.onResume()
        if (intent.extras != null && intent.extras!!.getString(GetImageFragment.CAR_PLATE_NUMBER)!!
                .isNotEmpty()
        ) {
            //todo: here is should be logic for number listed in base checking
            recognizedNumberLbl.visibility = View.VISIBLE
            carNumber.text = intent.extras!!.getString(GetImageFragment.CAR_PLATE_NUMBER)
        }
    }

    private fun startRecognition() {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        val printFragment: Fragment = GetImageFragment.newInstance()
        printFragment.arguments = Bundle().apply {
            putBoolean(GetImageFragment.SHOULD_DRAW_OVERLAY, true)
        }
        transaction.replace(R.id.container, printFragment)
        transaction.commit()
    }
}
