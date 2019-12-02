package com.example.a4176_project.ui.home


import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.example.a4176_project.MapAPIResponse
import com.example.a4176_project.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mContext: Context
    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var distance: Double = 500.toDouble()
    private var return_origin:Boolean = false
    private lateinit var currLoc:LatLng
    private lateinit var Con:Context
    //new content
    private var StepList = mutableListOf<LatLng>()
    private var StepLengthList = mutableListOf<Float>()
    private lateinit var CurrentCheckpoint:LatLng
    private var  ListIndex :Int = 0
    private lateinit var vibrator:Vibrator
    private var Line = PolylineOptions()
    private var MarkerList:HashMap<Int, Marker> = HashMap()
    private var Flag = false

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

        //sharedPreferences = context!!.getSharedPreferences("home", Context.MODE_PRIVATE)
        mapFragment.getMapAsync(OnMapReadyCallback {
            mMap = it
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled= false
            /*
            //load the saved map
            val lat = sharedPreferences.getFloat("LAT",0f)
            val lon = sharedPreferences.getFloat("LON",-63.5863118f)
            val zoom = sharedPreferences.getFloat("ZOOM",14.0f)
            if(lat!=0f)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat.toDouble(),lon.toDouble()), zoom))
            else

             */
                locate()//focus on the user current location by default
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(root.context)
        Con = root.context

        val button = root.findViewById<FloatingActionButton>(R.id.button)
        button.setOnClickListener {
            locate()
        }
        val runButton = root.findViewById<FloatingActionButton>(R.id.button_run)
        runButton.setOnClickListener {
            showDialog()
        }
        while(ListIndex<StepList.size) {
            //test if user is near the path
            var onthepath = OnthePath(currLoc, StepList[ListIndex], StepLengthList[ListIndex])
            if (onthepath == false) {
                vibrator =
                    context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(500)

            }
            //test if user reach the check point
            var arrive = Arrive(currLoc, StepList[ListIndex])
            if (arrive <=30) {
                ListIndex++
                var marker = MarkerList.get(ListIndex)!!
                marker.remove()
                marker = mMap.addMarker(
                    MarkerOptions().position(StepList.get(ListIndex)).icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.checkpointgreen
                        )
                    )
                )
                MarkerList.put(ListIndex, marker)
            }
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        val timerTask = object: TimerTask(){
            override fun run() {
                if(Flag==true && ListIndex<StepList.size) {
                    running().execute()
                }
            }
        }
        var timer = Timer()
        timer.schedule(timerTask,0,5000)
    }
    fun getDirectionURL(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&key=AIzaSyDU-IenVPIoA8bxKTK4PLdL7bova329WhY&sensor = false&mode = driving"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        MapsInitializer.initialize(context)
        mMap = googleMap
    }

    inner class GetDirection(val URL: String, DIS: Double) :
        AsyncTask<Void, Void, List<List<LatLng>>>() {
        private val dis: Double = DIS
        //User's destination
        private var Destination =  LatLng(90.toDouble(),0.toDouble())
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
                StepList.clear()
                StepLengthList.clear()
                ListIndex = 0

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

                    else if(current<=dis && i==respObj.routes[0].legs[0].steps.size-1){
                        Destination = LatLng(
                            respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble(),
                            respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble()
                        )
                    }

                    var checkpoint = LatLng(
                        respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble(),
                        respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble()
                    )
                    StepList.add(checkpoint)
                    StepLengthList.add(respObj.routes[0].legs[0].steps[i].distance.value.toFloat())
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
            Line = lineoption
            mMap.addPolyline(lineoption)

            for(i in 0..StepList.size-2){
                var marker = mMap.addMarker(MarkerOptions().position(StepList.get(i)).icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpointred)))
                MarkerList.put(i,marker)
            }
            if(Destination != LatLng(90.toDouble(),0.toDouble())) {
                var marker = mMap.addMarker(
                    MarkerOptions().position(Destination).icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.ic_dest
                        )
                    )
                )
                MarkerList.put(StepList.size-1,marker)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(Destination, 15f))
                Flag = true
            }

            else{
                Toast.makeText(
                    context,
                    "The random location is unreachable, please re-enter again or try another distance",
                    Toast.LENGTH_LONG
                ).show()
            }
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
            createLocationRequest()
        }
    }

    private fun hasLocationPermissions() = checkSelfPermission(
        mContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    //request user permission
    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 1000
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
                    currLoc = loc
                    //mMap.addMarker(MarkerOptions().position(loc).title("My Location"))
                    //Does not need to show the marker since the button does is to center the map to the current location
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f))
                } else
                    Toast.makeText(
                        this.context,
                        "Failed to get location.Please check if GPS is enabled",
                        Toast.LENGTH_SHORT
                    ).show()
            }.addOnFailureListener {
                Toast.makeText(this.context, "Failed to get last known location", Toast.LENGTH_SHORT).show()
            }
    }
    fun randomGeo(center:LatLng, radius:Double) : LatLng{
        var y0 = center.latitude
        var x0 = center.longitude
        var rd = radius / 111300; //about 111300 meters in one degree

        var u = Math.random();
        var v = Math.random();

        var w = rd * Math.sqrt(u);
        var t = 2 * Math.PI * v;
        var x = w * Math.cos(t);
        var y = w * Math.sin(t);

        //Adjust the x-coordinate for the shrinking of the east-west distances
        var xp = x / Math.cos(y0);

        var newlat = y + y0;
        var newlon = x + x0;

        return LatLng(newlat,newlon)

        //References
        //https://www.youtube.com/watch?v=eiexkzCI8m8
        //https://www.youtube.com/watch?v=urLA8z6-l3k
    }
    fun showDialog() {
        Flag = false
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Set Distance")

        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.run_dialog, null)
        val returnToOrigin = view.findViewById(R.id.checkBox2) as CheckBox
        //val spinner = view.findViewById(R.id.spinner2) as Spinner
        val runDistance = view.findViewById(R.id.runDistance) as EditText

        builder.setView(view)

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            val d = runDistance.text
            if (d.isNotEmpty()) {
                distance = runDistance.text.toString().toDouble()
                if (distance >= 100 && distance <= 100000) {
                    return_origin = returnToOrigin.isChecked
                    locate()
                    mMap.clear()
                    if (return_origin) {
                        distance /= 2
                        val point1 = currLoc
                        val point2 = randomGeo(point1, distance)
                        var URL = getDirectionURL(point1, point2)
                        GetDirection(URL, distance).execute()
                        URL = getDirectionURL(point2, point1)
                        GetDirection(URL, distance).execute()
                        //Flag = true
                    } else {
                        val point1 = currLoc
                        val point2 = randomGeo(point1, distance)
                        //val URL = getDirectionURL(point1, point2)
                        val URL = getDirectionURL(
                            LatLng(44.6403983, -63.5867983),
                            LatLng(44.646777, -63.583474)
                        )
                        GetDirection(URL, distance).execute()
                        //Flag = true
                    }
                } else
                    Toast.makeText(
                        context,
                        "Please enter a distance between 100m and 100km",
                        Toast.LENGTH_SHORT
                    ).show()
                builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
                    dialog.cancel()
                }
            }
            else
                Toast.makeText(
                    context,
                    "Please enter a distance !",
                    Toast.LENGTH_SHORT
                ).show()
        }
        builder.show()
    }
    private fun OnthePath(UserLocation:LatLng,Checkpoint:LatLng,StepLength:Float):Boolean{
        var location1:Location  = android.location.Location("")
        var location2:Location  = android.location.Location("")

        location1.latitude = UserLocation.latitude
        location1.longitude = UserLocation.longitude
        location2.latitude = Checkpoint.latitude
        location2.longitude = Checkpoint.longitude

        var distance:Float = location1.distanceTo(location2)
        if(distance>StepLength){
            return false
        }
        return true
    }

    private fun Arrive(UserLocation:LatLng,Checkpoint:LatLng):Float{
        var location1:Location=android.location.Location("")
        var location2:Location=android.location.Location("")

        location1.latitude = UserLocation.latitude
        location1.longitude = UserLocation.longitude
        location2.latitude = Checkpoint.latitude
        location2.longitude = Checkpoint.longitude

        var dis:Float = location1.distanceTo(location2)

        return dis
    }

    inner class running:AsyncTask<Void,Void,Boolean>(){
        override fun doInBackground(vararg params: Void?): Boolean {
            return Flag
        }
        override fun onPostExecute(result: Boolean?) {
            if(result == true){
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location
                        if (location != null) {
                            Log.d("CORD", location.latitude.toString() + "," + location.longitude)
                            val loc = LatLng(location.latitude, location.longitude)
                            currLoc = loc
                            //Does not need to show the marker since the button does is to center the map to the current location
                        } else
                            Toast.makeText(
                                Con,
                                "Failed to get location.Please check if GPS is enabled",
                                Toast.LENGTH_SHORT
                            ).show()
                    }.addOnFailureListener {
                        Toast.makeText(Con, "Failed to get last known location", Toast.LENGTH_SHORT).show()
                    }
                    //test if user is near the path
                    var onthepath = OnthePath(currLoc, StepList[ListIndex], StepLengthList[ListIndex])
                    //test if user reach the check point
                    var arrive = Arrive(currLoc, StepList[ListIndex])
                    if (onthepath == false) {
                        vibrator =
                            context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        Toast.makeText(Con, ""+arrive.toInt()+"m from next check point", Toast.LENGTH_LONG).show()
                    }

                    if (arrive <=50) {
                        vibrator.vibrate(500)
                        Toast.makeText(Con, "You have reached check point, great job!", Toast.LENGTH_SHORT).show()
                        mMap.addMarker(
                            MarkerOptions().position(StepList.get(ListIndex)).icon(
                                BitmapDescriptorFactory.fromResource(
                                    R.drawable.checkpointgreen
                                )
                            )
                        )
                        //MarkerList.put(ListIndex, marker)
                        ListIndex++
                    }
                if(ListIndex>=StepList.size){
                    Flag = false
                    Toast.makeText(Con, "Your finish your running, congratulation!", Toast.LENGTH_SHORT).show()
                    mMap.clear()
                }
            }
        }
    }
}