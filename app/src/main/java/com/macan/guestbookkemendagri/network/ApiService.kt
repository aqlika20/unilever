package com.macan.guestbookkemendagri.network

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

//    // post image capture
//    @FormUrlEncoded
//    @POST("/api/attendances/signin")
//    fun findGuest(
//        @Field("photo") photo: String
//    ): Call<ResponseBody>

    // post image capture
    @FormUrlEncoded
    @POST("/api/persons/attendance")
    fun findGuest(
        @Field("photo") photo: String
    ): Call<ResponseBody>

    // data absen
    @FormUrlEncoded
    @POST("/api/persons/register")
    fun submitUserData(
            @Field("name") name: String,
            @Field("identity_type_id") identity_type_id: String,
            @Field("unique_identity_number") unique_identity_number: String,
            @Field("photo") photo: String,
            @Field("role") role: String,
    ): Call<ResponseBody>

}