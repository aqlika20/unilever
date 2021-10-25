package com.macan.guestbookkemendagri.network

import android.content.Context
import android.net.ConnectivityManager
import com.macan.guestbookkemendagri.helper.MyApp

class NetworkUtils {

    companion object{
        fun haveNetwork(): Boolean {
            val cm = MyApp.getContext()?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }
    }
}