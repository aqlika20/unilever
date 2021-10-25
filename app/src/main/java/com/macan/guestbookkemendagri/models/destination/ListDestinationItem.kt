package com.macan.guestbookkemendagri.models.destination

open class ListDestinationItem(
    val type: Int
) {
    companion object {
        const val TYPE_PRIMARY = 0
        const val TYPE_DETAIL = 1
    }
}