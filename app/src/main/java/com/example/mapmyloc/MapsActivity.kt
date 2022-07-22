package com.example.mapmyloc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_maps.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback , TaskLoadedCallback, GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnCameraMoveListener,
    GoogleMap.OnCameraMoveCanceledListener,
    GoogleMap.OnCameraIdleListener {

    private val myPermissionCode: Int = 1000
    private var currentPolyline :Polyline? = null
    private lateinit var mMap: GoogleMap
    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()

    private lateinit var mLastLocation: Location
    private var mMarker: Marker? = null
    private lateinit var currentLatLng: LatLng
    private lateinit var destLatLon: LatLng

    //Location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationButton = (mapFragment.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(Integer.parseInt("2"))
        val rlp =  locationButton.getLayoutParams() as RelativeLayout.LayoutParams
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);

        rlp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
        rlp.addRule(RelativeLayout.ALIGN_END, 0);
        rlp.setMargins(100, 0, 0, 150);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest()
                buildLocationCallBack()
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            }
        } else {
            buildLocationRequest()
            buildLocationCallBack()
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    private fun checkLocationPermission(): Boolean {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), myPermissionCode)
            else
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), myPermissionCode)
            return false
        } else
            return true
    }

    private fun buildLocationCallBack() {

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(p0: LocationResult) {
                mLastLocation = p0!!.locations[p0!!.locations.size - 1]

                if (mMarker != null) {
                    mMarker!!.remove()
                }
                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude

                currentLatLng = LatLng(latitude, longitude)
                val markerOptions = MarkerOptions().position(currentLatLng).title("You are here >")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                mMarker = mMap.addMarker(markerOptions)
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(10f))


                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        // on below line we are getting the
                        // location name from search view.
                        val location: String = searchView.getQuery().toString()

                        // below line is to create a list of address
                        // where we will store the list of all address.
                        var addressList: List<Address>? = null

                        // checking if the entered location is null or not.
                        if (location != null || location == "") {
                            // on below line we are creating and initializing a geo coder.
                            val geocoder = Geocoder(this@MapsActivity)
                            try {
                                // on below line we are getting location from the
                                // location name and adding that location to address list.
                                addressList = geocoder.getFromLocationName(location, 1)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            if (addressList?.isEmpty() == true) {
                                Toast.makeText(this@MapsActivity,
                                    "Location Not entered correctly, Please try once again",
                                    Toast.LENGTH_SHORT).show()
                            }
                            // on below line we are getting the location
                            // from our list a first position.
                            val address: Address = addressList!![0]

                            // on below line we are creating a variable for our location
                            // where we will add our locations latitude and longitude.
                            destLatLon = LatLng(address.getLatitude(), address.getLongitude())

                            // on below line we are adding marker to that position.
                            val place2 = MarkerOptions().position(destLatLon).title("Destination")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                            mMarker = mMap.addMarker(place2)
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(destLatLon))
                            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(10f))

                            btnNav.visibility = View.VISIBLE
                            btnClear.visibility = View.VISIBLE
                            bynPoly.visibility = View.VISIBLE
                            btnMap.visibility = View.VISIBLE
                            btnNav.setOnClickListener {
                                mMap.addMarker(MarkerOptions().position(currentLatLng))
                                mMap.addMarker(MarkerOptions().position(destLatLon))
                                if (markerOptions.position != null && place2.position != null) {
                                    FetchURL(this@MapsActivity).execute(getUrl(markerOptions.position, place2.position, "driving"), "driving")
                                }
                                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(10f))
                            }
                            btnClear.setOnClickListener {
                                mMap.clear()
                                val markerOptions = MarkerOptions().position(currentLatLng).title("You are here >")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                mMarker = mMap.addMarker(markerOptions)
                                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(10f))
                            }
                            bynPoly.setOnClickListener {
                                mMap.addMarker(MarkerOptions().position(currentLatLng))
                                mMap.addMarker(MarkerOptions().position(destLatLon))
                                val line: Polyline = mMap.addPolyline(PolylineOptions()
                                    .add(currentLatLng, destLatLon)
                                    .width(5f)
                                    .color(Color.RED))
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14F))
                            }
                            btnMap.setOnClickListener {
                                val gmmIntentUri = Uri.parse("google.navigation:q="+destLatLon.latitude+","+destLatLon.longitude)
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                startActivity(mapIntent)
                            }
                        }
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        btnNav.visibility = View.INVISIBLE
                        btnClear.visibility = View.INVISIBLE
                        btnMap.visibility = View.INVISIBLE
                        bynPoly.visibility = View.INVISIBLE
                        return false
                    }
                })

                /*val place2 = MarkerOptions().position(LatLng(19.366636, 72.816101)).title("Kankaria Lake")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                mMarker = mMap.addMarker(place2)
                //move Camera
                if (markerOptions.position != null && place2.position != null) {
                    FetchURL(this@MapsActivity).execute(getUrl(markerOptions.position, place2.position, "driving"), "driving")
                }
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(10f))*/
            }
        }
    }

    override fun onTaskDone(vararg values: Any) {
        currentPolyline?.remove()
        currentPolyline = mMap!!.addPolyline(values[0] as PolylineOptions)
    }

    private fun getUrl(origin: LatLng, dest: LatLng, directionMode: String): String {
        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        // Mode
        val mode = "mode=$directionMode"
        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$mode"
        // Output format
        val output = "json"
        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key)
    }
    private fun buildLocationRequest() {

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f

    }

    //Override RequestPermission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            myPermissionCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if (checkLocationPermission()) {
                            buildLocationRequest()
                            buildLocationCallBack()
                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
                            mMap!!.isMyLocationEnabled = true
                        }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //Init Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap!!.isMyLocationEnabled = true
            }
        } else
            mMap!!.isMyLocationEnabled = true

        //enable Zoom control
        mMap.uiSettings.isZoomControlsEnabled = true
    }

    override fun onCameraMoveStarted(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onCameraMove() {
        btnNav.visibility = View.INVISIBLE
        btnClear.visibility = View.INVISIBLE
        btnMap.visibility = View.INVISIBLE
        bynPoly.visibility = View.INVISIBLE
    }

    override fun onCameraMoveCanceled() {
        btnNav.visibility = View.VISIBLE
        btnClear.visibility = View.VISIBLE
        bynPoly.visibility = View.VISIBLE
        btnMap.visibility = View.VISIBLE
    }

    override fun onCameraIdle() {
        TODO("Not yet implemented")
    }
}