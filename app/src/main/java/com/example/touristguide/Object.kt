package com.example.przewodnikpotoruniu

class Object {
    var id: Int? = null
    var name: String? = null
    var longitude: Double? = null
    var latitude: Double? = null
    var avgtime: Double? = null
    var category: String? = null
    var url: String? = null

    constructor(){}

    fun isInList(list: ArrayList<Object>) : Boolean{
        if (list.contains(this)){
            return true
        }
        return false
    }
}