package com.route4me.licenseplate.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.route4me.licenseplate.R
import kotlinx.android.synthetic.main.license_plate_layout.view.*

class LicensePlateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    init {
        LayoutInflater.from(context).inflate(R.layout.license_plate_layout, this, true)
    }

    fun bindData(number: String) {
        licenseNumber.text = number
    }
}