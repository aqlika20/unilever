package com.macan.guestbookkemendagri.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.macan.guestbookkemendagri.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat


class Helper {

    companion object{
        val BASE_URL = "http://192.168.1.237:8000"

        val CAMERA_PERMISSION_CODE = 1000
        val CAMERA_CAPTURE_CODE = 1001

        val METHOD_FACE = 0 //for capturing the face


        fun toastMessage(context: Context, msg: String){
            return Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

//        fun successtoastMessage(context: Context, msg: String){
//            val inflater: LayoutInflater =
//            val layout: View = inflater.inflate(
//                R.layout.custom_toas_success,
//                findViewById(R.id.toast_layout_root) as ViewGroup?
//            )
//
//            val text = layout.findViewById<View>(R.id.text) as TextView
//            text.text = "This is a custom toast"
//
//            val toast = Toast(ApplicationProvider.getApplicationContext<Context>())
//            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
//            toast.duration = Toast.LENGTH_LONG
//            toast.setView(layout)
//            toast.show()
//        }

        fun failedtoastMessage(context: Context, msg: String){
            return Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        fun alertDialog(context: Context?, msg: String?) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(msg)
            builder.setCancelable(false)
            builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }

        fun loadingDialog(context: Context, msg: String = "Loading..."): AlertDialog {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(msg)
            val dialog = builder.create()
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            return dialog
        }

        fun convertDateTimeToReadable(dateTimePhp: String, dateFormat: Int): String {
            var format = ""
            when(dateFormat){
                1 -> {
                    format = "MM/dd/yyyy hh:mm:ss a"
                }
                2 -> {
                    format = "yyyy-MM-dd"
                }
                3 -> {
                    format = "yyyy-MM-dd hh:mm:ss"
                }
            }


            try {
                val simpleDateFormat = SimpleDateFormat(format)
                val date = simpleDateFormat.parse(dateTimePhp)
                val convetDateFormat = SimpleDateFormat("dd MMMM yyyy")
                return convetDateFormat.format(date)
            } catch (e: Exception) {
                return e.printStackTrace().toString()
            }
        }

        fun encodeImage(bm: Bitmap):String {
            val baos = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.JPEG, 30, baos)
            val b = baos.toByteArray()
            val encImage = Base64.encodeToString(b, Base64.NO_WRAP)
            return encImage
        }

        fun decodeImage(base64Image: String): Bitmap{
            val decodedString = Base64.decode(base64Image, 0)

            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }


        fun rotateBitmap(source: Bitmap, angle: Float, mirror: Boolean = false): Bitmap? {
            val matrix = Matrix()
            matrix.postRotate(angle)
            if(mirror){
                matrix.postScale(-1f, 1f)
            }
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
        //barcode generator
        @SuppressLint("SetTextI18n")
        fun generateQRCode(context: Context, text: String): Bitmap {
            val width = 200
            val height = 200
            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val codeWriter = MultiFormatWriter()
            try {
                val bitMatrix = codeWriter.encode(
                    text,
                    BarcodeFormat.DATA_MATRIX,
                    width,
                    height
                )

                val barcodeEncoder = BarcodeEncoder()
                bitmap = barcodeEncoder.createBitmap(bitMatrix)


            } catch (e: WriterException) {

                Log.i("error", "generateQRCode: ${e.message}")

            }
//        return addWhiteBorder(bitmap, 10)!!
//            return addOverlayToCenter(context, bitmap)
            return bitmap
        }

        fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
            var drawable: Drawable = ContextCompat.getDrawable(context, drawableId)!!
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = DrawableCompat.wrap(drawable).mutate()
            }
            val bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
            var width = image.width
            var height = image.height
            val bitmapRatio = width.toFloat() / height.toFloat()
            if (bitmapRatio > 1) {
                width = maxSize
                height = (width / bitmapRatio).toInt()
            } else {
                height = maxSize
                width = (height * bitmapRatio).toInt()
            }
            return Bitmap.createScaledBitmap(image, width, height, true)
        }

//        fun addOverlayToCenter(context: Context, currentBitmap: Bitmap): Bitmap {
//
//            val overlayBitmap: Bitmap = getResizedBitmap(
//                BitmapFactory.decodeResource(
//                    context.resources,
//                    R.drawable.kemkes_medium
//                )!!, 100
//            )
////            val overlayBitmap: Bitmap = getBitmapFromVectorDrawable(
////                context,
////                R.drawable.directoratsvg
////            )!!
//            val bitmap2Width = overlayBitmap.width
//            val bitmap2Height = overlayBitmap.height
//
//            val newWidth = 0
//            val newHeight = 0
////            // calculate the scale - in this case = 0.4f
////            val scaleWidth: Float = newWidth.toFloat() / bitmap2Width
////            val scaleHeight: Float = newHeight.toFloat() / bitmap2Height
//
//            val marginLeft = (currentBitmap.width * 0.5 - bitmap2Width * 0.5).toFloat()
//            val marginTop = (currentBitmap.height * 0.5 - bitmap2Height * 0.5).toFloat()
//            val canvas = Canvas(currentBitmap)
//            canvas.drawBitmap(currentBitmap, Matrix(), null)
////            canvas.scale(newWidth.toFloat(), newHeight.toFloat())
//            canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null)
//            return addWhiteBorder(currentBitmap, 10)!!
//        }

        private fun addWhiteBorder(bmp: Bitmap, borderSize: Int): Bitmap? {
            val bmpWithBorder =
                Bitmap.createBitmap(
                    bmp.width + borderSize * 2,
                    bmp.height + borderSize * 2,
                    bmp.config
                )
            val canvas = Canvas(bmpWithBorder)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bmp, borderSize.toFloat(), borderSize.toFloat(), null)
            return bmpWithBorder
        }

        //keyboard hider
        fun hideKeyboard(context: Context, view: View) {
            val inputMethodManager: InputMethodManager? =
                context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager!!.hideSoftInputFromWindow(view.getWindowToken(), 0)
        }


        fun viewToBitmap(view: View): Bitmap? {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        } //fungsi ini bisa convert layout (LinearLayout, Layout apapun lah) jadi image, jadi bisa save sertifikat (jika diperlukan)

        fun saveImage(context: Context, contentResolver: ContentResolver, qrBitmap: Bitmap) {
            //Generating a file name
            val filename = "${System.currentTimeMillis()}.png"

            //Output stream
            var fos: OutputStream? = null

            //For devices running android >= Q
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //getting the contentResolver
                contentResolver.also { resolver ->

                    //Content resolver will process the contentvalues
                    val contentValues = ContentValues().apply {

                        //putting file information in content values
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    //Inserting the contentValues to contentResolver and getting the Uri
                    val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    //Opening an outputstream with the Uri that we got
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                //These for devices running on android < Q
                //So I don't think an explanation is needed here
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            fos?.use {
                //Finally writing the bitmap to the output stream that we opened
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }



    }

}