package com.macan.guestbookkemendagri

import android.Manifest
import android.R.attr
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.basgeekball.awesomevalidation.AwesomeValidation
import com.basgeekball.awesomevalidation.ValidationStyle
import com.macan.guestbookkemendagri.helper.Helper
import com.macan.guestbookkemendagri.helper.MyApp.Companion.getContext
import com.macan.guestbookkemendagri.network.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class RegisterActivity : AppCompatActivity() {
    private var nama: EditText? = null
    private var unique_identity_number: EditText? = null
    private var photo: ImageView? = null
    private var btnBack: ImageView? = null

    private var btnNext: Button? = null
    private var validator: AwesomeValidation? = null
    private var registerGoToFaceRecog: ImageView? = null

    private var type: String = ""
    private var arrayIndentity: String =""
    private var arrayRole: String =""

    var image_uri: Uri? = null

    private var encodedImage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        validator = AwesomeValidation(ValidationStyle.BASIC)

        nama = findViewById(R.id.nama)
        unique_identity_number = findViewById(R.id.unique_identity_number)
        photo = findViewById(R.id.photo)
        registerGoToFaceRecog = findViewById(R.id.registerGoToFaceRecog)
        btnNext = findViewById(R.id.btn_next)
        btnBack = findViewById(R.id.back)

        // Dropdown Button Identity Type Id
        val type_identity = resources.getStringArray(R.array.type_identity)
        val identity_type_id = findViewById<Spinner>(R.id.identity_type_id)

        if (identity_type_id != null) {
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, type_identity)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            identity_type_id.adapter = adapter

            identity_type_id.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {

                    type = type_identity[position]
                    dataIdentityType(type)

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }

        // Dropdown Button Identity Type Id
        val list_role = resources.getStringArray(R.array.role)
        val role = findViewById<Spinner>(R.id.role)

        if (role != null) {
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, list_role)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            role.adapter = adapter

            role.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {

                    type = list_role[position]
                    dataRole(type)

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }

        nama!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                Helper.hideKeyboard(v.context, v)
            }
        }

        unique_identity_number!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                Helper.hideKeyboard(v.context, v)
            }
        }

        role!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                Helper.hideKeyboard(v.context, v)
            }
        }

        btnBack!!.setOnClickListener{
            backToLobby()
        }

        registerGoToFaceRecog!!.setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_DENIED ) {
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestPermissions(permission, Helper.CAMERA_PERMISSION_CODE)
                }
                else {
                    openCamera()
                }
            }
            else {
                openCamera()
            }
        }

        photo!!.setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_DENIED ) {
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestPermissions(permission, Helper.CAMERA_PERMISSION_CODE)
                }
                else {
                    openCamera()
                }
            }
            else {
                openCamera()
            }
        }

        btnNext!!.setOnClickListener{
            if(validator!!.validate()){
                submitForm()
            }
        }
    }

    private fun dataIdentityType(list: String){
        if (list == "Choose..."){
            arrayIndentity = 0.toString()
        }
        else if (list == "Nomor Induk Pegawai") {
            arrayIndentity = 1.toString()
        }
        else if (list == "KTP"){
            arrayIndentity = 2.toString()
        }
    }

    private fun dataRole(list: String){
        if (list == "Choose..."){
            arrayRole = 0.toString()
        }
        else if (list == "Visitor") {
            arrayRole = "visitor"
        }
        else if (list == "Subcon"){
            arrayRole = "subcon"
        }
        else if (list == "Employee"){
            arrayRole = "employee"
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New picture.")
        values.put(MediaStore.Images.Media.TITLE, "From the camera.")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, Helper.CAMERA_CAPTURE_CODE)
    }
    private fun backToLobby(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        backToLobby()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            Helper.CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Helper.toastMessage(this@RegisterActivity, "Permission denied.")
                }
            }
        }
    }

    private fun submitForm(){
        val namaVal = nama!!.text.toString().trim()
        val noidentityVal = unique_identity_number!!.text.toString().trim()
        val typeidentityVal = arrayIndentity
        val roleVal = arrayRole

        if(encodedImage.isNotEmpty() && namaVal.isNotEmpty() && noidentityVal.isNotEmpty() && typeidentityVal != "0" && roleVal != "0"){
            Log.i("data yang akan di kirim", typeidentityVal + roleVal + noidentityVal + namaVal)
            Log.i("KAMERA HAHAHA", encodedImage);

            RetrofitClient.instance.submitUserData(namaVal, typeidentityVal, noidentityVal, encodedImage, roleVal).enqueue(object :
                Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) = if (response.isSuccessful) {
                    try {
                        val result = JSONObject(response.body()!!.string())
                        val message: String = result.getString("message")

                        val i = Intent(
                            this@RegisterActivity,
                            MainActivity::class.java
                        )
                        startActivity(i)


                        Helper.toastMessage(this@RegisterActivity, message)


                    } catch (e: JSONException) {
                        Log.i("ERROR1", e.message.toString())
                        e.printStackTrace()
                    } catch (e: IOException) {
                        Log.i("ERROR2", e.message.toString())
                        e.printStackTrace()
                    }
                } else {
                    try {
                        val err = JSONObject(response.errorBody()?.string())

                        val message = err.getString("message")
                        Log.i("ERROR RESPONSE", err.toString())
                        Helper.alertDialog(
                            this@RegisterActivity,
                            message
                        )


                    } catch (e: JSONException) {
                        Log.i("ERROR3", e.message.toString())
                        e.printStackTrace()
                    } catch (e: IOException) {
                        Log.i("ERROR4", e.message.toString())
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.i("ERROR5", t.message.toString())
                }

            })
        }else{
            Helper.toastMessage(this, "Mohon lengkapi data Anda.")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check that it is the SecondActivity with an OK result
        if(resultCode == Activity.RESULT_OK){



            when(requestCode){

                Helper.CAMERA_CAPTURE_CODE -> {
//                    photo!!.setImageURI(image_uri)

                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, image_uri)
                    val nh = (bitmap.height * (256.0 / bitmap.width)).toInt()
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, nh, true)
                    encodedImage = Helper.encodeImage(scaledBitmap!!)

                    val bp = MediaStore.Images.Media.getBitmap(
                        getContext().getContentResolver(),
                        image_uri
                    )
                    val matrix = Matrix()
                    matrix.postRotate(90F)

                    val bmp = Bitmap.createBitmap(bp,0,0,bp.getWidth(),
                        bp.getHeight(),
                        matrix,
                        true
                    )
                    photo!!.setImageBitmap(bmp)

//                    Log.i("Base64",encodedImage)
                }
            }
        }

    }

}