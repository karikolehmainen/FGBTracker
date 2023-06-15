package com.example.fgbtracker.model;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.example.fgbtracker.MainActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

public class VirtualPilot {
    private static final String TAG = VirtualPilot.class.getName();
    //private LocationCoordinate3D currentTarget;
    //private LocationCoordinate3D currentPosition;
    //private LocationCoordinate3D lastPosition;
    private Location currentTarget;
    private Location currentPosition;
    private Location lastPosition;

    private SharedPreferences prefs;
    private int interval = 1000;
    private long lastLocationTS = 0;
    private boolean running = false;
    private FlightController flightController;
    private double targetAlt;
    private float yaw;
    private float throttle;
    private float roll;
    private float pitch;
    //private double rawroll;
    //private double rawpitch;
    private float deltaLatM;
    private float deltaLonM;

    private double droneSpeed;
    private long lastUpdateTS;
    private double speedThreshold = 0.5;
    private double maxSpeed;
    private double headingThreshold = 5.0;
    private PositionObserverTask positionObserverTask;
    private Timer positionObserverTimer;
    private boolean pilotActive;
    private float deltaD;
    private float deltaAlt;
    private float targetDist; // meters
    private SendVirtualStickDataTask sendVirtualStickDataTask;
    private Timer sendVirtualStickDataTimer;

