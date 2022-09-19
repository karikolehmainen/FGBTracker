package com.example.fgbtracker.ui.maps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import dji.common.flightcontroller.LocationCoordinate3D;
import fgbtracker.R;
//import fgbtracker.databinding.FragmentDashboardBinding;
import fgbtracker.databinding.FragmentMapsBinding;

public class MapsFragment extends Fragment {
    public static final String TAG = MapsFragment.class.getName();
    private FragmentMapsBinding binding;

    private GoogleMap mMap;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {

            mMap = googleMap;
        }
    };
    private MarkerOptions mDroneMarker;
    private MarkerOptions mRobotMarker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        binding = FragmentMapsBinding.inflate(inflater, container,false);

        View root = binding.getRoot();
        //return inflater.inflate(R.layout.fragment_maps, container, false);
        Log.d(TAG, "onCreateView");
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    public void updateRobotMarker(Location robot) {
        LatLng point = new LatLng(robot.getLatitude(),robot.getLongitude());
        if (mRobotMarker != null)
            mRobotMarker.position(point);
        else {
            mRobotMarker = new MarkerOptions().position(point).title("robot")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.robot_icon));
            mMap.addMarker(mRobotMarker);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(point));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1, null);
    }

    public void updateDroneMarker(LocationCoordinate3D robot) {
        LatLng point = new LatLng(robot.getLatitude(),robot.getLongitude());
        if (mDroneMarker != null)
            mDroneMarker.position(point);
        else {
            mDroneMarker = new MarkerOptions().position(point).title("drone")
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.drone_icon));
            mMap.addMarker(mDroneMarker);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(point));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18), 1, null);
    }
}