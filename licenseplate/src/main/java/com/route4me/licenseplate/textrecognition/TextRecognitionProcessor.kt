package com.route4me.licenseplate.textrecognition

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.route4me.licenseplate.VisionProcessorBase
import com.route4me.licenseplate.common.FrameMetadata
import com.route4me.licenseplate.common.GraphicOverlay
import com.route4me.licenseplate.preferences.RecognitionPreferences
import java.io.IOException

/** Processor for the text recognition demo.  */
class TextRecognitionProcessor(private val context: Context, private val callback : (result: String?) -> Unit) : VisionProcessorBase<FirebaseVisionText>() {

    private val detector: FirebaseVisionTextRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: $e")
        }
    }

    override fun detectInImage(image: FirebaseVisionImage): Task<FirebaseVisionText> {
        Log.d(TAG, "text: " + detector.processImage(image))
        return detector.processImage(image)
    }

    override fun onSuccess(
        originalCameraImage: Bitmap?,
        results: FirebaseVisionText,
        frameMetadata: FrameMetadata?,
        graphicOverlay: GraphicOverlay
    ) {
        graphicOverlay.clear()
        val blocks = results.textBlocks
        val sortedBlocks = blocks.sortedBy { -it.boundingBox!!.height() }
        if (sortedBlocks.isNotEmpty()) {
            val trimmedBlocks = mutableListOf<FirebaseVisionText.TextBlock>(sortedBlocks[0])
            for (i in trimmedBlocks.indices) {
                val lines = trimmedBlocks[i].lines
                for (j in lines.indices) {
                    val elements = lines[j].elements
                    for (k in elements.indices) {
                        val textGraphic = TextGraphic(graphicOverlay, elements[k])
                        var number = ""
                        for (element in elements) {
                            number += element.text
                        }
                        callback.invoke(number)
                        if (RecognitionPreferences(context).showTextOverlay!!) {
                            graphicOverlay.add(textGraphic)
                        }
                    }
                }
            }
        } else {
            callback.invoke("")
        }
        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Text detection failed.$e")
    }

    companion object {

        private const val TAG = "TextRecProc"
    }
}
