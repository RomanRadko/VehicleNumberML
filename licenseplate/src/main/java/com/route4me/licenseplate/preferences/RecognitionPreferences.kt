package com.route4me.licenseplate.preferences

import android.content.Context

const val NAME: String = "RecognitionPreferences"

class RecognitionPreferences(context: Context) : Preferences(context, NAME) {
    var showTextOverlay by booleanPref()
}