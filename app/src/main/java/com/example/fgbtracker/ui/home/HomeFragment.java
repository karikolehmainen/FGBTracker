package com.example.fgbtracker.ui.home;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import com.example.fgbtracker.MainActivity;
import com.example.fgbtracker.ui.maps.MapsFragment;

import fgbtracker.R;
import fgbtracker.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    public static final String TAG = HomeFragment.class.getName();
    private FragmentHomeBinding binding;
    private MainActivity activity;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, (ViewGroup) container, false);
        View root = binding.getRoot();
        activity = (MainActivity) getActivity();

        // Moved UI Elements from Main Activity here...

        activity.setHomeFragmentRef(this);
        Log.d(TAG, "onCreateView -end");
        return root;
    }

    private void updateMapPoint(Location robot) {
        Log.d(TAG, "updateMapPoint "+robot.getLatitude()+","+robot.getLongitude());
        Fragment mapsFragment = null;
        Fragment parent = getChildFragmentManager().findFragmentById(R.id.situmap);
        if (parent != null) {
                ((MapsFragment)mapsFragment).updateRobotMarker(robot);
        }
        else {
                Log.e(TAG, "updateMapPoint mapsFragment null");
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}