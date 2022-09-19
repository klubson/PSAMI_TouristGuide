package com.example.touristguide

class Spot{
    var id: Int? = null
    var name: String? = null
    var longitude: Double? = null
    var latitude: Double? = null
    var avgtime: Double? = null
    var category: String? = null
    var url: String? = null


    constructor(id: Int,
                name: String,
                longitude: Double,
                latitude: Double,
                avgtime: Double,
                category: String,
                url: String){
        this.id = id
        this.name = name
        this.longitude = longitude
        this.latitude = latitude
        this.avgtime = avgtime
        this.category = category
        this.url = url
    }

    constructor()

}