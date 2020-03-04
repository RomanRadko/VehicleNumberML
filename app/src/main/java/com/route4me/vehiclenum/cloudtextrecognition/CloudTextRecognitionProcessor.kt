package com.route4me.vehiclenum.cloudtextrecognition

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.route4me.vehiclenum.VisionProcessorBase
import com.route4me.vehiclenum.common.FrameMetadata
import com.route4me.vehiclenum.common.GraphicOverlay
import java.util.*

/**
 * Processor for the cloud text detector demo.
 */
class CloudTextRecognitionProcessor : VisionProcessorBase<FirebaseVisionText>() {

    private val detector: FirebaseVisionTextRecognizer = FirebaseVision.getInstance().cloudTextRecognizer

    override fun detectInImage(image: FirebaseVisionImage): Task<FirebaseVisionText> {
        return detector.processImage(image)
    }

    override fun onSuccess(
        originalCameraImage: Bitmap?,
        results: FirebaseVisionText,
        frameMetadata: FrameMetadata?,
        graphicOverlay: GraphicOverlay
    ) {
        graphicOverlay.clear()
        if (results == null) {
            return // TODO: investigate why this is needed
        }
        val blocks = results.textBlocks
        val sortedBlocks = blocks.sortedBy { -it.boundingBox!!.height() }
        if (sortedBlocks.isNotEmpty()) {
            val trimmedBlocks = mutableListOf<FirebaseVisionText.TextBlock>(sortedBlocks[0])
            for (i in trimmedBlocks.indices) {
                val lines = trimmedBlocks[i].lines
                for (j in lines.indices) {
                    val elements = lines[j].elements
                    for (l in elements.indices) {
                        val cloudTextGraphic = CloudTextGraphic(
                            graphicOverlay,
                            elements[l]
                        )
                        graphicOverlay.add(cloudTextGraphic)
                        var number = ""
                        for (element in elements) {
                            number += element.text
                        }
                        Toast.makeText(cloudTextGraphic.applicationContext,
                            "RESULT :: $number", Toast.LENGTH_LONG
                        ).show()
                        graphicOverlay.postInvalidate()
                    }
                }
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Cloud Text detection failed.$e")
    }

    companion object {

        private const val TAG = "CloudTextRecProc"
    }
}
