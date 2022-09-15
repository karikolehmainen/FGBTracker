package com.example.fgbtracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.followme.FollowMeMissionEvent;
import dji.common.mission.followme.FollowMeMissionState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.followme.FollowMeMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;
import dji.thirdparty.io.reactivex.Observable;
import dji.thirdparty.io.reactivex.schedulers.Schedulers;
import dji.thirdparty.org.reactivestreams.Subscription;
import fgbtracker.R;
import fgbtracker.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private FlightController flightController;
    private FollowMeMissionOperator followmeOperator;
    private Handler mHandler;
    private String serialNumber;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Subscription timerSubcription;
    private Observable<Long> timer =Observable.timer(100, TimeUnit.MILLISECONDS).observeOn(Schedulers.computation()).repeat();

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private TextView mHeadingText;
    public TextView mLocationText;
    private LocationManager locationManager;
    private Button mFMButton;
    private TextView mFollowpointText;
    private TextView mDeltaText;
    private Button mTOButton;
    private Button mSFMButton;

    private LocationCoordinate3D mDroneLocation;
    private Location mFollowLocation;

    private float mDroneHeading;
    private double homeLatitude;
    private FlightMode flightState;
    private double homeLongitude;
    private FollowMeMissionOperatorListener listener;
    private float roll;
    private float pitch;
    private float yaw;
    private float throttle;
    private boolean isGPS = false;
    private Timer sendVirtualStickDataTimer;
    private SendVirtualStickDataTask sendVirtualStickDataTask;
    private Timer positionObserverTimer;
    private PositionObserverTask positionObserverTask;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;

    private float deltaLatM;
    private float deltaLonM;

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);

        } else {
            if (true) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                    if (location != null) {
                        mFollowLocation = location;
                    } else {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1 * 1000); // 10 seconds
        locationRequest.setFastestInterval(1 * 1000); // 5 seconds

        new GpsUtils(this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });
        // Debuglogs to file
        String filePath = Environment.getExternalStorageDirectory() + "/logcat.txt";
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, TAG+":V", "*:S"});
            Log.d(TAG,"direct logs to file: "+filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        /*
        UI Elements
         */
        mHeadingText = findViewById(R.id.text_heading);
        mLocationText = findViewById(R.id.text_location);
        mFollowpointText = findViewById(R.id.text_followpoint);
        mDeltaText = findViewById(R.id.text_delta);
        mFMButton = findViewById(R.id.followme_btn);
        mFMButton.setOnClickListener(this);
        mTOButton = findViewById(R.id.takeoff_btn);
        mTOButton.setOnClickListener(this);
        mSFMButton = findViewById(R.id.stopfollowme_btn);
        mSFMButton.setOnClickListener(this);
        Log.d(TAG, "onCreate create Init DJI SDK Manager");
        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "onCreate ready");

        // Location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    mFollowLocation = location;
                    mFollowpointText.setText(String.format("%,.4f",mFollowLocation.getLatitude())+","+String.format("%,.4f",mFollowLocation.getLongitude()));
                }
            }
        };
        getLocation();
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    
    private void startSDKRegistration() {
        Log.d(TAG, "startSDKRegistration - START");
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                showToast("Register Success");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                showToast("Register sdk fails, please check the bundle id and network connection!");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("Product Disconnected");
                            stopFollowMeMissionVS();
                            notifyStatusChange();
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct.getModel().getDisplayName()));
                            showToast("Product Connected");
                            mProduct = baseProduct;
                            initFC();
                            getRealTimeData();
                            notifyStatusChange();
                            setupVirtualController();
                            //setupFollowMeMission();
                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {
                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                        notifyStatusChange();
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }

                        @Override
                        public void onProductChanged(BaseProduct baseProduct) {
                            mProduct = baseProduct;
                            initFC();
                            Log.d(TAG, "onProductChanged: " + mProduct.getModel().getDisplayName());
                            if (mProduct == null) {
                                showToast("No DJI drone detected");
                                onDroneDisconnected();
                            } else {
                                if (mProduct instanceof Aircraft) {
                                    showToast("DJI aircraft detected");
                                    onDroneConnected();
                                } else {
                                    showToast("DJI non-aircraft product detected");
                                    onDroneDisconnected();
                                }
                            }
                            notifyStatusChange();
                        }
                    });
                }
            });
        }
        Log.d(TAG, "startSDKRegistration - END");
    } //startSDKRegistration

    private void onDroneConnected() {

        if (mProduct.getModel() == null) {
            showToast("Aircraft is not on!");
            return;
        } else {
            showToast("Aircraft : " + mProduct.getModel().getDisplayName());
        }

        if (mProduct.getBattery() == null) {
            showToast("Reconnect your android device to the RC for full functionality.");
            return;
        }

        //sendDroneConnected();
        final Drawable connectedDrawable = getResources().getDrawable(R.drawable.ic_baseline_connected_24px, null);
    }

    private void onDroneDisconnected() {
        final Drawable disconnectedDrawable = getResources().getDrawable(R.drawable.ic_outline_disconnected_24px, null);
        showToast("Drone disconnected");

    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    private void setupVirtualController() {
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                flightController.setVirtualStickAdvancedModeEnabled(true);
                if (djiError != null)
                    Log.d(TAG, djiError.getDescription());
                else
                    Log.d(TAG, "Virtual Stick enabled");
            }
        });
        if (flightController.isVirtualStickControlModeAvailable())
        {
            showToast("Virtual stick IS available");
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        }
        else
        {
            showToast("Virtual stick IS NOT available");
        }
    }
    private void showToast(final String toastMsg) {
        Log.d(TAG, "toast: " + toastMsg);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });

    }
    private void stopFollowMeMission() {
        followmeOperator.stopMission(djiError -> {
            if (djiError != null)
                showToast("stopFollowMeMission: "+djiError.getDescription()+ " ("+djiError.getErrorCode()+")");
        });
    }
    private void stopFollowMeMissionVS() {
        if (null != sendVirtualStickDataTimer) {
            if (sendVirtualStickDataTask != null) {
                sendVirtualStickDataTask.cancel();
            }
            sendVirtualStickDataTimer.cancel();
            sendVirtualStickDataTimer.purge();
            sendVirtualStickDataTimer = null;
            sendVirtualStickDataTask = null;
        }
        if (null != positionObserverTimer) {
            if (positionObserverTask != null) {
                positionObserverTask.cancel();
            }
            positionObserverTimer.cancel();
            positionObserverTimer.purge();
            positionObserverTimer = null;
            positionObserverTask = null;
        }
    }
    private void startFollowMeMission() {
        Log.d(TAG, "startFollowMeMission: " + mFollowLocation.toString());
        mFollowpointText.setText(String.format("%,.4f",mFollowLocation.getLatitude())+","+String.format("%,.4f",mFollowLocation.getLongitude()));
        if (followmeOperator != null) {
            if (followmeOperator.getCurrentState().toString().equals(FollowMeMissionState.READY_TO_EXECUTE.toString())) {
                showToast("starting FM mission");
                FollowMeMission mission = new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,mFollowLocation.getLatitude(),mFollowLocation.getLongitude(), (float) 30);
                LocationCoordinate2D newLocation = new LocationCoordinate2D(mFollowLocation.getLatitude(),mFollowLocation.getLongitude());
                followmeOperator.updateFollowingTarget(newLocation, djiError1 -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (djiError1 != null)
                        showToast("error while following: "+djiError1.getDescription()+" ("+ djiError1.getErrorCode()+")");
                    else
                        Log.d(TAG,"successfully moving to next position");
                });
                followmeOperator.startMission(mission, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        showToast("Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                        if (djiError != null)
                            Log.d(TAG, "Mission Start failed: "+djiError.getDescription()+" ("+djiError.getErrorCode()+")");
                        else
                        {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    int cnt = 0;
                                    //Location loc = getLastBestLocation();
                                    while(cnt < 100) {
                                        //loc = getLastBestLocation();
                                        Log.d(TAG,"lat: "+ mFollowLocation.getLatitude() + " lon: " + mFollowLocation.getLongitude());
                                        mFollowpointText.setText(String.format("%,.4f",mFollowLocation.getLatitude())+","+String.format("%,.4f",mFollowLocation.getLongitude()));
                                        LocationCoordinate2D newLocation = new LocationCoordinate2D(mFollowLocation.getLatitude(),mFollowLocation.getLongitude());
                                        followmeOperator.updateFollowingTarget(newLocation, djiError1 -> {
                                            try {
                                                Thread.sleep(1500);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            if (djiError1 != null)
                                                showToast("error while following: "+djiError1.getDescription()+" ("+ djiError1.getErrorCode()+")");
                                            else
                                                Log.d(TAG,"successfully moving to next position");
                                        });
                                        cnt++;
                                    }
                                }
                            }).start();
                        }
                    }});
            }
        }
    }

    private void setUpListener() {
        // Example of Listener
        listener = new FollowMeMissionOperatorListener() {

            @Override
            public void onExecutionUpdate(@NonNull @NotNull FollowMeMissionEvent followMeMissionEvent) {
                // Example of Execution Listener
                Log.d("TAG",
                        (followMeMissionEvent.getPreviousState() == null
                                ? ""
                                : followMeMissionEvent.getPreviousState().getName())
                                + ", "
                                + followMeMissionEvent.getCurrentState().getName()
                                + ", "
                                + followMeMissionEvent.getDistanceToTarget()
                                + ", "
                                + followMeMissionEvent.getError().getDescription());
                updateFollowMeMissionState();
            }

            @Override
            public void onExecutionStart() {
                showToast("Mission started");
                updateFollowMeMissionState();
            }

            @Override
            public void onExecutionFinish(@Nullable @org.jetbrains.annotations.Nullable DJIError djiError) {
                showToast("Mission finished");
                updateFollowMeMissionState();
            }
        };
    }
    
    private void updateFollowMeMissionState() {
        if (followmeOperator != null && followmeOperator.getCurrentState() != null) {
            String msg = String.format("H Coords: (%.03f.4,%.03f.4) State %s M_State %s T Coords(%.03f.4,%.03f.4)", homeLatitude, homeLongitude, flightState.name(), followmeOperator.getCurrentState().getName(), followmeOperator.getFollowingTarget().getLatitude(), followmeOperator.getFollowingTarget().getLongitude());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLocationText != null)
                        mLocationText.setText(msg);
                }
            });

            Log.d(TAG, msg);
        } else {
            String msg = String.format("H Coords: (%.03f.4,%.03f.4) State %s", homeLatitude, homeLongitude, flightState.name());
            //if (mLocationText != null)
            //    mLocationText.setText(msg);
            Log.d(TAG, msg);
        }
    }
