package com.example.touristguide

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
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
import com.example.touristguide.BuildConfig.GOOGLE_MAPS_API_KEY
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.calculateDistanceAndFormatToString
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.calculateTimeAndFormatToString
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.decodePolyline
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.totalTravelDistance
import com.example.touristguide.Helpers.CalculatingHelpers.Companion.totalTravelTime
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors

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
    private lateinit var googleSearchCardViewButton: ImageButton
    private lateinit var wikipediaSiteCardViewButton: ImageButton
    private lateinit var createRouteCardViewButton: ImageButton

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

    var dataNamesOnly: ArrayList<String>? = null
    var categoriesPolishNames : ArrayList<String>? = null

    private var chosenSpotName : String? = null
    private var chosenSpot : Spot? = null
    private var chosenSpotLatLng : LatLng? = null
    var chosenCategoryName : String? = null
    var chosenCategory : Category? = null
    var route : Route? = null

    private val spotsCoordinatesArray = ArrayList<Spot>()

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
            changeObjectsVisbility(View.INVISIBLE, View.VISIBLE)
        }

        searchCardView = findViewById(R.id.searchCardView)
        categorySpinner = findViewById(R.id.spinner)
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

        clearSearchBarButton = findViewById(R.id.clearSearchBarButton)
        clearSearchBarButton.setOnClickListener {
            resetSettings(clearAutoCompleteTextView = true, clearMap = false)
        }
        closeSearchBarButton = findViewById(R.id.closeSearchBarButton)
        closeSearchBarButton.setOnClickListener {
            changeObjectsVisbility(View.VISIBLE, View.INVISIBLE)
            moreActionsCardView.visibility = View.INVISIBLE
            resetSettings(clearAutoCompleteTextView = true, clearMap = true)
        }

        moreActionsCardView = findViewById(R.id.spotCardView)
        objectNameOnCardView = findViewById(R.id.objectNameCardView)
        googleSearchCardViewButton = findViewById(R.id.googleCardViewButton)
        googleSearchCardViewButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, chosenSpotName)
            startActivity(intent)
        }
        wikipediaSiteCardViewButton = findViewById(R.id.wikiCardViewButton)
        wikipediaSiteCardViewButton.setOnClickListener {
            if (chosenSpot?.url != ""){
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(chosenSpot?.url))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Brak podstrony Wikipedii!", Toast.LENGTH_LONG).show()
            }

        }

        navigationCardView = findViewById(R.id.navigationCardView)
        totalTravelTimeLabel = findViewById(R.id.totalTravelTimeLabel)
        totalTravelTimeValue = findViewById(R.id.totalTravelTimeValue)
        totalTravelDistanceLabel = findViewById(R.id.totalTravelDistanceLabel)
        totalTravelDistanceValue = findViewById(R.id.totalTravelDistanceValue)

        createRouteCardViewButton = findViewById(R.id.navigationCardViewButton)
        createRouteCardViewButton.setOnClickListener {
            changeObjectsVisbility(View.VISIBLE, View.INVISIBLE)
            moreActionsCardView.visibility = View.INVISIBLE
            if (chosenSpot != null) {
                spotsCoordinatesArray.add(MapAndRouteHelpers.getDefaultSpot())
                spotsCoordinatesArray.add(chosenSpot!!)
                databaseHandler.getSpotByName("Pomnik Harcerski")
                    ?.let { it1 -> spotsCoordinatesArray.add(it1) }
                route = Route()
                route!!.setSpotsList(spotsCoordinatesArray)
                createNavigationLine(route!!.getSpotsList())

            }

        }

        eraseRouteButton = findViewById(R.id.eraseRouteButton)
        eraseRouteButton.setOnClickListener{
            navigationCardView.visibility = View.INVISIBLE
            moreActionsCardView.visibility = View.VISIBLE
            mMap.clear()
            chosenSpotLatLng?.let { it1 -> MarkerOptions().position(it1) }
                ?.let { it2 -> mMap.addMarker(it2) }
            resetRouteParameters()
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

    }

    private fun changeObjectsVisbility(
        showButtonVisibility: Int,
        searchCardViewVisibility: Int){
        showSearchBarButton.visibility = showButtonVisibility
        searchCardView.visibility = searchCardViewVisibility
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
        totalTravelDistance = 0
        totalTravelTime = 0
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
        mMap.addMarker(MarkerOptions().position(latLng))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun createNavigationLine(spots : ArrayList<Spot>){
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
                    totalTravelTimeValue.text = route?.let { calculateTimeAndFormatToString(it.getTotalTime()) }
                    navigationCardView.visibility = View.VISIBLE
                }

            }
        }
    }

}