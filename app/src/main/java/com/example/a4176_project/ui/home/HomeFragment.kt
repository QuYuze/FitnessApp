package com.example.a4176_project.ui.home


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.a4176_project.MainActivity
import com.example.a4176_project.MapAPIResponse
import com.example.a4176_project.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_home.*
import okhttp3.OkHttpClient
import okhttp3.Request

import java.lang.Exception
import com.google.android.gms.maps.SupportMapFragment

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mContext: Context
    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var distance: Double = 500.toDouble()

    private lateinit var mMapView: MapView
    private lateinit var mView: View

    //https://stackoverflow.com/questions/8215308/using-context-in-a-fragment referenced to get context
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(OnMapReadyCallback {
            mMap = it
            val location1 = LatLng(13.0356745, 77.5933021)
            val location2 = LatLng(13.029727, 77.5933021)

            mMap.addMarker(MarkerOptions().position(location1).title("location1"))
            mMap.addMarker(MarkerOptions().position(location2).title("location2"))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location1, 15f))

            val URL = getDirectionURL(location1, location2)
            GetDirection(URL, distance).execute()
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(root.context)
        val button = root.findViewById<FloatingActionButton>(R.id.button)
        button.setOnClickListener {
         locate()
        }
        return root
    }

    fun getDirectionURL(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&key=AIzaSyDU-IenVPIoA8bxKTK4PLdL7bova329WhY&sensor = false&mode = driving"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        MapsInitializer.initialize(context)
        mMap = googleMap
        val location1 = LatLng(13.0356745, 77.5933021)
        val location2 = LatLng(13.029727, 77.5933021)

        mMap.addMarker(MarkerOptions().position(location1).title("location1"))
        mMap.addMarker(MarkerOptions().position(location2).title("location2"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location1, 15f))

        val URL = getDirectionURL(location1, location2)
        GetDirection(URL, distance).execute()
    }

    inner class GetDirection(val URL: String, DIS: Double) :
        AsyncTask<Void, Void, List<List<LatLng>>>() {
        private val dis: Double = DIS
        //User's destination
        private lateinit var Destination: LatLng
        //Bias distance of destionation
        private var bias: Double = 0.toDouble()

        override fun doInBackground(vararg p0: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(URL).build()
            val response = client.newCall(request).execute()
            val data = response.body()!!.string()
            val result = ArrayList<List<LatLng>>()

            try {
                val respObj = Gson().fromJson(data, MapAPIResponse::class.java)

                val path = ArrayList<LatLng>()
                var current: Float = 0.toFloat()
                for (i in 0..(respObj.routes[0].legs[0].steps.size - 1)) {
                    current += respObj.routes[0].legs[0].steps[i].distance.value
                    path.addAll(decodePoly(respObj.routes[0].legs[0].steps[i].polyline.points))
                    //
                    if (current > dis) {
                        bias = current - dis
                        Destination = LatLng(
                            respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble(),
                            respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble()
                        )
                        break
                    }
                }
                result.add(path)
                val lineoption = PolylineOptions()
                for (i in result.indices) {
                    lineoption.addAll(result[i])
                    lineoption.width(10f)
                    lineoption.color(Color.BLUE)
                    lineoption.geodesic(true)
                }
                mMap.addPolyline(lineoption)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices) {
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLUE)
                lineoption.geodesic(true)
            }
            mMap.addPolyline(lineoption)
            mMap.addMarker(MarkerOptions().position(Destination))
        }

    }

    private fun decodePoly(encoded: String): List<LatLng> {
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
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }

    override fun onStart() {
        super.onStart()
        if (!hasLocationPermissions()) {
            //request location on first launch
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 35
            )
        } else {
            //createLocationRequest()
        }
    }

    fun hasLocationPermissions() = checkSelfPermission(
        mContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun locate() {
        if (checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not yet granted, ask the user for permission
            this.requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 35)
            //check the permission again
            if (ContextCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            )
                Toast.makeText(
                    context,
                    "Permission denied, can't get access to location ",
                    Toast.LENGTH_LONG
                ).show()
            else//permission is granted,proceed
                locUpdate()
        } else
            locUpdate()
    }

    private fun locUpdate() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location
                if (location != null) {
                    Log.d("CORD", location.latitude.toString() + "," + location.longitude)
                    val loc = LatLng(location.latitude, location.longitude)
                    //mMap.addMarker(MarkerOptions().position(loc).title("My Location"))
                    //Does not need to show the marker since the button does is to center the map to the current location
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f))
                } else
                    Toast.makeText(
                        this.context,
                        "Failed to get location",
                        Toast.LENGTH_SHORT
                    ).show()
            }.addOnFailureListener {
                Toast.makeText(this.context, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
    }

}