/*
    private Location getLastBestLocation() {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location returnLoc = null;
            long GPSLocationTime = 0;
            if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

            long NetLocationTime = 0;

            if (null != locationNet) {
                NetLocationTime = locationNet.getTime();
            }

            if ( 0 < GPSLocationTime - NetLocationTime ) {
                Log.d(TAG,"getLastBestLocation(GPS):"+locationGPS.getLatitude()+","+locationGPS.getLongitude());
                returnLoc = locationGPS;
            }
            else {
                Log.d(TAG,"getLastBestLocation(NET):"+locationNet.getLatitude()+","+locationNet.getLongitude());
                returnLoc =  locationNet;
            }
            mFollowLocation = returnLoc;
            mFollowLocationTS = System.currentTimeMillis();
            return returnLoc;
    }
 */
    private void setupFollowMeMission() {
        followmeOperator = DJISDKManager.getInstance().getMissionControl().getFollowMeMissionOperator();
        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    homeLatitude = flightControllerState.getHomeLocation().getLatitude();
                    latitude = flightControllerState.getHomeLocation().getLatitude();
                    homeLongitude = flightControllerState.getHomeLocation().getLongitude();
                    longitude = flightControllerState.getHomeLocation().getLongitude();
                    flightState = flightControllerState.getFlightMode();

                    if (flightControllerState.isLandingConfirmationNeeded()) {
                        flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                showToast(djiError == null ? "confirmLanding OK" : djiError.getDescription());
                            }
                        });
                    }

                    updateFollowMeMissionState();
                }
            });
        }
        setUpListener();
    }
    private void initFC() {
        if (mProduct == null) {
            flightController = null;
            showToast(getResources().getString(R.string.playback_disconnected));
        } else {
            Aircraft aircraft = (Aircraft) mProduct;
            if (null != aircraft.getFlightController()) {
                flightController = aircraft.getFlightController();
                flightController.getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                    @Override
                    public void onSuccess(String s) {
                        serialNumber = s;
                    }
                    @Override
                    public void onFailure(DJIError djiError) {
                        showToast("getSerialNumber failed: " + djiError.getDescription());
                    }
                });
                flightController.setStateCallback(new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState state){
                           //Log.d(TAG,"FlightControllerState.Callback onUpdate: " + flightController.getCompass().getHeading());
                            mDroneHeading = flightController.getCompass().getHeading();
                            mDroneLocation = state.getAircraftLocation();
                            droneLocationTS = System.currentTimeMillis();

                            if (mHeadingText != null)
                                mHeadingText.setText(String.format("%,03.1f°",mDroneHeading));
                            if (mLocationText != null)
                                mLocationText.setText(String.format("%,.4f", mDroneLocation.getLatitude())+","+String.format("%,.4f", mDroneLocation.getLongitude())+","+String.format("%,.1f", mDroneLocation.getAltitude()));
                     }
                });
            }
        }
        //setupFollowMeMission();
    }

    // gets real time flight data of the aircraft with the given serial number
    private void getRealTimeData() {
        final ArrayList<String> serialNumbers = new ArrayList<>(1);
        serialNumbers.add(serialNumber);
        Log.d(TAG,"Compass heading: " +flightController.getCompass().getHeading());
        Log.d(TAG,"aircraftLocation: " +flightController.getState().getAircraftLocation());
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG,"onClick"+ v.toString());
        if (v.getId() == mFMButton.getId()) {
            Log.d(TAG,"onClick Follow Me pressed");
            //startFollowMeMission();
            startFollowMeMissionVS();
        }
        if (v.getId() == mSFMButton.getId()) {
            Log.d(TAG,"onClick Stop Follow Me pressed");
            //stopFollowMeMission();
            stopFollowMeMissionVS();
        }
        if (v.getId() == mTOButton.getId()) {
            Log.d(TAG,"onClick Take Off pressed");
            takeOff();
        }
        
    }

    private void startFollowMeMissionVS() {
        if (null == positionObserverTimer) {
            positionObserverTask = new PositionObserverTask();
            positionObserverTimer = new Timer();
            positionObserverTimer.schedule(positionObserverTask, 50, 200);
        }

        if (null == sendVirtualStickDataTimer) {
            sendVirtualStickDataTask = new SendVirtualStickDataTask();
            sendVirtualStickDataTimer = new Timer();
            sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 100, 200);
        }
    }

    private void takeOff() {
        showToast("ready to start FM mission taking off");
        flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showToast("Take OFF Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                if (djiError != null)
                    Log.d(TAG, "Take OFF Start: "+djiError.getDescription()+" ("+djiError.getErrorCode()+")");
            }});
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (flightController != null) {
                flightController.sendVirtualStickFlightControlData(new FlightControlData(roll, pitch, yaw, throttle), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private class PositionObserverTask extends TimerTask {
        @Override
        public void run() {
            //Location pointLoc = getLastBestLocation();
            LocationCoordinate3D droneLoc = flightController.getState().getAircraftLocation();
            // Control Altitude
            float targetAlt = 30;
            float altMargin = 0.5f;
            float deltaAlt = targetAlt - droneLoc.getAltitude();
            if (deltaAlt-altMargin > 0)
                throttle = 2;
            else if (deltaAlt+altMargin < 0)
                throttle = (float) -0.5;
            else
                throttle = 0;

            // Control heading
            float heading = flightController.getCompass().getHeading();
            if (heading < 0)
                yaw = 2f;
            else if (heading > 0)
                yaw = -2f;
            else
                yaw = 0;

            // Control position
            pitch = 0;
            roll = 0;
            deltaLatM = 0f;
            deltaLonM = 0f;
            if(mFollowLocation != null) {
                float deltaLat = (float) (mFollowLocation.getLatitude() - droneLoc.getLatitude());
                float deltaLon = (float) (mFollowLocation.getLongitude() - droneLoc.getLongitude());
                // Convert to meters:
                deltaLatM = deltaLat * 111540; // Coords to meters
                deltaLonM = deltaLon * 111540; // Coords to meters
                if (deltaLatM + 2 > 0) // move south
                    pitch = 1f;
                else if (deltaLatM - 2 < 0) // move north
                    pitch = -1f;
                if (deltaLonM - 2 > 0) // move west
                    roll = 1f;
                else if (deltaLonM + 2 < 0) // move east
                    roll = -1f;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String msg = String.format("d_heading: %.1f d_ele: %.1f d_lat: %.1f d_lon: %.1f", heading, deltaAlt, deltaLatM, deltaLonM);
                    Log.d(TAG, msg);
                    mDeltaText.setText(msg);
                }
            });
        }
    }
}