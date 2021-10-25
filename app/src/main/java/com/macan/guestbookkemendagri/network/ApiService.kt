package com.macan.guestbookkemendagri.network

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("find-guest")
    fun findGuest(
        @Field("face") face: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("insert-guest")
    fun insertGuest(
            @Field("nik") nik: String,
            @Field("phone") phone: String,
            @Field("destination_detail") destinationDetail: Int
    ): Call<ResponseBody>

    //liveness api
    @Headers("Content-Type: application/json")
    @POST
    fun checkMouth(
        @Body body: JsonObject,
        @Url url: String = "http://128.199.55.64/api/mouth"
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST
    fun checkEyes(
        @Body body: JsonObject,
        @Url url: String = "http://128.199.55.64/api/eyes"
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST
    fun checkHeadRight(
        @Body body: JsonObject,
        @Url url: String = "http://128.199.55.64/api/head_right"
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST
    fun checkHeadLeft(
        @Body body: JsonObject,
        @Url url: String = "http://128.199.55.64/api/head_left"
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST
    fun checkEyeLeft(
        @Body body: JsonObject,
        @Url url: String = "http://128.199.55.64/api/eye_left"
    ): Call<ResponseBody>


    @Headers("Content-Type: application/json")
    @POST
    fun checkEyeRight(
        @Body body: JsonObject,
        @Url url: String = "http://128.199.55.64/api/eye_right"
    ): Call<ResponseBody>





}