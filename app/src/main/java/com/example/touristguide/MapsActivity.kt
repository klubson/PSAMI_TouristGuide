package com.example.touristguide

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.przewodnikpotoruniu.DBHelper
import com.example.przewodnikpotoruniu.Object
import com.example.touristguide.BuildConfig.GOOGLE_MAPS_API_KEY
import com.example.touristguide.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener{

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var databaseHandler: DBHelper
    private lateinit var showSearchBarButton: ImageButton
    private lateinit var clearSearchBarButton: ImageButton
    private lateinit var closeSearchBarButton: ImageButton
    private lateinit var cardView: CardView
    private lateinit var objectNameOnCardView: TextView
    private lateinit var googleSearchCardViewButton: ImageButton
    private lateinit var wikipediaSiteCardViewButton: ImageButton
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private val defaultLocation = LatLng( 52.40995297951002, 16.92583832833938)
    var dataNamesOnly: ArrayList<String>? = null
    var objectsAdapter : ArrayAdapter<String>? = null
    var chosenObjectName : String? = null
    var chosenObject : Object? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    @SuppressLint("SetJavaScriptEnabled")
    fun initializeComponents(){
        databaseHandler = DBHelper(this)
        dataNamesOnly = databaseHandler.objectNames
        objectsAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, dataNamesOnly!!
        )
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        autoCompleteTextView.setAdapter(objectsAdapter)
        autoCompleteTextView.setOnItemClickListener { _, _, _, id ->
            chosenObjectName = dataNamesOnly?.get(id.toInt())
            chosenObject = chosenObjectName?.let { databaseHandler.getObjectByItsName(it) }
            if (chosenObject != null) {
                val newLocation = Location("")
                newLocation.latitude = chosenObject!!.latitude!!
                newLocation.longitude = chosenObject!!.longitude!!
                onLocationChanged(newLocation)
                cardView.visibility = View.VISIBLE
                objectNameOnCardView.text = chosenObjectName
            }
        }
        showSearchBarButton = findViewById(R.id.showSearchBarButton)
        showSearchBarButton.setOnClickListener {
            changeObjectsVisbility(View.INVISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE)
        }
        clearSearchBarButton = findViewById(R.id.clearSearchBarButton)
        clearSearchBarButton.setOnClickListener {
            autoCompleteTextView.text.clear()
        }
        closeSearchBarButton = findViewById(R.id.closeSearchBarButton)
        closeSearchBarButton.setOnClickListener {
            changeObjectsVisbility(View.VISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)
        }

        cardView = findViewById(R.id.objectCardView)
        objectNameOnCardView = findViewById(R.id.objectNameCardView)
        googleSearchCardViewButton = findViewById(R.id.googleCardViewButton)
        googleSearchCardViewButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, chosenObjectName)
            startActivity(intent)
        }
        wikipediaSiteCardViewButton = findViewById(R.id.wikiCardViewButton)
        wikipediaSiteCardViewButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(chosenObject?.url))
            startActivity(intent)
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
        clearButtonVisibility: Int,
        closeButtonVisibility: Int,
        autoTextViewVisibility: Int){
        showSearchBarButton.visibility = showButtonVisibility
        clearSearchBarButton.visibility = clearButtonVisibility
        closeSearchBarButton.visibility = closeButtonVisibility
        autoCompleteTextView.visibility = autoTextViewVisibility

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getLocationPermission()
        getDeviceLocation()
    }
    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.longitude, location.latitude)
        //val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 50.0f)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(latLng.longitude - 10.0, latLng.latitude), DEFAULT_ZOOM.toFloat())
        mMap.animateCamera(cameraUpdate)
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(latLng))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
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

//        try {
//            if (locationPermissionGranted) {
//                val locationResult = fusedLocationProviderClient.lastLocation
//                locationResult.addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
//                        // Set the map's camera position to the current location of the device.
//                        lastKnownLocation = task.result
//                        if (lastKnownLocation != null) {
//                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                LatLng(lastKnownLocation!!.latitude,
//                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
//                        }
//                    } else {
//                        mMap.moveCamera(CameraUpdateFactory
//                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
//                        mMap.uiSettings.isMyLocationButtonEnabled = true
//                    }
//                }
//            }
//        } catch (e: SecurityException) {
//            Log.e("Exception: %s", e.message, e)
//        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 13
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

}