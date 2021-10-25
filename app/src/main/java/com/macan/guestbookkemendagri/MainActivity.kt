package com.macan.guestbookkemendagri

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.macan.guestbookkemendagri.captureframecomponent.*
import com.macan.guestbookkemendagri.helper.Helper
import com.macan.guestbookkemendagri.network.RetrofitClient
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.Runnable
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), ImageReader.OnImageAvailableListener {
    var timer: Timer? = null

    var scope = MainScope() // could also use an other scope such as viewModelScope if available
    var job: Job? = null

    var methodTitle: TextView? = null

    var previewHeight = 0
    var previewWidth = 0
    var sensorOrientation = 0

    //TODO getting frames of live camera footage and passing them to model
    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    private var rgbFrameBitmap: Bitmap? = null
    private var closeActivity = false
    private var recursiveRunning = false
    private var boundingBoxFrameLayout : BoundingBoxFrameLayout? = null
    var frameContainer : FrameLayout ? = null

    private var activity: Activity? = null

    var optionsFaceDetector: Any? = null
    var faceDetector = FaceDetection.getClient()

    var listMethod: ArrayList<Int> = ArrayList()
    var selectedImage: Bitmap? = null
    var selectedMethod: Int = 0
    var methodFinished: Boolean = false

    private var graphicOverlay: GraphicOverlay? = null


    private var mImageMaxWidth: Int? = null

    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null


    companion object{
        var LIVENESS_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activity = this

        methodTitle = findViewById(R.id.methodTitle)
        frameContainer = findViewById(R.id.container)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        boundingBoxFrameLayout = findViewById(R.id.boundingBoxFrame)

        optionsFaceDetector = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(optionsFaceDetector as FaceDetectorOptions)


        //TODO ask for permission of camera upon first launch of application
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
        ) {
            val permission = arrayOf(
                    Manifest.permission.CAMERA
            )
            requestPermissions(permission, 1122)
        } else {
            //TODO show live camera footage
            setFragment()
        }



        initiateMethod()
    }

    override fun onRestart() {
        super.onRestart()
        val intent = intent
        finish()
        startActivity(intent)
    }

    private fun initiateMethod(){
        recursiveRunning = false
        if(methodFinished){
            val intent = Intent(this@MainActivity, DestinationFormActivity::class.java)
            intent.putExtra("image", Helper.encodeImage(selectedImage!!))
            startActivity(intent)
        }else{
            //initiate liveness method here.
            if (listMethod.isEmpty()){
//                listMethod.add(Helper.METHOD_MOUTH)
//                listMethod.add(Helper.METHOD_EYES)
//                listMethod.add(Helper.METHOD_HEAD_RIGHT)
//                listMethod.add(Helper.METHOD_HEAD_LEFT)
//                listMethod.add(Helper.METHOD_EYE_LEFT)
//                listMethod.add(Helper.METHOD_EYE_RIGHT)

                listMethod.shuffle()
                listMethod.add(0, Helper.METHOD_FACE)
                return initiateMethod()
            }else{
                selectedMethod = listMethod[0]
                alertMethod(selectedMethod)
            }
        }
    }


    private fun alertMethod(method: Int){

        var msg = ""
        when(method) {
            Helper.METHOD_FACE -> {
                msg = "Mohon menunjukkan wajah di kamera."
            }
            Helper.METHOD_MOUTH -> {
                msg = "Mohon membuka mulut anda."
            }
            Helper.METHOD_EYES -> {
                msg = "Mohon memejamkan mata anda."
            }
            Helper.METHOD_HEAD_RIGHT -> {
                msg = "Mohon mengarahkan wajah ke kanan."
            }
            Helper.METHOD_HEAD_LEFT -> {
                msg = "Mohon mengarahkan wajah ke kiri."
            }
            Helper.METHOD_EYE_LEFT -> {
                msg = "Mohon pejamkan mata kiri anda."
            }
            Helper.METHOD_EYE_RIGHT -> {
                msg = "Mohon pejamkan mata kanan anda."
            }
        }

        methodTitle!!.text = msg

        val builder = AlertDialog.Builder(activity)
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which -> checkLiveness(method) }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()




    }

    private fun getImageMaxWidth(): Int? {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = frameContainer!!.width
        }
        return mImageMaxWidth
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxHeight(): Int? {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight = frameContainer!!.height
        }
        return mImageMaxHeight
    }

    // Gets the targeted width / height.
    private fun getTargetedWidthHeight(): Pair<Int?, Int?>? {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode = getImageMaxWidth()!!
        val maxHeightForPortraitMode = getImageMaxHeight()!!
        targetWidth = maxWidthForPortraitMode
        targetHeight = maxHeightForPortraitMode
        return Pair(targetWidth, targetHeight)
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap{
        val targetedSize = getTargetedWidthHeight()

        val targetWidth = targetedSize!!.first!!
        val maxHeight = targetedSize.second!!

        // Determine how much to scale down the image

        // Determine how much to scale down the image
        val scaleFactor = (bitmap.width.toFloat() / targetWidth.toFloat()).coerceAtLeast(bitmap.height.toFloat() / maxHeight.toFloat())

        val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width / scaleFactor).toInt(),
                (bitmap.height / scaleFactor).toInt(),
                true)

        return resizedBitmap
    }


    private fun checkLiveness(method: Int){
        recursiveRunning = true
        if(closeActivity){
            destroyActivity()
        }else{
            val encoded = Helper.encodeImage(rgbFrameBitmap!!)
            val paramObject = JsonObject()

            paramObject.addProperty("Image", encoded)

            when(method){
                Helper.METHOD_FACE -> {
                    Log.i("Tracking", "FACE")
                    val currMethod = Helper.METHOD_FACE
                    scope = MainScope()
                    scope.launch {
                        while (isActive) {
                            if (closeActivity) {
                                destroyActivity()
                            } else {
                                val rotatedBitmap = resizeBitmap(Helper.rotateBitmap(rgbFrameBitmap!!, -90F)!!)
                                val image = InputImage.fromBitmap(rotatedBitmap, 0) // wajib diputer dulu.

                                faceDetector.process(image)
                                        .addOnSuccessListener { faces ->
                                            graphicOverlay!!.clear()
                                            Log.i("faces count", faces.size.toString())
                                            if (faces.size == 1) {
                                                for (face in faces) {
                                                    val bounds = face.boundingBox
                                                    Log.i("bounds", "left = ${bounds.left}, top = ${bounds.top}, right = ${bounds.right}, bot = ${bounds.bottom}")

                                                    val faceGraphic = FaceContourGraphic(graphicOverlay)
                                                    graphicOverlay!!.add(faceGraphic)
                                                    faceGraphic.updateFace(face)

                                                    if (bounds.left > BoundingBoxFrameLayout.left && bounds.top > BoundingBoxFrameLayout.top && bounds.right < BoundingBoxFrameLayout.right && bounds.bottom < BoundingBoxFrameLayout.bottom && face.rightEyeOpenProbability != null && face.leftEyeOpenProbability != null) {
                                                        Log.i("capturedInFrame", "Yes")

                                                        if(face.rightEyeOpenProbability > 0.60 && face.leftEyeOpenProbability > 0.60){
                                                            Log.i("eyesOpen", "Yes")
                                                            val croppedBmp: Bitmap = Bitmap.createBitmap(rotatedBitmap, bounds.left, bounds.top, bounds.width(), bounds.height()) //crop the face
                                                            selectedImage = croppedBmp
                                                            methodFinished(currMethod)
                                                            postInferenceCallback!!.run()
                                                        }
//

                                                    }

                                                }
//
                                            }

                                        }
                                        .addOnFailureListener {
                                            // Task failed with an exception
                                            Log.e("errorFaceRecog", it.message.toString())
                                        }
                            }
                            delay(800)
                        }
                    }


                }
                Helper.METHOD_MOUTH -> {
                    Log.i("Tracking", "MOUTH")
                    val currMethod = Helper.METHOD_MOUTH
                    /*
                        {
                            Image: base64
                        }

                        {
                            result: True / False
                        }
                     */

                    RetrofitClient.instance.checkMouth(paramObject).enqueue(object :
                            Callback<ResponseBody> {
                        override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                        ) = if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                val data: String = result.getString("result")

                                Log.i("Result Mouth", data)
                                if (data == "True") {
                                    methodFinished(currMethod)
                                } else {
                                    checkLiveness(currMethod)
                                }
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        } else {
                            try {
                                val err = response.errorBody()?.string()
                                Log.i("ERROR RESPONSE", err.toString())
                                Helper.alertDialog(
                                        activity!!,
                                        err
                                )
                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i("ERROR5", t.message.toString())
                            checkLiveness(currMethod)
                        }

                    })
                }
                Helper.METHOD_EYES -> {
                    Log.i("Tracking", "EYES")
                    val currMethod = Helper.METHOD_EYES
                    RetrofitClient.instance.checkEyes(paramObject).enqueue(object :
                            Callback<ResponseBody> {
                        override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                        ) = if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                val data: String = result.getString("result")

                                Log.i("Result Eyes", data)
                                if (data == "True") {
                                    methodFinished(currMethod)
                                } else {
                                    checkLiveness(currMethod)
                                }
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        } else {
                            try {
                                val err = response.errorBody()?.string()
                                Log.i("ERROR RESPONSE", err.toString())
                                Helper.alertDialog(
                                        activity!!,
                                        err
                                )
                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i("ERROR5", t.message.toString())
                            checkLiveness(currMethod)
                        }

                    })
                }
                Helper.METHOD_HEAD_RIGHT -> {
                    Log.i("Tracking", "HEAD RIGHT")
                    val currMethod = Helper.METHOD_HEAD_RIGHT
                    RetrofitClient.instance.checkHeadRight(paramObject).enqueue(object :
                            Callback<ResponseBody> {
                        override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                        ) = if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                val data: String = result.getString("result")

                                Log.i("Result Head Right", data)
                                if (data == "True") {
                                    methodFinished(currMethod)
                                } else {
                                    checkLiveness(currMethod)
                                }
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        } else {
                            try {
                                val err = response.errorBody()?.string()
                                Log.i("ERROR RESPONSE", err.toString())
                                Helper.alertDialog(
                                        activity!!,
                                        err
                                )
                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i("ERROR5", t.message.toString())
                            checkLiveness(currMethod)
                        }

                    })
                }
                Helper.METHOD_HEAD_LEFT -> {
                    Log.i("Tracking", "HEAD LEFT")
                    val currMethod = Helper.METHOD_HEAD_LEFT
                    RetrofitClient.instance.checkHeadLeft(paramObject).enqueue(object :
                            Callback<ResponseBody> {
                        override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                        ) = if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                val data: String = result.getString("result")

                                Log.i("Result Head Left", data)
                                if (data == "True") {
                                    methodFinished(currMethod)
                                } else {
                                    checkLiveness(currMethod)
                                }
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        } else {
                            try {
                                val err = response.errorBody()?.string()
                                Log.i("ERROR RESPONSE", err.toString())
                                Helper.alertDialog(
                                        activity!!,
                                        err
                                )
                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i("ERROR5", t.message.toString())
                            checkLiveness(currMethod)
                        }

                    })
                }
                Helper.METHOD_EYE_LEFT -> {
                    Log.i("Tracking", "EYE LEFT")
                    val currMethod = Helper.METHOD_EYE_LEFT
                    RetrofitClient.instance.checkEyeLeft(paramObject).enqueue(object :
                            Callback<ResponseBody> {
                        override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                        ) = if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                val data: String = result.getString("result")

                                Log.i("Result Eye Left", data)
                                if (data == "True") {
                                    methodFinished(currMethod)
                                } else {
                                    checkLiveness(currMethod)
                                }
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        } else {
                            try {
                                val err = response.errorBody()?.string()
                                Log.i("ERROR RESPONSE", err.toString())
                                Helper.alertDialog(
                                        activity!!,
                                        err
                                )
                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i("ERROR5", t.message.toString())
                            checkLiveness(currMethod)
                        }

                    })
                }
                Helper.METHOD_EYE_RIGHT -> {
                    Log.i("Tracking", "EYE RIGHT")
                    val currMethod = Helper.METHOD_EYE_RIGHT
                    RetrofitClient.instance.checkEyeRight(paramObject).enqueue(object :
                            Callback<ResponseBody> {
                        override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                        ) = if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                val data: String = result.getString("result")

                                Log.i("Result Eye Right", data)
                                if (data == "True") {
                                    methodFinished(currMethod)
                                } else {
                                    checkLiveness(currMethod)
                                }
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        } else {
                            try {
                                val err = response.errorBody()?.string()
                                Log.i("ERROR RESPONSE", err.toString())
                                Helper.alertDialog(
                                        activity!!,
                                        err
                                )
                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                checkLiveness(currMethod)
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.i("ERROR5", t.message.toString())
                            checkLiveness(currMethod)
                        }

                    })
                }
            }
        }
    }

    fun methodFinished(currMethod: Int){
//        Helper.toastMessage(activity!!, "Berhasil!")
        scope.cancel()
        if(!methodFinished){
            if(listMethod.contains(currMethod)){
                listMethod.remove(currMethod)
                Log.i("ListMethodSize", listMethod.size.toString())
                if(listMethod.isEmpty()){
                    methodFinished = true
                }
                initiateMethod()
            }

        }
    }

    //TODO fragment which show llive footage from camera
    protected fun setFragment() {
        val manager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        try {
            cameraId = manager.cameraIdList[1]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        val fragment: Fragment
        val camera2Fragment = CameraConnectionFragment.newInstance(
                object :
                        CameraConnectionFragment.ConnectionCallback {
                    override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
                        previewHeight = size!!.height
                        previewWidth = size.width
                        sensorOrientation = rotation - getScreenOrientation()
                    }
                },
                this,
                R.layout.camera_fragment,
                Size(640, 480)
        )
        camera2Fragment.setCamera(cameraId)
        fragment = camera2Fragment
        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()

        setBoundinxBoxFrame()
    }

    fun setBoundinxBoxFrame(){
        val handler = Handler()
        handler.postDelayed({
            boundingBoxFrameLayout = findViewById(R.id.boundingBoxFrame)
            val boundingBoxLayoutParams = boundingBoxFrameLayout!!.layoutParams
            boundingBoxLayoutParams.width = frameContainer!!.width
            boundingBoxLayoutParams.height = frameContainer!!.height
            boundingBoxFrameLayout!!.layoutParams = boundingBoxLayoutParams
        }, 1000)
    }

    protected fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader.acquireLatestImage() ?: return
            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                ImageUtils.convertYUV420ToARGB8888(
                        yuvBytes[0]!!,
                        yuvBytes[1]!!,
                        yuvBytes[2]!!,
                        previewWidth,
                        previewHeight,
                        yRowStride,
                        uvRowStride,
                        uvPixelStride,
                        rgbBytes!!
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }
            processImage()
        } catch (e: Exception) {
            return
        }
    }


    private fun processImage() {
        imageConverter!!.run()
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        rgbFrameBitmap?.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight)
        postInferenceCallback!!.run()
    }

    protected fun fillBytes(
            planes: Array<Image.Plane>,
            yuvBytes: Array<ByteArray?>
    ) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }

    //TODO rotate image if image captured on sumsong devices
    //Most phone cameras are landscape, meaning if you take the photo in portrait, the resulting photos will be rotated 90 degrees.
    fun rotateBitmap(input: Bitmap): Bitmap? {
        Log.d("trySensor", sensorOrientation.toString() + "     " + getScreenOrientation())
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(sensorOrientation.toFloat())
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //TODO show live camera footage
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //TODO show live camera footage
            setFragment()
        } else {
            finish()
        }
    }


    private fun destroyActivity(){
        if(selectedMethod == Helper.METHOD_FACE){
            scope.cancel()
        }
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        closeActivity = true //di set true, lalu ketika
        if(!recursiveRunning){
            finish()
        }
    }
}