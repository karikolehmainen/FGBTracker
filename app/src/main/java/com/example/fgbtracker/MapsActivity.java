package com.example.fgbtracker;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fgbtracker.ui.maps.MapsFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import fgbtracker.R;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String TAG = MapsActivity.class.getName();
    private GoogleMap mMap;
    private MarkerOptions mDroneMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void updateDroneMarker(double latitude, double longitude)
    {
        LatLng point = new LatLng(latitude,longitude);
        mDroneMarker = new MarkerOptions().position(point).title("drone");
        mMap.moveCamera(CameraUpdateFactory.newLatLng(point));
    }
    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions()
        //        .position(sydney)
        //        .title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void updateRobotMarker(Location robot) {
        LatLng point = new LatLng(robot.getLatitude(),robot.getLongitude());
        mDroneMarker = new MarkerOptions().position(point).title("robot");
        mMap.moveCamera(CameraUpdateFactory.newLatLng(point));
    }
}