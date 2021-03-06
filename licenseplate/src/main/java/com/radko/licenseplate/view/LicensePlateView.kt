package com.radko.licenseplate.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.radko.licenseplate.R
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
        if (number.isNotEmpty()) {
            resultsLayout.visibility = View.VISIBLE
            noResultsLayout.visibility = View.GONE
            licenseNumber.text = number
        } else {
            resultsLayout.visibility = View.GONE
            noResultsLayout.visibility = View.VISIBLE
        }
    }
}