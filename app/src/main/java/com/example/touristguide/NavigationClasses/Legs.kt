package com.example.touristguide.NavigationClasses

class Legs {
    var distance = Distance()
    var duration = Duration()
    var end_address = ""
    var start_address = ""
    var end_location =Location()
    var start_location = Location()
    var steps = ArrayList<Steps>()
}