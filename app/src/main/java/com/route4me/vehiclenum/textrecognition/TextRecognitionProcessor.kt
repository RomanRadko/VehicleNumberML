package com.route4me.vehiclenum.textrecognition

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.route4me.vehiclenum.VisionProcessorBase
import com.route4me.vehiclenum.common.CameraImageGraphic
import com.route4me.vehiclenum.common.FrameMetadata
import com.route4me.vehiclenum.common.GraphicOverlay
import java.io.IOException

/** Processor for the text recognition demo.  */
class TextRecognitionProcessor : VisionProcessorBase<FirebaseVisionText>() {

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
        originalCameraImage.let { image ->
            val imageGraphic = CameraImageGraphic(graphicOverlay, image)
            graphicOverlay.add(imageGraphic)
        }
        val blocks = results.textBlocks
        val sortedBlocks = blocks.sortedBy { -it.boundingBox!!.height() }
        val trimmedBlocks = mutableListOf<FirebaseVisionText.TextBlock>(sortedBlocks[0])
        for (i in trimmedBlocks.indices) {
            val lines = trimmedBlocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic = TextGraphic(graphicOverlay, elements[k])
                    graphicOverlay.add(textGraphic)
                }
            }
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
