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