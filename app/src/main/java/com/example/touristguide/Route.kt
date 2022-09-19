package com.example.touristguide

class Route {

    private var totalDistance: Int = 0
    private var totalTime: Int = 0
    private var totalAvgSpotsTime: Int = 0
    private lateinit var spotsList: ArrayList<Spot>

    fun getTotalDistance(): Int {
        return this.totalDistance
    }

    fun setTotalDistance(newDistance: Int){
        this.totalDistance = newDistance
    }

    fun addToTotalDistance(distanceToAdd: Int){
        this.totalDistance += distanceToAdd
    }

    fun getTotalTime(): Int {
        return this.totalTime
    }

    fun setTotalTime(newTime: Int){
        this.totalTime = newTime
    }

    fun addToTotalTime(timeToAdd: Int){
        this.totalTime += timeToAdd
    }

    fun getTotalAvgSpotsTime(): Int {
        return this.totalAvgSpotsTime
    }

    fun setTotalAvgSpotsTime(newTime: Int){
        this.totalAvgSpotsTime = newTime
    }

    fun addToTotalAvgSpotsTime(timeToAdd: Int){
        this.totalAvgSpotsTime += timeToAdd
    }

    fun getSpotsList(): ArrayList<Spot> {
        return this.spotsList
    }

    fun setSpotsList(newSpotsList: ArrayList<Spot>){
        this.spotsList = newSpotsList
    }

    fun addToSpotsList(spot: Spot){
        this.spotsList.add(spot)
    }
}