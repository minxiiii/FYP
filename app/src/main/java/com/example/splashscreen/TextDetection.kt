package com.example.splashscreen

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.widget.Toast
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.text.TextRecognizer
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import java.util.Locale
import kotlin.arrayOf


class TextDetection : AppCompatActivity() {
    private val MY_PERMISSION_REQUEST_READ_CONTACTS: Int = 101
    private lateinit var mCameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer
    // Initialise gesturedetector
    private lateinit var gestureDetector: GestureDetector
    private val tag: String? = "TextDetection"
    private var previousPopup: View? = null
    var currentGesture = ""
    private var isTextDetected=true
    lateinit var tts: TextToSpeech
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_detection)
        // initialize the GestureDetector
        gestureDetector = GestureDetector(this, MyGestureListener())
        requestForPermission()

        val surface_camera_preview: SurfaceView = findViewById(R.id.surface_camera_preview)
        val tv_result : TextView = findViewById(R.id.tv_result)

        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = Locale.ENGLISH
                tts.setSpeechRate(1.5f)
                tts.speak("Text detection mode, swipe down to scan",TextToSpeech.QUEUE_FLUSH, null);
            }
        })

        textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational) {
            Toast.makeText(this,"Dependencies are not loaded yet...please try after few moments!!", Toast.LENGTH_SHORT)
                .show()
            Log.e(tag, "Dependencies are downloading....try after few moments")
            return
        }
        // init camera source to use high resolution and auto focus
        mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(1280, 1024)
            .setAutoFocusEnabled(true)
            .setRequestedFps(2.0f)
            .build()

        surface_camera_preview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(p0: SurfaceHolder) {
                mCameraSource.stop()
            }
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    if (isCameraPermissionGranted()) {
                        mCameraSource.start(surface_camera_preview.holder)
                    } else {
                        requestForPermission()
                    }
                } catch (e: Exception) {
                    toast("Error: "  + e.message)
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            }
        }
        )
        textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                val items = detections.detectedItems

                if (items.size() <= 0 || isTextDetected) {
                    return
                }
                tv_result.post {
                    val stringBuilder = StringBuilder()
                    for (i in 0 until items.size()) {
                        val item = items.valueAt(i)
                        stringBuilder.append(item.value)
                        stringBuilder.append("\n")
                    }
                    // this is the part that prints the text
                    tv_result.text = stringBuilder.toString()
                    tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
                        if (status != TextToSpeech.ERROR) {
                            tts.language = Locale.ENGLISH
                            tts.speak(tv_result.text.toString(),TextToSpeech.QUEUE_FLUSH, null);
                        }
                    })
                    showObjectText(tv_result.text.toString().capitalizeFirstLetter())
                    isTextDetected=true
                }
            }
        })

    }
    private fun gestureDetected(gesture: Char) {
        // This method will be called when a swipe gesture is detected
        when (gesture) {
            // set boolean flag to false so that text detection can run once
            'u' -> {navigateToObjectDetectionActivity(this)
                if (::tts.isInitialized) {
                    tts.stop()
                    tts.setSpeechRate(1.5f)
                }
            }
            'd' -> {
                isTextDetected = false
                if (::tts.isInitialized) {
                    tts.stop()
                    tts.setSpeechRate(1.5f)
                }
            }
            'l' -> showToast("left")
            'r' -> callHelp(this)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    fun showObjectText(text: String) {
        // if there is a previous popup, remove it from the layout
        if (previousPopup != null) {
            (previousPopup?.parent as? ViewGroup)?.removeView(previousPopup)
        }
        val inflater = LayoutInflater.from(this)
        val overlayView = inflater.inflate(R.layout.fixed_popup_text, null) as ConstraintLayout
        val tvDisplayText = overlayView.findViewById<TextView>(R.id.tvDisplayText)

        // set the text to the TextView in the overlay layout
        tvDisplayText.text = text

        // add the overlayView to the Main Activity's layout
        addContentView(
            overlayView,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        )
        // store the reference to the newly displayed popup
        previousPopup = overlayView
    }
    fun String.capitalizeFirstLetter(): String {
        return if (isNotEmpty()) {
            this[0].uppercase() + substring(1)
        } else {
            this
        }
    }

    private fun requestForPermission() {
        if (ContextCompat.checkSelfPermission(
                this@TextDetection,
                Manifest.permission.CAMERA
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@TextDetection,
                    Manifest.permission.CAMERA
                )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this@TextDetection,
                    arrayOf(Manifest.permission.CAMERA),
                    MY_PERMISSION_REQUEST_READ_CONTACTS
                )

            }
        } else {

        }
    }


    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this@TextDetection, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    //method for toast
    fun toast(text : String) {
        Toast.makeText(this@TextDetection, text, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_READ_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    requestForPermission()
                }
                return
            }
        }
    }
    // GESTURE DETECTOR FUNCTIONS DON'T REMOVE THIS PLS :)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            val horizontalSwipe = Math.abs(diffX) > Math.abs(diffY)
            if (horizontalSwipe) {
                if (diffX > 0) {
                    // right swipe
                    gestureDetected('r')
                } else {
                    // left swipe
                    gestureDetected('l')
                }
            } else {
                if (diffY > 0) {
                    // down swipe
                    gestureDetected('d')
                } else {
                    // up swipe
                    gestureDetected('u')
                }
            }

            return true
        }
    }
    fun navigateToObjectDetectionActivity(context: Context) {
        val intent = Intent(context, ObjectDetection::class.java)
        context.startActivity(intent)
    }
    fun callHelp(context: Context) {
        // create an intent to dial the number
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:122")

        // check if there's an app to handle the intent (dialer app)
        if (intent.resolveActivity(context.packageManager) != null) {
            // start the dialer activity
            context.startActivity(intent)
        } else {
        }
    }


}