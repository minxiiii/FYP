package com.example.splashscreen

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.splashscreen.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.lang.Math.abs
import java.util.Locale

class ObjectDetection : AppCompatActivity(), GestureDetector.OnGestureListener {

    lateinit var labels:List<String>
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap:Bitmap
    lateinit var imageView: ImageView // to process frames
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView //camera
    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var tts: TextToSpeech
    private var isDetectionComplete = false
    private lateinit var gestureDetector: GestureDetector
    private var previousPopup: View? = null
    var y1: Float = 0.0f
    var y2: Float = 0.0f
    companion object{
        const val MIN_DIST = 50
    }
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar()?.hide();
        // this references maindetection.xml
        setContentView(R.layout.maindetection)
        get_permission()
        gestureDetector = GestureDetector(this, this)
        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(this)
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageViewObj)

        textureView = findViewById(R.id.textureViewObj)
        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }


            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val classes = outputs.classesAsTensorBuffer.floatArray
                val maxScoreIndex = outputs.scoresAsTensorBuffer.floatArray.indices.maxByOrNull { outputs.scoresAsTensorBuffer.floatArray[it] } ?: -1

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                val objectName = findViewById<TextView>(R.id.objectNameObj)
                if (!isDetectionComplete) {
                    val index = maxScoreIndex //take the object that has the highest scoring detection
                    // Draw bounding boxes and labels
                    objectName.text = labels[classes[index].toInt()]
//                    objectName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
//                    objectName.setTextColor(Color.BLUE)
                    tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
                        if (status != TextToSpeech.ERROR) {
                            // Choose language
                            tts.language = Locale.ENGLISH
                            var label = labels.get(classes.get(index).toInt())
                            tts.speak(label ,TextToSpeech.QUEUE_ADD, null)
                            // NEED TO UPDATE HERE***
//                            showDialog()
                            showObjectText(objectName.text.toString().capitalizeFirstLetter());
                        }
                    })
                    isDetectionComplete = true

                }
                imageView.setImageBitmap(mutable)
                //storing captured frames
            }
        }

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

    }
    // ben
    fun showObjectText(text: String) {
        // If there is a previous popup, remove it from the layout
        if (previousPopup != null) {
            (previousPopup?.parent as? ViewGroup)?.removeView(previousPopup)
        }

        // Create a new popup view
        val inflater = LayoutInflater.from(this)
        val overlayView = inflater.inflate(R.layout.fixed_popup, null) as ConstraintLayout
        val tvDisplayText = overlayView.findViewById<TextView>(R.id.tvDisplayObject)

        // Set the text to the TextView in the overlay layout
        tvDisplayText.text = text

        // Add the overlayView to the Main Activity's layout
        addContentView(
            overlayView,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Store the reference to the newly displayed popup
        previousPopup = overlayView
    }
    // ben
    fun String.capitalizeFirstLetter(): String {
        return if (isNotEmpty()) {
            this[0].uppercase() + substring(1)
        } else {
            this
        }
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action){
            MotionEvent.ACTION_DOWN -> //action down
            {
                y1 = event.y
            }

            MotionEvent.ACTION_UP-> { // action up
                y2 = event.y
                val valueY: Float = y2 - y1

                if (abs(valueY) > MIN_DIST)
                {
                    if (y2 > y1)
                    {
                        navigateToTextDetectionActivity(this)

                    } else {
                        isDetectionComplete = false
                    }
                }

            }

        }

        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }

    //request for permission to start camera
    fun get_permission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission()
        }
    }

    override fun onDown(p0: MotionEvent): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
        //TODO("Not yet implemented")
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        // TODO("Not yet implemented")
        return false
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
        //TODO("Not yet implemented")
    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

//    fun openTextDect() {
//        val intent = Intent(this, TextDetection::class.java)
//        startActivity(intent)
//    }
    fun navigateToTextDetectionActivity(context: Context) {
        val intent = Intent(context, TextDetection::class.java)
        context.startActivity(intent)
    }
}