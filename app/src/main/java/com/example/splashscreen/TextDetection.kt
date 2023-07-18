package com.example.splashscreen

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.text.TextRecognizer
import android.view.SurfaceView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import kotlin.arrayOf


class TextDetection : AppCompatActivity() {
    private val MY_PERMISSION_REQUEST_READ_CONTACTS: Int = 101
    private lateinit var mCameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer
    private val tag: String? = "TextDetection"
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_detection)

        requestForPermission()

        val surface_camera_preview: SurfaceView = findViewById(R.id.surface_camera_preview)
        val tv_result : TextView = findViewById(R.id.tv_result)

        textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational) {
            Toast.makeText(this,"Dependencies are not loaded yet...please try after few moment!!", Toast.LENGTH_SHORT)
                .show()
            Log.e(tag, "Dependencies are downloading....try after few moment")
            return
        }
        // Init camera source to use high resolution and auto focus
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

                if (items.size() <= 0) {
                    return
                }

                tv_result.post {
                    val stringBuilder = StringBuilder()
                    for (i in 0 until items.size()) {
                        val item = items.valueAt(i)
                        stringBuilder.append(item.value)
                        stringBuilder.append("\n")
                    }
                    tv_result.text = stringBuilder.toString()
                }
            }
        })

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
}