package com.macan.guestbookkemendagri.network

import android.util.Log
import com.macan.guestbookkemendagri.helper.Helper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    val instance: ApiService by lazy{
        val BASE_URL = Helper.BASE_URL

        Log.i("RETROFIT BASE URL", BASE_URL)

        val interceptor = AppHttpInterceptor()

        val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).readTimeout(60, TimeUnit.SECONDS).connectTimeout(60, TimeUnit.SECONDS).build()


        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        retrofit.create(ApiService::class.java)
    }
}