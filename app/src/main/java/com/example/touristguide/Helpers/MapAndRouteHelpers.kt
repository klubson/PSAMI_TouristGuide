package com.example.touristguide.Helpers

import android.location.Location
import com.example.touristguide.Spot
import com.google.android.gms.maps.model.LatLng

class MapAndRouteHelpers {

    companion object {

        var locationPermissionGranted = false
        var lastKnownLocation: Location? = null
        const val DEFAULT_ZOOM = 13
        const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private val defaultSpot = Spot()

        fun getDirectionURL(origin: LatLng, dest: LatLng, secret: String) : String{
            return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${dest.latitude},${dest.longitude}" +
                    "&sensor=false" +
                    "&mode=driving" +
                    "&key=$secret"
        }

        fun setDefaultSpot(){
            defaultSpot.latitude = 52.40995297951002
            defaultSpot.longitude = 16.92583832833938
            defaultSpot.name = "Bieżąca lokalizacja"
        }

        fun getDefaultSpot(): Spot {
            return defaultSpot
        }

        fun getDefaultSpotLatLng(): LatLng? {
            return defaultSpot.latitude?.let { defaultSpot.longitude?.let { it1 -> LatLng(it, it1) } }
        }


    }
}