    public VirtualPilot(SharedPreferences prefers, FlightController flightCTRL)
    {
        prefs = prefers;
        interval = prefs.getInt("pref_pilot_interval", 250);
        lastLocationTS = System.currentTimeMillis();
        flightController = flightCTRL;
        targetDist = 999;
        maxSpeed = 5.0;
        double pitch = 0.0;
        double roll = 0.0;

        String filePath = Environment.getExternalStorageDirectory() + "/fgbtracker_virtualpilot.txt";
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, TAG+":V", "*:S"});
            Log.d(TAG,"direct logs to file: "+filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setTarget(LocationCoordinate3D target, double speed)
    //public void setTarget(Location current, Location target, long speed)
    {
        Log.d(TAG, "setTarget lat: "+target.getLatitude()+" lon:"+target.getLongitude());
        lastLocationTS = System.currentTimeMillis();
        if (currentTarget == null)
        {
            currentTarget = new Location("dummyprovider");
        }
        currentTarget.setLatitude(target.getLatitude());
        currentTarget.setLongitude(target.getLongitude());
        droneSpeed = speed;
        //currentPosition = current;
        //targetAlt = target.getAltitude();
        pilotActive = false;
    }
    public void setCurrentPosition(double lat, double lon, double alt)
    {
        Log.d(TAG, "setCurrentPosition lat: "+lat+" lon:"+lon);
        if (currentPosition == null)
            currentPosition = new Location("dummy provider");
        currentPosition.setLatitude(lat);
        currentPosition.setLongitude(lon);
        currentPosition.setAltitude(alt);
    }
    public double getTargetDistance()
    {
        return (double)targetDist;
    }
    public void startPilot()
    {
        Log.d(TAG, "startPilot with interval: "+interval);
        positionObserverTask = new PositionObserverTask();
        positionObserverTimer = new Timer();
        positionObserverTimer.schedule(positionObserverTask, 50, interval);
        pilotActive = true;
        if (null == sendVirtualStickDataTimer) {
            sendVirtualStickDataTask = new SendVirtualStickDataTask();
            sendVirtualStickDataTimer = new Timer();
            sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 100, 200);
        }
    }

    public void stopPilot()
    {
        Log.d(TAG, "stopPilot");
        if (null != sendVirtualStickDataTimer) {
            if (sendVirtualStickDataTask != null) {
                sendVirtualStickDataTask.cancel();
            }
            sendVirtualStickDataTimer.cancel();
            sendVirtualStickDataTimer.purge();
            sendVirtualStickDataTimer = null;
            sendVirtualStickDataTask = null;
        }
        if (positionObserverTimer != null)
            positionObserverTimer.cancel();
        if (positionObserverTask != null)
           positionObserverTask.cancel();
        pilotActive = false;

    }
    public boolean isPilotActive() {
        return pilotActive;
    }

    public Location getCurrentPosition() {
        return currentPosition;
    }

    public float getDeltaDistance()
    {
        return deltaD;
    }

    public float getDeltaAltitude()
    {
        return deltaAlt;
    }
    /*
    private class PositionObserverTask extends TimerTask {
        @Override
        public void run() {
            double pitch = 0.0;
            double roll  = 0.0;
            LocationCoordinate3D droneLoc = flightController.getState().getAircraftLocation();
            lastPosition = currentPosition;
            setCurrentPosition(droneLoc.getLatitude(),droneLoc.getLongitude(),droneLoc.getAltitude());
            if (lastPosition == null)
                lastPosition = currentPosition;
            if (currentTarget != null) {
                long deltaT = System.currentTimeMillis() - lastLocationTS;
                long lastT = System.currentTimeMillis() - lastUpdateTS;
                lastUpdateTS = System.currentTimeMillis();
                double lastDist = lastPosition.distanceTo(currentPosition);
                targetDist = currentPosition.distanceTo(currentTarget);
                double lastSpeed = lastDist / (lastT / 1000); // m/s
                deltaD = currentPosition.distanceTo(currentTarget);
                double targetSpeed = deltaD / (deltaT / 1000); // m/s
                if (targetSpeed > maxSpeed)
                    targetSpeed = maxSpeed;
                double deltaV = targetSpeed - lastSpeed;
                double bearing_last = lastPosition.bearingTo(currentPosition);
                double bearing_target = currentPosition.bearingTo(currentTarget);
                float altMargin = 0.5f;
                deltaAlt = (float) (targetAlt - currentPosition.getAltitude());
                Log.d(TAG, "PositionObserverTask deltaT:" + deltaT + " deltaD:" + deltaD + " deltaAlt:"+deltaAlt+" deltaV:"+deltaV+" lastDist:" + lastDist + " lastSpeed:" + lastSpeed + " targetDist:" + targetDist + " targetSpeed:" + targetSpeed);

                if (deltaAlt - altMargin > 0)
                    throttle = 2;
                else if (deltaAlt + altMargin < 0)
                    throttle = (float) -0.5;
                else
                    throttle = 0;
                if (Math.abs(roll)>targetSpeed)
                    if (roll < 0)
                        roll = (0-targetSpeed);
                    else
                        roll = targetSpeed;
                pitch = Math.sqrt(targetSpeed*targetSpeed-roll*roll);
                if (Math.abs(pitch)>targetSpeed)
                    if (pitch < 0)
                        pitch = (0-targetSpeed);
                    else
                        pitch = targetSpeed;
                roll = Math.sqrt(targetSpeed*targetSpeed-roll*roll);
                //pitch = (float) ((float) pitch+(deltaV / targetSpeed));
                //roll = (float) ((float) pitch+(deltaV / targetSpeed));
                pitch = (pitch*(targetSpeed/lastSpeed));
                roll = (roll*(targetSpeed/lastSpeed));
                //double tmp_speed = Math.sqrt(roll*roll+pitch*pitch);

                if (Math.abs(pitch)>maxSpeed)
                    if (pitch < 0)
                        pitch = (0-maxSpeed);
                    else
                        pitch = maxSpeed;
                if (Math.abs(roll)>maxSpeed)
                    if (roll < 0)
                        roll = (0-maxSpeed);
                    else
                        roll = maxSpeed;

                // Control heading
                float heading = flightController.getCompass().getHeading();
                float yawSpeed = Float.parseFloat(prefs.getString("pref_flight_anglespeed", "3"));
                if (heading < 0)
                    yaw = yawSpeed;
                else if (heading > 0)
                    yaw = -1 * yawSpeed;
                else
                    yaw = 0;
                //Log.d(TAG, "PositionObserverTask: pitch:"+pitch+" roll:"+roll+" yaw:"+yaw);
                // Control position
                // check first direction Location bearintTo returns direction easto from north in degrees
                double diff = bearing_target - bearing_last;
                double rawroll = roll;
                double rawpitch = pitch;
                /*if (diff < 0)
                    diff = diff + 360;
                if (diff > 180)
                    diff = 360 - diff;
                if (diff > headingThreshold) // do direction correction
                {
                    // use rotational vector to modify the roll and pitch values
                    //  | roll'  | _ | cos(diff) -sin(diff) | | roll  |
                    //  | pitch' | - | sin(diff)  cos(diff) | | pitch |

                    if (bearing_target > bearing_last) // actual heading is left from the target CCW Rotation
                    {
                        diff = 0 - diff;
                    }
                    float[] vector = {roll, pitch};
                    float[] rotatedVector = rotateVector(vector, (float) diff);
                    roll = vector[0];
                    pitch = vector[1];
                }*/
    /*

                if (flightController != null) {
                    Log.d(TAG, "PositionObserverTask roll:" + roll + "rawroll:" + rawroll + " pitch:" + pitch + "rawpitch:" + rawpitch + " heading:"+heading+" yaw:" + yaw + " throttle:" + throttle);
                    flightController.sendVirtualStickFlightControlData(new FlightControlData((float)roll, (float)pitch, yaw, throttle), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            }
                        }
                    });
                }
            } // if current target is null
            else
            {
                Log.e(TAG, "PositionObserverTask no target");
            }
        } // run
    }
    */
    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (flightController != null) {
                flightController.sendVirtualStickFlightControlData(new FlightControlData(roll, pitch, yaw, throttle), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.d(TAG, "SendVirtualStickDataTask:"+ djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private class PositionObserverTask extends TimerTask {
        private LocationCoordinate3D mDroneLocation;

        @Override
        public void run() {
            //Location pointLoc = getLastBestLocation();
            LocationCoordinate3D droneLoc = flightController.getState().getAircraftLocation();
            // Control Altitude
            mDroneLocation = droneLoc;
            setCurrentPosition(droneLoc.getLatitude(),droneLoc.getLongitude(),droneLoc.getAltitude());
            //updateMapDrone(droneLoc);
            //if(mTOButton == null)
            //    setUpGUIComponents();

            float altMargin = 0.5f;
            float deltaAlt = (float) (targetAlt - droneLoc.getAltitude());
            if (deltaAlt-altMargin > 0)
                throttle = 2;
            else if (deltaAlt+altMargin < 0)
                throttle = (float) -0.5;
            else
                throttle = 0;

            // Control heading
            float heading = flightController.getCompass().getHeading();
            float yawSpeed = Float.parseFloat(prefs.getString("pref_flight_anglespeed","1"));
            if (heading < 0)
                yaw = yawSpeed;
            else if (heading > 0)
                yaw = -1 * yawSpeed;
            else
                yaw = 0;

            // Control position
            pitch = 0;
            roll = 0;
            deltaLatM = 0f;
            deltaLonM = 0f;
            if(currentTarget != null) {
                float deltaLat = (float) (currentTarget.getLatitude() - droneLoc.getLatitude());
                float deltaLon = (float) (currentTarget.getLongitude() - droneLoc.getLongitude());
                // Convert to meters:
                deltaLatM = deltaLat * 111540; // Coords to meters
                deltaLonM = deltaLon * 111540; // Coords to meters and  then accont for latitude multiplier
                deltaLonM = (float) (deltaLonM*Math.cos(Math.toRadians(currentTarget.getLatitude())));
                determineStickPosition(deltaLatM, deltaLonM);

            }
        }
    }

    private void determineStickPosition(float deltaLatM, float deltaLonM) {

        if (deltaLatM > 0) // move south
        {
            if (deltaLatM > maxSpeed)
                deltaLatM = (float) maxSpeed;
        }
        else if (deltaLatM < 0) // move north
            if (deltaLatM < -1f * maxSpeed)
                deltaLatM = (float) (-1f * maxSpeed);
        pitch = deltaLatM;

        if (deltaLonM > 0) // move west
        {
            if (deltaLonM > maxSpeed)
                deltaLonM = (float) maxSpeed;
        }
        else if (deltaLonM < 0) // move east
            if (deltaLonM < maxSpeed)
                deltaLonM = (float) (-1f * maxSpeed);
        roll = deltaLonM;
    }


    public void setTargetAlt(double alt)
    {
        targetAlt = alt;
    }
    // Algorithm by Chat GPT
    private float[][] multiplyMatrix(float[][] matrix1, float[][] matrix2) {
        int rows1 = matrix1.length;
        int cols1 = matrix1[0].length;
        int cols2 = matrix2[0].length;

        float[][] result = new float[rows1][cols2];

        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < cols2; j++) {
                for (int k = 0; k < cols1; k++) {
                    result[i][j] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }

        return result;
    }
    // Algorithm by Chat GPT
    private float[] rotateVector(float[] vector, float angleDegrees) {
        // Convert the angle to radians
        float angleRadians = (float) Math.toRadians(angleDegrees);

        // Create the rotational matrix
        float[][] rotationMatrix = {
                { (float) Math.cos(angleRadians), (float) -Math.sin(angleRadians) },
                { (float) Math.sin(angleRadians), (float) Math.cos(angleRadians) }
        };

        // Convert the vector to a matrix (2x1 matrix)
        float[][] vectorMatrix = {
                { vector[0] },
                { vector[1] }
        };

        // Perform the matrix multiplication
        float[][] resultMatrix = multiplyMatrix(rotationMatrix, vectorMatrix);

        // Convert the resulting matrix back to a vector
        float[] resultVector = { resultMatrix[0][0], resultMatrix[1][0] };

        return resultVector;
    }
}
