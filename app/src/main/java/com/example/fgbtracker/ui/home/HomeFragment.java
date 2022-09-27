package com.example.fgbtracker.ui.home;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private Bundle savedState = null;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        //HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        activity = (MainActivity) getActivity();
        binding = FragmentHomeBinding.inflate(inflater, (ViewGroup) container, false);
        View root = binding.getRoot();
        activity.setHomeFragmentRef(this);
        Log.d(TAG, "onCreateView -end");
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        binding = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        //Save the fragment's state here
    }
}