package com.example.a4176_project.ui

import android.content.Context
import android.location.Location
import android.os.Vibrator
import com.example.a4176_project.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*

class StartRuning :TimerTask{
    private var mMap:GoogleMap
    private var currLoc:LatLng
    private var StepList = mutableListOf<LatLng>()
    private var StepLengthList = mutableListOf<Float>()
    private var  ListIndex :Int = 0
    private var context:Context?
    private lateinit var vibrator:Vibrator
    private var Line = PolylineOptions()

  constructor(map:GoogleMap,current:LatLng,SL: MutableList<LatLng>,SLL:MutableList<Float>,con:Context?,line:PolylineOptions){
      mMap = map
      currLoc = current
      StepList = SL
      StepLengthList = SLL
      context = con
      Line = line
  }

    override fun run(){
        //boolean method that check if user is still on the path
        var onthepath = OnthePath(currLoc,StepList[ListIndex],StepLengthList[ListIndex])
        if(onthepath==false) {
            vibrator =
                context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(500)

        }

        var arrive = Arrive(currLoc,StepList[ListIndex])
        if(arrive == true){
            ListIndex ++
            mMap.clear()

            mMap.addPolyline(Line)

            for(i in ListIndex..StepList.size-2){
                mMap.addMarker(
                    MarkerOptions().position(StepList.get(i)).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable. checkpointred)))
            }

            for(i in 0..ListIndex){
                mMap.addMarker(
                    MarkerOptions().position(StepList.get(i)).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.checkpointgreen)))
            }
        }
    }

    private fun OnthePath(UserLocation:LatLng,Checkpoint:LatLng,StepLength:Float):Boolean{
        var location1: Location = android.location.Location("")
        var location2: Location = android.location.Location("")

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

    private fun Arrive(UserLocation:LatLng,Checkpoint:LatLng):Boolean{
        var location1: Location =android.location.Location("")
        var location2: Location =android.location.Location("")

        location1.latitude = UserLocation.latitude
        location1.longitude = UserLocation.longitude
        location2.latitude = Checkpoint.latitude
        location2.longitude = Checkpoint.longitude

        var distance:Float = location1.distanceTo(location2)
        if(distance>20.toDouble()){
            return false
        }
        return true
    }
}
