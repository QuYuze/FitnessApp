package com.example.a4176_project.ui.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import kotlin.math.abs

class ShakeDetector(val context: Context) {

    interface ShakeListener {
        fun onShake(force: Float)
    }

    /**
     * Accuracy configuration
     */
    private var threshold = 10.0f
    private var interval = 100

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var listener: ShakeListener? = null

    /**
     * indicates whether or not Accelerometer Sensor is supported
     */
    private var supported: Boolean? = null

    /**
     * Returns true if the manager is listening to orientation changes
     */
    private var isListening = false


    private val sensorEventListener = object : SensorEventListener {

        private var now: Long = 0
        private var timeDiff: Long = 0
        private var lastUpdate: Long = 0
        private var lastShake: Long = 0

        private var x = 0f
        private var y = 0f
        private var z = 0f
        private var lastX = 0f
        private var lastY = 0f
        private var lastZ = 0f
        private var force = 0f

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            // use the event timestamp as reference
            // so the manager precision won't depends
            // on the AccelerometerListener implementation
            // processing time
            now = event.timestamp

            x = event.values[0]
            y = event.values[1]
            z = event.values[2]

            // if not interesting in shake events
            // just remove the whole if then else block
            if (lastUpdate == 0L) {
                lastUpdate = now
                lastShake = now
                lastX = x
                lastY = y
                lastZ = z
                Toast.makeText(context, "No Motion detected", Toast.LENGTH_SHORT).show()

            } else {
                timeDiff = now - lastUpdate

                if (timeDiff > 0) {

                    force = abs(x + y + z - lastX - lastY - lastZ)

                    if (force.compareTo(threshold) > 0) {

                        if (now - lastShake >= interval) {
                            // trigger shake event
                            listener?.onShake(force)
                        } else {
                            Toast.makeText(context, "No Motion detected",
                                    Toast.LENGTH_SHORT).show()

                        }
                        lastShake = now
                    }
                    lastX = x
                    lastY = y
                    lastZ = z
                    lastUpdate = now
                } else {
                    Toast.makeText(context, "No Motion detected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    fun isSupported(): Boolean {
        if (supported == null) {
            // Count accelerometer sensors on device
            supported = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size > 0
        }
        return supported!!
    }

    /**
     * Registers a listener and start listening
     *
     * @param shakeListener callback for shake events
     */
    fun startListening(shakeListener: ShakeListener) {
        // Find first accelerometer
        sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).firstOrNull()?.let { sensor ->
            // Register Accelerometer Listener
            isListening = sensorManager.registerListener(
                sensorEventListener, sensor,
                SensorManager.SENSOR_DELAY_GAME)
            listener = shakeListener
        }
    }

    /**
     * Unregisters listeners
     */
    fun stopListening() {
        isListening = false
        try {
            sensorManager.unregisterListener(sensorEventListener)
        } catch (e: Exception) {
        }
    }

    /**
     * Configure the listener for shaking
     *
     * @param threshold minimum acceleration variation for considering shaking
     * @param interval  minimum interval between to shake events
     */
    fun configure(threshold: Int, interval: Int) {
        this.threshold = threshold.toFloat()
        this.interval = interval
    }

}
