package com.example.touristguide.Helpers

import android.location.Location
import com.google.android.gms.maps.model.LatLng

class MapHelpers {

    companion object {

        var locationPermissionGranted = false
        var lastKnownLocation: Location? = null
        val defaultLocation = LatLng( 52.40995297951002, 16.92583832833938)
        const val DEFAULT_ZOOM = 13
        const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        fun getDirectionURL(origin: LatLng, dest: LatLng, secret: String) : String{
            return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${dest.latitude},${dest.longitude}" +
                    "&sensor=false" +
                    "&mode=driving" +
                    "&key=$secret"
        }
    }
}