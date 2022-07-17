package com.macan.guestbookkemendagri.network

import android.provider.Settings
import android.util.Log
import com.macan.guestbookkemendagri.helper.Helper
import com.macan.guestbookkemendagri.helper.MyApp
import okhttp3.*
import okio.Buffer


import java.io.IOException

class AppHttpInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val android_id = Settings.Secure.getString(
            MyApp.getContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        )

        var request = chain.request()
            .newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("device-unique-id", android_id.toString())
            .build()
//        val requestBuilder = request.newBuilder()
//        val accessToken = Helper.API_KEY


        //adding a header to the original request
//        requestBuilder.addHeader("Accept", "application/json")
//
//
//
//        //if access_token is in storage, means that user is logged in, add the bearer token to request header.
//        if(request.url().toString().contains(Helper.BASE_URL)){
//            Log.i("INTERCEPTOR API TOKEN", accessToken)
//            var postBodyString = bodyToString(request.body())
//            val concat = if(postBodyString.isNotEmpty()) "&" else ""
//            val apiTokenBody = FormBody.Builder().add("api_token", accessToken).build()
//            postBodyString = postBodyString + concat + bodyToString(apiTokenBody)
//            request = requestBuilder.post(
//                RequestBody.create(
//                    MediaType.parse("application/x-www-form-urlencoded;charset=UTF-8"),
//                    postBodyString
//                )
//            ).build()
//        }

//        requestBuilder.addHeader("device-unique-id", "qhf4mvnkjgiaabd68iuepbrakhemrukrx4cog4ss")


        Log.i("Action", request.url().toString())

        //returns a response
        return chain.proceed(request)
    }

    private fun bodyToString(request: RequestBody?): String {
        return try {
            val buffer = Buffer()
            request?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: IOException) {
            "error"
        }
    }
}