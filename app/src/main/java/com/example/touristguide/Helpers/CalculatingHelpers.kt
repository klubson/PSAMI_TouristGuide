package com.example.touristguide.Helpers

import com.google.android.gms.maps.model.LatLng
import kotlin.math.round

class CalculatingHelpers {
    companion object {

        var totalTravelDistance : Int = 0
        var totalTravelTime : Int = 0

        fun calculateDistanceAndFormatToString(distanceInMetres : Int) : String{
            return if (distanceInMetres < 1000){
                "$distanceInMetres m"
            } else {
                (round((distanceInMetres.toDouble() / 1000.0) * 100.0) / 100.0).toString() + " km"
            }
        }

        fun calculateTimeAndFormatToString(timeInSeconds : Int) : String{
            var minutes = round(timeInSeconds / 60.0)
            var hours = 0
            while (minutes > 60){
                hours += 1
                minutes -= 60
            }
            return if (hours > 0){
                hours.toString() + "h " + minutes.toInt().toString() + "min"
            } else {
                minutes.toInt().toString() + "min"
            }
        }

        fun decodePolyline(encoded: String): List<LatLng> {
            val poly = ArrayList<LatLng>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0
            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat
                shift = 0
                result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng
                val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
                poly.add(latLng)
            }
            return poly
        }
    }
}