package com.example.touristguide

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.touristguide.BuildConfig.GOOGLE_MAPS_API_KEY
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener{

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var databaseHandler: DBHelper
    private lateinit var showSearchBarButton: ImageButton
    private lateinit var searchCardView: CardView
    private lateinit var clearSearchBarButton: ImageButton
    private lateinit var closeSearchBarButton: ImageButton
    private lateinit var categorySpinner: Spinner
    private lateinit var moreActionsCardView: CardView
    private lateinit var navigationCardView: CardView
    private lateinit var totalTravelDistanceLabel: TextView
    private lateinit var totalTravelDistanceValue: TextView
    private lateinit var totalTravelTimeLabel: TextView
    private lateinit var totalTravelTimeValue: TextView
    private lateinit var objectNameOnCardView: TextView
    private lateinit var googleSearchCardViewButton: ImageButton
    private lateinit var wikipediaSiteCardViewButton: ImageButton
    private lateinit var createRouteCardViewButton: ImageButton
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng( 52.40995297951002, 16.92583832833938)
    var dataNamesOnly: ArrayList<String>? = null
    var categoriesPolishNames : ArrayList<String>? = null
    private var chosenSpotName : String? = null
    private var chosenSpot : Spot? = null
    var chosenCategoryName : String? = null
    var chosenCategory : Category? = null
    var totalTravelDistance : Int = 0
    var totalTravelTime : Int = 0

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
            val chosenSpotLatLng = chosenSpot?.latitude?.let { it1 -> chosenSpot!!.longitude?.let { it2 ->
                LatLng(it1,
                    it2
                )
            } }
            if (chosenSpotLatLng != null) {
                val spotsCoordinatesArray = ArrayList<LatLng>()
                spotsCoordinatesArray.add(defaultLocation)
                spotsCoordinatesArray.add(chosenSpotLatLng)
                val pomnikHarcerski = databaseHandler.getSpotByName("Pomnik Harcerski")
                if (pomnikHarcerski != null) {
                    pomnikHarcerski.latitude?.let { it1 -> pomnikHarcerski.longitude?.let { it2 ->
                        LatLng(it1,
                            it2
                        )
                    } }
                        ?.let { it2 -> spotsCoordinatesArray.add(it2) }
                }
                //createNavigationLine(defaultLocation, chosenSpotLatLng)
                createNavigationLine(spotsCoordinatesArray)
                println("dystans out: $totalTravelDistance")
                println("czas out: $totalTravelTime")

            }



            totalTravelDistanceValue.text = calculateDistanceAndFormatToString(totalTravelDistance)
            totalTravelTimeValue.text = calculateTimeAndFormatToString(totalTravelTime)
            navigationCardView.visibility = View.VISIBLE

        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

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
        totalTravelDistance = 0
        totalTravelTime = 0
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

    private fun createNavigationLine(spotsCoordinates : ArrayList<LatLng>){
        for (i in 0 until spotsCoordinates.size - 1){
            val url = getDirectionURL(spotsCoordinates[i], spotsCoordinates[i+1], GOOGLE_MAPS_API_KEY)
            asyncTask(url)
            mMap.addMarker(MarkerOptions().position(spotsCoordinates[i]))
        }
        mMap.addMarker(MarkerOptions().position(spotsCoordinates.last()))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14F))
    }

    private fun calculateDistanceAndFormatToString(distanceInMetres : Int) : String{
        return if (distanceInMetres < 1000){
            "$distanceInMetres m"
        } else {
            (distanceInMetres.toDouble() / 1000.0).toString() + " km"
        }
    }

    private fun calculateTimeAndFormatToString(timeInSeconds : Int) : String{
        var minutes = timeInSeconds / 60
        var hours = 0
        while (minutes > 60){
            hours += 1
            minutes -= 60
        }
        return if (hours > 0){
            hours.toString() + "h " + minutes.toString() + "min"
        } else {
            minutes.toString() + "min"
        }
    }

    private fun createNavigationLine(from : LatLng, to : LatLng){
        val url = getDirectionURL(from, to, GOOGLE_MAPS_API_KEY)
        mMap.addMarker(MarkerOptions().position(from))
        mMap.addMarker(MarkerOptions().position(to))
        asyncTask(url)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14F))

    }

    private fun getDirectionURL(origin:LatLng, dest:LatLng, secret: String) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
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
        mMap.moveCamera(CameraUpdateFactory
            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
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
                        mMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        mMap.uiSettings.isMyLocationButtonEnabled = true
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun asyncTask(url : String){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body?.string()

            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data, MapData::class.java)
                totalTravelTime += respObj.routes[0].legs[0].duration.value
                totalTravelDistance += respObj.routes[0].legs[0].distance.value
                println("dystans in: $totalTravelDistance")
                println("czas in: $totalTravelTime")
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
            }
        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 13
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

}