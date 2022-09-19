package com.example.touristguide

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.example.touristguide.BuildConfig.GOOGLE_MAPS_API_KEY
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.calculateDistanceAndFormatToString
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.calculateTimeAndFormatToString
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.decodePolyline
import com.example.touristguide.Helpers.MapAndRouteHelpers
import com.example.touristguide.Helpers.MapAndRouteHelpers.Companion.DEFAULT_ZOOM
import com.example.touristguide.Helpers.MapAndRouteHelpers.Companion.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.example.touristguide.Helpers.MapAndRouteHelpers.Companion.getDirectionURL
import com.example.touristguide.Helpers.MapAndRouteHelpers.Companion.lastKnownLocation
import com.example.touristguide.Helpers.MapAndRouteHelpers.Companion.locationPermissionGranted
import com.example.touristguide.NavigationClasses.MapData
import com.example.touristguide.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener, SpotsListAdapter.OnItemClickListener{

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var databaseHandler: DBHelper

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var showSearchBarButton: ImageButton
    private lateinit var searchCardView: CardView
    private lateinit var clearSearchBarButton: ImageButton
    private lateinit var closeSearchBarButton: ImageButton
    private lateinit var categorySpinner: Spinner

    private lateinit var moreActionsCardView: CardView
    private lateinit var objectNameOnCardView: TextView
    private lateinit var closeMoreActionsCardView: ImageButton
    private lateinit var googleSearchImageButton: ImageButton
    private lateinit var wikipediaImageButton: ImageButton
    private lateinit var createRouteImageButton: ImageButton
    private lateinit var tripImageButton: ImageButton

    private lateinit var navigationCardView: CardView
    private lateinit var totalTravelDistanceLabel: TextView
    private lateinit var totalTravelDistanceValue: TextView
    private lateinit var totalTravelTimeLabel: TextView
    private lateinit var totalTravelTimeValue: TextView
    private lateinit var eraseRouteButton: Button
    private lateinit var showRouteButton: Button

    private lateinit var spotsListCardView: CardView
    private lateinit var spotsListCardViewCloseButton: ImageButton
    private lateinit var spotsListRecyclerView: RecyclerView
    private lateinit var spotsListAdapter: SpotsListAdapter

    private lateinit var tripSettingCardView: CardView
    private lateinit var tripStartTimeSpinner: Spinner
    private lateinit var tripEndTimeSpinner: Spinner
    private lateinit var tripCategorySpinner: Spinner
    private lateinit var clearTripCardButton: ImageButton
    private lateinit var closeTripCardButton: ImageButton
    private lateinit var showTripButton: Button

    var dataNamesOnly: ArrayList<String>? = null
    var categoriesPolishNames : ArrayList<String>? = null

    private var chosenSpot : Spot? = null
    private var chosenSpotLatLng : LatLng? = null
    var chosenCategoryName : String? = null
    var chosenCategory : Category? = null
    var route : Route? = null

    private val testing : Boolean = true
    private var roadType : Boolean = true //true -> wycieczka, false -> nawigacja

    private var spotsCoordinatesArray = ArrayList<Spot>()

    private var startTimePositionIndex : Int = 0
    private var endTimePositionIndex : Int = 0
    private val client = OkHttpClient()

    private var spotListFromApi = ArrayList<Spot>()

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, GOOGLE_MAPS_API_KEY)
        }
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Places.initialize(applicationContext, GOOGLE_MAPS_API_KEY)
        placesClient = Places.createClient(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        initializeComponents()
        MapAndRouteHelpers.setDefaultSpot()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetJavaScriptEnabled")
    fun initializeComponents(){
        databaseHandler = DBHelper(this)
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        autoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
            chosenSpot = databaseHandler.getSpotByName(autoCompleteTextView.text.toString())
            if (chosenSpot != null) {
                val newLocation = Location("")
                newLocation.latitude = chosenSpot!!.latitude!!
                newLocation.longitude = chosenSpot!!.longitude!!
                onLocationChanged(newLocation)
                moreActionsCardView.visibility = View.VISIBLE
                objectNameOnCardView.text = chosenSpot!!.name
            }
        }
        showSearchBarButton = findViewById(R.id.showSearchBarButton)
        showSearchBarButton.setOnClickListener {
            showSearchBarButton.visibility = View.INVISIBLE
            searchCardView.visibility = View.VISIBLE
        }

        searchCardView = findViewById(R.id.searchCardView)
        categorySpinner = findViewById(R.id.categoryPickerSearchCard)
        categoriesPolishNames = databaseHandler.categoriesPolishNames
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriesPolishNames!!)
        categorySpinner.adapter = adapter
        categorySpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                chosenCategoryName = categoriesPolishNames!![position]
                chosenCategory = chosenCategoryName?.let { databaseHandler.getCategoryByPolishName(it) }
                if (chosenCategory != null){
                    dataNamesOnly = chosenCategory!!.category?.let {
                        databaseHandler.getSpotNamesByCategory(
                            it
                        )
                    }
                    autoCompleteTextView.setAdapter(ArrayAdapter(
                        baseContext, android.R.layout.simple_dropdown_item_1line, dataNamesOnly!!
                    ))
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        clearSearchBarButton = findViewById(R.id.clearSearchBarButtonSearchCard)
        clearSearchBarButton.setOnClickListener {
            resetSettings(clearAutoCompleteTextView = true, clearMap = false)
        }
        closeSearchBarButton = findViewById(R.id.closeSearchBarButtonSearchCard)
        closeSearchBarButton.setOnClickListener {
            showSearchBarButton.visibility = View.VISIBLE
            searchCardView.visibility = View.INVISIBLE
            moreActionsCardView.visibility = View.INVISIBLE
            //resetSettings(clearAutoCompleteTextView = true, clearMap = true)
            //resetRouteParameters()
            navigationCardView.visibility = View.INVISIBLE
        }

        moreActionsCardView = findViewById(R.id.spotCardView)
        objectNameOnCardView = findViewById(R.id.objectNameCardView)
        closeMoreActionsCardView = findViewById(R.id.closeSpotCardViewButton)
        closeMoreActionsCardView.setOnClickListener {
            moreActionsCardView.visibility = View.INVISIBLE
        }
        googleSearchImageButton = findViewById(R.id.googleSearchImageButton)
        googleSearchImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            var searchPhrase = chosenSpot!!.name
            if (!searchPhrase!!.contains("Pozna")){
                searchPhrase = "$searchPhrase Poznań"
            }
            intent.putExtra(SearchManager.QUERY, searchPhrase)
            startActivity(intent)
        }
        wikipediaImageButton = findViewById(R.id.wikipediaImageButton)
        wikipediaImageButton.setOnClickListener {
            if (chosenSpot?.url != ""){
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(chosenSpot?.url))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Brak podstrony Wikipedii!", Toast.LENGTH_LONG).show()
            }

        }

        createRouteImageButton = findViewById(R.id.createRouteImageButton)
        createRouteImageButton.setOnClickListener {
            roadType = false
            showSearchBarButton.visibility = View.VISIBLE
            searchCardView.visibility = View.INVISIBLE
            moreActionsCardView.visibility = View.INVISIBLE
            resetRouteParameters()
            if (chosenSpot != null) {
                spotsCoordinatesArray.add(MapAndRouteHelpers.getDefaultSpot())
                spotsCoordinatesArray.add(chosenSpot!!)
//                databaseHandler.getSpotByName("Pomnik Harcerski")
//                    ?.let { it1 -> spotsCoordinatesArray.add(it1) }
//                route = Route()
//                route!!.setSpotsList(spotsCoordinatesArray)
                createNavigationLine(spotsCoordinatesArray)

            }

        }

        tripImageButton = findViewById(R.id.tripImageButton)
        tripImageButton.setOnClickListener {
            roadType = true
            moreActionsCardView.visibility = View.INVISIBLE
            searchCardView.visibility = View.INVISIBLE
            tripSettingCardView.visibility = View.VISIBLE
        }

        navigationCardView = findViewById(R.id.navigationCardView)
        totalTravelTimeLabel = findViewById(R.id.totalTravelTimeLabel)
        totalTravelTimeValue = findViewById(R.id.totalTravelTimeValue)
        totalTravelDistanceLabel = findViewById(R.id.totalTravelDistanceLabel)
        totalTravelDistanceValue = findViewById(R.id.totalTravelDistanceValue)
        eraseRouteButton = findViewById(R.id.eraseRouteButton)
        eraseRouteButton.setOnClickListener{
            navigationCardView.visibility = View.INVISIBLE
            moreActionsCardView.visibility = View.VISIBLE
            mMap.clear()
            chosenSpotLatLng?.let { it1 -> MarkerOptions().position(it1) }
                ?.let { it2 -> mMap.addMarker(it2) }
            resetRouteParameters()
            spotListFromApi.clear()
            showSearchBarButton.visibility = View.VISIBLE
        }

        showRouteButton = findViewById(R.id.showSpotsList)
        showRouteButton.setOnClickListener {
            spotsListAdapter = SpotsListAdapter(spotsCoordinatesArray, this)
            spotsListRecyclerView.adapter = spotsListAdapter
            spotsListRecyclerView.layoutManager = LinearLayoutManager(this)
            spotsListRecyclerView.setHasFixedSize(true)
            navigationCardView.visibility = View.INVISIBLE
            spotsListCardView.visibility = View.VISIBLE
        }

        spotsListCardView = findViewById(R.id.spotsListCardView)
        spotsListCardViewCloseButton = findViewById(R.id.closeSpotsListCardView)
        spotsListCardViewCloseButton.setOnClickListener {
            spotsListCardView.visibility = View.INVISIBLE
            navigationCardView.visibility = View.VISIBLE
        }
        spotsListRecyclerView = findViewById(R.id.spotsListRecyclerView)

        tripSettingCardView = findViewById(R.id.tripSettingsCardView)
        tripStartTimeSpinner = findViewById(R.id.tripStartTimePicker)
        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, createAdapterForTimePicker())
        tripStartTimeSpinner.adapter = timeAdapter
        tripStartTimeSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                startTimePositionIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        tripEndTimeSpinner = findViewById(R.id.tripEndTimePicker)
        tripEndTimeSpinner.adapter = timeAdapter
        tripEndTimeSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                endTimePositionIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        tripCategorySpinner = findViewById(R.id.categoryPickerTripCard)
        tripCategorySpinner.adapter = adapter
        tripCategorySpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        clearTripCardButton = findViewById(R.id.clearSearchBarButtonTripCard)
        clearTripCardButton.setOnClickListener {
            tripStartTimeSpinner.setSelection(0)
            tripEndTimeSpinner.setSelection(0)
            tripCategorySpinner.setSelection(0)
        }
        closeTripCardButton = findViewById(R.id.closeSearchBarButtonTripCard)
        closeTripCardButton.setOnClickListener {
            tripSettingCardView.visibility = View.INVISIBLE
            searchCardView.visibility = View.VISIBLE
            moreActionsCardView.visibility = View.VISIBLE
        }
        showTripButton = findViewById(R.id.showTripButton)
        showTripButton.setOnClickListener {
            if(tripStartTimeSpinner.selectedItem == 0 || tripEndTimeSpinner.selectedItem == 0){

            }
            else {
                tripSettingCardView.visibility = View.INVISIBLE
                createTrip(tripStartTimeSpinner.selectedItemId.toInt(), tripEndTimeSpinner.selectedItemId.toInt())
            }
        }


    }

    private fun createTrip(startPosition : Int, endPosition : Int){
        val tripTime = calculateTripTime(startPosition, endPosition)
        getTimeToFirst()
        if (testing){
            spotListFromApi = getTestRoad(this.baseContext)
        } else {
            chosenSpot?.id?.let { route?.let { it1 -> getSpotListFromApi(it, tripTime, it1.getTotalTime()) } }
        }

        spotListFromApi.add(0, MapAndRouteHelpers.getDefaultSpot())
        for (spot in spotListFromApi) {
            if (spot.avgtime != null){
                route?.addToTotalAvgSpotsTime(spot.avgtime!!.toInt())
            }
        }
        resetSettings(true, true)
        spotsCoordinatesArray = spotListFromApi

        createNavigationLine(spotListFromApi)
    }

    private fun calculateTripTime(startPosition : Int, endPosition : Int) : Int{
        return abs(endPosition - startPosition) *15
    }

    private fun getTimeToFirst(){
        val array = java.util.ArrayList<Spot>()
        array.add(MapAndRouteHelpers.getDefaultSpot())
        array.add(chosenSpot!!)
        createNavigationLine(array)
    }

    private fun callApi(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.println(Log.ERROR,"error","API execute failed")
            }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body!!.string()
                Log.println(Log.DEBUG, "json", json)
                spotListFromApi = Klaxon().parseArray<Spot>(response.body!!.string()) as ArrayList<Spot>
            }
        })
    }

    private fun getSpotListFromApi(startSpotID : Int, fullTime : Int, timeToFirst : Int){
            val baseURL = "https://37uw3xmyg9.execute-api.eu-central-1.amazonaws.com/test/road?"
//        val requestString = """{ "id": $startSpotID,
//            |                    "full_time": $fullTime,
//            |                    "time_to_first": $timeToFirst}""".trimMargin()
            var requestString = baseURL + "id=" + startSpotID + "&full_time=" +  fullTime + "&time_to_first=" + timeToFirst
            requestString = "https://37uw3xmyg9.execute-api.eu-central-1.amazonaws.com/test/road?id=40&full_time=70&time_to_first=3"
//        requestString = "https://api.github.com/users/Evin1-/repos"
            callApi(requestString)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    private fun createAdapterForTimePicker() : ArrayList<String> {
        val whateverDateYouWant = Date()
        val calendar = Calendar.getInstance()
        calendar.time = whateverDateYouWant

        val unroundedMinutes = calendar[Calendar.MINUTE]
        val mod = unroundedMinutes % 15
        calendar.add(Calendar.MINUTE, if (mod < 8) -mod else 15 - mod)

        val timeArray = ArrayList<String>()
        timeArray.add("wybierz")
        for(i in 1..96){
            var hours = calendar.time.hours.toString()
            if (hours.length == 1){
                hours = "0$hours"
            }
            var minutes = calendar.time.minutes.toString()
            if (minutes == "0"){
                minutes = "00"
            }
            timeArray.add("$hours:$minutes")
            calendar.add(Calendar.MINUTE, 15)
        }
        return timeArray
    }

    private fun resetSettings(clearAutoCompleteTextView: Boolean, clearMap: Boolean){
        categorySpinner.setSelection(0)
        dataNamesOnly = databaseHandler.spotNames
        if(clearAutoCompleteTextView){
            autoCompleteTextView.text.clear()
        }
        if(clearMap){
            mMap.clear()
        }
        resetRouteParameters()
    }

    private fun resetRouteParameters(){
        route = null
        spotsCoordinatesArray.clear()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getLocationPermission()
        getDeviceLocation()
    }
    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.longitude, location.latitude)
        //val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 50.0f)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(latLng.longitude - 10.0, latLng.latitude),
            10.0F
        )
        mMap.animateCamera(cameraUpdate)
        mMap.clear()
        //mMap.addMarker(MarkerOptions().position(latLng))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun createNavigationLine(spots : ArrayList<Spot>){
        route = Route()
        route!!.setSpotsList(spots)
        val arraySize = spots.size
        for (index in 0 until arraySize-1){
            val originLatLng = spots[index].latitude?.let { spots[index].longitude?.let { it1 ->
                LatLng(it,
                    it1
                )
            } }
            val destinationLatLng = spots[index+1].latitude?.let { spots[index+1].longitude?.let { it1 ->
                LatLng(it,
                    it1
                )
            } }
            val url = getDirectionURL(originLatLng!!, destinationLatLng!!, GOOGLE_MAPS_API_KEY)
            asyncTask(index, arraySize-2, url)
            originLatLng.let { MarkerOptions().position(it) }.let { mMap.addMarker(it) }
        }
        val lastSpotLatLng = spots.last().latitude?.let { spots.last().longitude?.let { it1 ->
            LatLng(it,
                it1
            )
        } }
        lastSpotLatLng?.let { MarkerOptions().position(it) }?.let { mMap.addMarker(it) }
        MapAndRouteHelpers.getDefaultSpotLatLng()
            ?.let { CameraUpdateFactory.newLatLngZoom(it, 14F) }?.let { mMap.animateCamera(it) }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    private fun getTestRoad(context: Context) : ArrayList<Spot>{
        val spotsList = ArrayList<Spot>()
        val json: String
        try {
            val iS = context.assets.open("testRoad.json")
            val size = iS.available()
            val buffer = ByteArray(size)
            iS.read(buffer)
            iS.close()

            json = String(buffer)
            val jsonArray = JSONArray(json)
            for(i in 0 until jsonArray.length()){
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getInt("id")
                val name = obj.getString("name")
                val latitude = obj.getDouble("latitude")
                val longitude = obj.getDouble("longitude")
                val avgtime = obj.getDouble("avgtime")
                val category = obj.getString("category")
                val url = obj.getString("url")
                spotsList.add(Spot(id, name, latitude, longitude, avgtime, category, url))
            }

        } catch (e: IOException){
            e.printStackTrace()
        } catch (e: JSONException){
            e.printStackTrace()
        }

        return spotsList
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        MapAndRouteHelpers.getDefaultSpotLatLng()?.let {
            CameraUpdateFactory
                .newLatLngZoom(it, DEFAULT_ZOOM.toFloat())
        }?.let { mMap.moveCamera(it) }
        mMap.uiSettings.isMyLocationButtonEnabled = true

        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        MapAndRouteHelpers.getDefaultSpotLatLng()?.let {
                            CameraUpdateFactory
                                .newLatLngZoom(it, DEFAULT_ZOOM.toFloat())
                        }?.let { mMap.moveCamera(it) }
                        mMap.uiSettings.isMyLocationButtonEnabled = true
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun asyncTask(index: Int, loopsCount: Int, url: String?){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val client = OkHttpClient()
            val request = url?.let { Request.Builder().url(it).build() }
            val response = request?.let { client.newCall(it).execute() }
            val data = response?.body?.string()

            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data, MapData::class.java)
                route?.addToTotalTime(respObj.routes[0].legs[0].duration.value)
                route?.addToTotalDistance(respObj.routes[0].legs[0].distance.value)
                val path =  ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size){
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                e.printStackTrace()
            }

            handler.post {
                val lineoption = PolylineOptions()
                for (i in result.indices){
                    lineoption.addAll(result[i])
                    lineoption.width(10f)
                    lineoption.color(Color.GREEN)
                    lineoption.geodesic(true)
                }
                mMap.addPolyline(lineoption)
                if (index == loopsCount){
                    totalTravelDistanceValue.text = route?.let { calculateDistanceAndFormatToString(it.getTotalDistance()) }
                    totalTravelTimeValue.text = route?.let { calculateTimeAndFormatToString(it.getTotalTime() + it.getTotalAvgSpotsTime()) }
                    navigationCardView.visibility = View.VISIBLE
                }

            }
        }
    }

}