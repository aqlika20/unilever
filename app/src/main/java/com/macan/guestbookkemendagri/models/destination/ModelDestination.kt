package com.macan.guestbookkemendagri.models.destination

class ModelDestination(private var id: Int, private var primary: String, private var detail: String) {

    fun getId() : Int{
        return this.id
    }

    fun getPrimary() : String {
        return this.primary
    }

    fun getDetail() : String {
        return this.detail
    }





}