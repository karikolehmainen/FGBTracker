package com.example.fgbtracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fgbtracker.ui.home.HomeFragment;
import com.example.fgbtracker.ui.maps.MapsFragment;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.followme.FollowMeMissionEvent;
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

    public static boolean FLAG_PREFS_CHANGED = false;
    public static boolean FLAG_TELEMETRY_ADDRESS_CHANGED = false;
    public static boolean FLAG_VIDEO_ADDRESS_CHANGED = false;
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
    private final List<String> missingPermission = new ArrayList<>();
    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Subscription timerSubcription;
    private final Observable<Long> timer =Observable.timer(100, TimeUnit.MILLISECONDS).observeOn(Schedulers.computation()).repeat();

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private TextView mBatteryText;
    public TextView mLocationText;
    private LocationManager locationManager;
    private Button mFMButton;
    private TextView mDeltaText;
    private TextView mAltitudeText;
    private TextView mCameraRecording;
    private TextView mRecordingText;
    private Button mTOButton;
    private Button mSFMButton;
    private Button mLandButton;

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
    private SharedPreferences prefs;

    private float deltaLatM;
    private float deltaLonM;

    public static boolean FLAG_MQTT_HOST_CHANGED = false;
    public static boolean FLAG_MQTT_TOPIC_CHANGED = false;
    public MQTTClient mMQTTclient;
    private float targetAlt;
    private float maxHorizontalSpeed;
    private MapsActivity mMapsActivity;
    private HomeFragment mHomeFragemnt;
    private BatteryState.Callback mBatteryCallback;


    private Handler mMQTTHandler;


    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, AppConstants.LOCATION_REQUEST);

        } else {
            if (true) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                    if (location != null) {
                        mFollowLocation.setLatitude(location.getLatitude());
                        mFollowLocation.setLongitude(location.getLongitude());

                        // for debugging purposes
                        //sendControllerLocation(mFollowLocation);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        // Debuglogs to file
        /*
        String filePath = Environment.getExternalStorageDirectory() + "/logcat.txt";
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, TAG+":V", "*:S"});
            Log.d(TAG,"direct logs to file: "+filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_maps, R.id.navigation_video, R.id.navigation_settings).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }
        Fragment mapsFragment = getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapsFragment != null && mapsFragment.getActivity() instanceof MapsActivity)
            mMapsActivity = (MapsActivity) mapsFragment.getActivity();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        /*
        UI Elements
         */
        mBatteryText = findViewById(R.id.text_battery);
        mRecordingText = findViewById(R.id.text_recording_home);
        mDeltaText = findViewById(R.id.text_delta);
        mAltitudeText = findViewById(R.id.text_altitude);
        mCameraRecording = findViewById(R.id.text_recording);
        mRecordingText = findViewById(R.id.text_recording_home);
        mFMButton = findViewById(R.id.followme_btn);
        mFMButton.setOnClickListener(this);
        mTOButton = findViewById(R.id.takeoff_btn);
        mTOButton.setOnClickListener(this);
        mSFMButton = findViewById(R.id.stopfollowme_btn);
        mSFMButton.setOnClickListener(this);
        mLandButton = findViewById(R.id.land_btn);
        mLandButton.setOnClickListener(this);
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
                    if(!prefs.getBoolean("pref_mqtt_enabled", false)) {
                        mFollowLocation = location;
                        updateMapPoint(mFollowLocation);
                        getLocation();
                        //mDeltaText.setText(String.format("%,.4f", mFollowLocation.getLatitude()) + "," + String.format("%,.4f", mFollowLocation.getLongitude()));
                    }
                    // debug
                    //mFollowLocation = location;
                    //updateMapPoint(mFollowLocation);
                    //getLocation();
                    //sendControllerLocation(mFollowLocation);
                }
            }
        };
        getLocation();

        // MQTT Client
        if(prefs.getBoolean("pref_mqtt_enabled", true)) {
            Log.d(TAG, "MQTT Enabled-> connecting");
            String hostURL = prefs.getString("pref_mqtt_hosturl", "tcp://flexigrobots.collab-cloud.eu:1883");
            String subsTopic = prefs.getString("pref_mqtt_topicdef", "/54321/drone01/#");
            Log.d(TAG, "MQTT host: "+hostURL+" topic: "+ subsTopic);
            mMQTTclient = new MQTTClient(this, hostURL , "dronetracker");
            mMQTTclient.setTopic(subsTopic);
            //mMQTTclient.subscribeMqttChannel(subsTopic);
            mMQTTclient.connect();
            Log.d(TAG, "MQTT Enabled - connected");
        }
        // MQTT Handler
        mMQTTHandler = new Handler();


        // Video feed
        //LiveStreamManager.LiveStreamVideoSource source = LiveStreamManager.LiveStreamVideoSource.Primary;

        //DJISDKManager.getInstance().getLiveStreamManager().

        // Create callbacks
        mBatteryCallback = new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBatteryText.setText(String.format("%d", batteryState.getChargeRemainingInPercent()));
                    }
                });
            }
        };
    }

    private void setupCamera()
    {
        // Set Camera to record video
        mProduct.getCamera().setFlatMode(SettingsDefinitions.FlatCameraMode.VIDEO_NORMAL, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null)
                    Log.d(TAG, djiError.getDescription());
            }
        });
    }
    private void updateMapDrone(LocationCoordinate3D drone) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "updateMapDrone1 " + drone.getLatitude() + "," + drone.getLongitude());
                FragmentContainerView temp = findViewById(R.id.situmap);
                if (temp != null) {
                    ((MapsFragment) temp.getFragment()).updateDroneMarker(drone);
                    Log.d(TAG, "updateMapDrone2 " + temp.getContext().getClass().getName());
                }
            }
        });
    }
    private void updateMapPoint(Location robot) {
        Log.d(TAG, "updateMapPoint0 " + robot.getLatitude() + "," + robot.getLongitude());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "updateMapPoint1 " + robot.getLatitude() + "," + robot.getLongitude());
                FragmentContainerView temp = findViewById(R.id.situmap);
                if (temp != null) {
                    ((MapsFragment) temp.getFragment()).updateRobotMarker(robot);
                    Log.d(TAG, "updateMapPoint2 " + temp.getContext().getClass().getName());
                }
            }
        });
    }
    // Method for reinitiating MQTT Client and connection
    private void updateMQTTConnection() {
        if(prefs.getBoolean("pref_mqtt_enabled", false)) {
            this.mMQTTclient.disconnect();
            this.mMQTTclient = null;
            this.mMQTTclient = new MQTTClient(this, prefs.getString("pref_mqtt_hosturl", "tcp://mqtt.example.com:1883"), "rosetta");
            this.mMQTTclient.setTopic(prefs.getString("pref_mqtt_topicdef", "rosetta"));
            this.mMQTTclient.connect();
            FLAG_MQTT_HOST_CHANGED = false;
            FLAG_MQTT_TOPIC_CHANGED = false;
        }
    }

    public void parseMqttMessage(String topic, String msg) {
        Log.d(TAG, "receoived MQTT message: "+ msg);
        if(prefs.getBoolean("pref_mqtt_enabled", false)) {
            String[] topicElems = topic.split("/");
            for (int k = 0; k < topicElems.length; k++) {
                Log.d(TAG, "topic element: "+ topicElems[k]);
                if (topicElems[k].equals("status")) {
                    String[] elements = msg.split("\\|");
                    double lat = 0;
                    double lon = 0;
                    for (int i = 0; i < elements.length; i++) {
                        Log.d(TAG, "status element: "+ elements[i]);
                        if (elements[i].equals("lat"))
                            lat = Double.parseDouble(elements[i + 1]);
                        if (elements[i].equals("lon"))
                            lon = Double.parseDouble(elements[i + 1]);
                    }
                    Log.d(TAG, "new lat/lon: "+lat+","+lon);
                    //if (lat != 0.0 && lon != 0.0) {
                    double finalLat = lat;
                    double finalLon = lon;

                    mMQTTHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mFollowLocation == null)
                                mFollowLocation = new Location("dummyprovider");
                            mFollowLocation.setLatitude(finalLat);
                            mFollowLocation.setLongitude(finalLon);
                            Log.d(TAG,"parseMqttMessage call updateMapPoint");
                            updateMapPoint(mFollowLocation);
                        }
                    });

                }
            }
        }
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
                            setupCamera();
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

    private final Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

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

    private void startFollowMeMissionVS() {

        targetAlt =  Float.parseFloat(prefs.getString("pref_flight_alt","40.0"));
        maxHorizontalSpeed = Float.parseFloat(prefs.getString("pref_flight_speed","5"));
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
        setGimbalPitch(-90f);
        startRecording();
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
        flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                flightController.setVirtualStickAdvancedModeEnabled(false);
                if (djiError != null)
                    Log.d(TAG, djiError.getDescription());
                else
                    Log.d(TAG, "Virtual Stick disabled");
            }
        });
        stopRecording();
    }

    private void setGimbalPitch(float angle) {
        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
        builder.pitch(angle);
        mProduct.getGimbal().rotate(builder.build(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showToast("Set Gimbal to -90 deg: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                if (djiError != null)
                    Log.d(TAG, "Set Gimbal to -90 deg: "+djiError.getDescription()+" ("+djiError.getErrorCode()+")");
            }
        });
    }

    private void startRecording() {
        mProduct.getCamera().startRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showToast("start recording: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                if (djiError != null)
                    Log.d(TAG, "start recording: "+djiError.getDescription()+" ("+djiError.getErrorCode()+")");
                else {
                    if (mCameraRecording != null)
                        mCameraRecording.setText("REC");
                    if(mRecordingText != null)
                        mRecordingText.setText("REC");
                }
            }
        });
    }

    private void stopRecording() {
        mProduct.getCamera().stopRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showToast("stop recording: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                if (djiError != null)
                    Log.d(TAG, "stop recording: "+djiError.getDescription()+" ("+djiError.getErrorCode()+")");
                else {
                    if (mCameraRecording != null)
                        mCameraRecording.setText("STOP");
                    if(mRecordingText != null)
                        mRecordingText.setText("STOP");
                    else
                        mRecordingText = findViewById(R.id.text_recording_home);
                }
            }
        });
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
                            //sendDroneLocation(mDroneLocation);
                            updateMapDrone(mDroneLocation);
                            if (mLocationText != null)
                                mLocationText.setText(String.format("%,.4f", mDroneLocation.getLatitude())+","+String.format("%,.4f", mDroneLocation.getLongitude())+","+String.format("%,.1f", mDroneLocation.getAltitude()));
                     }
                });
                // Set position observer task
                positionObserverTask = new PositionObserverTask();
                positionObserverTimer = new Timer();
                positionObserverTimer.schedule(positionObserverTask, 50, 200);
            }
        }
        //setupFollowMeMission();
    }

    private void sendControllerLocation(Location ctrlLocation) {
        Log.d(TAG, "sendDroneLocation publish drone location");
        if (ctrlLocation.getLatitude() != 0 && ctrlLocation.getLongitude() != 0) {
            String msrg = String.format("lat|%.6f|lon|%.6f|ele|%.1f|h|%.1f|s|0|stat|0",ctrlLocation.getLatitude(),ctrlLocation.getLongitude(),ctrlLocation.getAltitude(),mDroneHeading);
            mMQTTclient.publish(msrg,mMQTTclient);
        }
    }

    private void sendDroneLocation(LocationCoordinate3D mDroneLocation) {
        Log.d(TAG, "sendDroneLocation publish drone location");
        if (mDroneLocation.getLatitude() != 0 && mDroneLocation.getLongitude() != 0) {
            String msrg = String.format("lat|%.6f|lon|%.6f|ele|%.1f|h|%.1f|s|0|stat|0",mDroneLocation.getLatitude(),mDroneLocation.getLongitude(),mDroneLocation.getAltitude(),mDroneHeading);
            mMQTTclient.publish(msrg,mMQTTclient);
        }
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
        if (v.getId() == mLandButton.getId()) {
            Log.d(TAG,"onClick Land pressed");
            landDrone();
        }
        
    }

    private void landDrone() {
        showToast("Land Drone");
        flightController.startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showToast("Landing Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                if (djiError != null)
                    Log.d(TAG, "Landing Start: "+djiError.getDescription()+" ("+djiError.getErrorCode()+")");
            }});
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    public void setHomeFragmentRef(HomeFragment homeFragment) {
        mHomeFragemnt = homeFragment;
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
            mDroneLocation = droneLoc;
            updateMapDrone(droneLoc);

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
            if(mFollowLocation != null) {
                float deltaLat = (float) (mFollowLocation.getLatitude() - droneLoc.getLatitude());
                float deltaLon = (float) (mFollowLocation.getLongitude() - droneLoc.getLongitude());
                // Convert to meters:
                deltaLatM = deltaLat * 111540; // Coords to meters
                deltaLonM = deltaLon * 111540; // Coords to meters and  then accont for latitude multiplier
                deltaLonM = (float) (deltaLonM*Math.cos(Math.toRadians(mFollowLocation.getLatitude())));
                determineStickPosition(deltaLatM, deltaLonM);

            }
            if (mBatteryText != null) {
                mProduct.getBattery().setStateCallback(mBatteryCallback);
            }
            else
                mBatteryText = findViewById(R.id.text_battery);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mAltitudeText != null)
                        mAltitudeText.setText(String.format("%.1f", droneLoc.getAltitude()));
                    else
                        mAltitudeText = findViewById(R.id.text_altitude);

                    if(mDeltaText != null)
                    {
                        mDeltaText.setText(String.format("%.1f,%.1f",deltaLatM,deltaLonM));
                    }
                    else
                        mDeltaText = findViewById(R.id.text_delta);

                    String msg = String.format("d_heading: %.1f d_ele: %.1f d_lat: %.1f d_lon: %.1f", heading, deltaAlt, deltaLatM, deltaLonM);
                    Log.d(TAG, msg);
                }
            });
        }
    }

    private void determineStickPosition(float deltaLatM, float deltaLonM) {

        if (deltaLatM > 0) // move south
        {
            if (deltaLatM > maxHorizontalSpeed)
                deltaLatM = maxHorizontalSpeed;
        }
        else if (deltaLatM < 0) // move north
            if (deltaLatM < -1f * maxHorizontalSpeed)
                deltaLatM = -1f * maxHorizontalSpeed;
        pitch = deltaLatM;

        if (deltaLonM > 0) // move west
        {
            if (deltaLonM > maxHorizontalSpeed)
                deltaLonM = maxHorizontalSpeed;
        }
        else if (deltaLonM < 0) // move east
            if (deltaLonM < maxHorizontalSpeed)
                deltaLonM = -1f * maxHorizontalSpeed;
        roll = deltaLonM;
    }
}