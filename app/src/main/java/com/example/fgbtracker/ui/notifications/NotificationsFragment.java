package com.example.fgbtracker.ui.notifications;
/*
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import fgbtracker.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import com.example.fgbtracker.MainActivity;
import com.example.fgbtracker.ui.home.HomeViewModel;

import fgbtracker.R;
import fgbtracker.databinding.FragmentHomeBinding;
import fgbtracker.databinding.FragmentNotificationsBinding;

// Display value of preference in summary field
//public class NotificationsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
public class NotificationsFragment extends Fragment {
    protected static final String PAGE_ID = "settings";
    private static final String TAG = NotificationsFragment.class.getSimpleName();
    SharedPreferences sharedPreferences;


    public static NotificationsFragment newInstance(String pageId) {
        Log.d(TAG,"newInstance");
        NotificationsFragment settingsFragment = new NotificationsFragment();
        Bundle args = new Bundle();
        args.putString(PAGE_ID, pageId);
        //settingsFragment.setArguments(args);
        return (settingsFragment);
    }

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        NotificationsViewModel settingsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        binding = FragmentNotificationsBinding.inflate(inflater, (ViewGroup) container, false);
        View root = binding.getRoot();
        Log.d(TAG,"onCreateView -end");
        return root;
    }
    /**
     * @param savedInstanceState Any saved state we are bringing into the new fragment instance
     **/
    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        sharedPreferences = getPreferenceManager().getSharedPreferences();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
*/
    /**
     * @param savedInstanceState
     * @param rootKey

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(TAG,"onCreatePreferences");
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setListeners();
    }

    public void setListeners() {
        Log.d(TAG,"setListeners");

        findPreference("pref_separate_gcs").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                MainActivity.FLAG_PREFS_CHANGED = true;
                MainActivity.FLAG_VIDEO_ADDRESS_CHANGED = true;
                return true;
            }
        });


        // Kari: MQTT Settings here
        findPreference("pref_mqtt_hosturl").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    MainActivity.FLAG_PREFS_CHANGED = true;
                    MainActivity.FLAG_MQTT_HOST_CHANGED = true;
                    return true;
                } catch (NumberFormatException ignored) {
                }
                //NotificationHandler.notifyAlert(SettingsFragment.this.getActivity(), TYPE_MQTT_HOST, null, null);
                return false;
            }
        });
        findPreference("pref_mqtt_topicdef").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    MainActivity.FLAG_PREFS_CHANGED = true;
                    MainActivity.FLAG_MQTT_TOPIC_CHANGED = true;
                    return true;
                } catch (NumberFormatException ignored) {
                }
                //NotificationHandler.notifyAlert(SettingsFragment.this.getActivity(), TYPE_MQTT_TOPIC, null, null);
                return false;
            }
        });

        findPreference("pref_video_ip").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Patterns.IP_ADDRESS.matcher((String) newValue).matches()) {
                    MainActivity.FLAG_PREFS_CHANGED = true;
                    MainActivity.FLAG_VIDEO_ADDRESS_CHANGED = true;
                    return true;
                } else {
                    //NotificationHandler.notifyAlert(SettingsFragment.this.getActivity(), TYPE_VIDEO_IP, null, null);
                    return false;
                }
            }
        });

        findPreference("pref_enable_video").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                MainActivity.FLAG_PREFS_CHANGED = true;
                MainActivity.FLAG_VIDEO_ADDRESS_CHANGED = true;
                return true;
            }
        });

        findPreference("pref_video_port").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    if (Integer.parseInt((String) newValue) >= 1 && Integer.parseInt((String) newValue) <= 65535) {
                        MainActivity.FLAG_PREFS_CHANGED = true;
                        MainActivity.FLAG_VIDEO_ADDRESS_CHANGED = true;
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
                //NotificationHandler.notifyAlert(SettingsFragment.this.getActivity(), TYPE_VIDEO_PORT, null, null);
                return false;
            }
        });

        findPreference("pref_video_bitrate").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    if (Integer.parseInt((String) newValue) >= 1 && Integer.parseInt((String) newValue) <= 65535) {
                        MainActivity.FLAG_PREFS_CHANGED = true;
                        MainActivity.FLAG_VIDEO_ADDRESS_CHANGED = true;
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
                //NotificationHandler.notifyAlert(SettingsFragment.this.getActivity(), TYPE_VIDEO_BITRATE, null, null);
                return false;
            }
        });

        findPreference("pref_encode_speed").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                MainActivity.FLAG_PREFS_CHANGED = true;
                MainActivity.FLAG_VIDEO_ADDRESS_CHANGED = true;
                return true;
            }
        });
    }
     */

    /**
     *

    @Override
    public void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    Preference singlePref = preferenceGroup.getPreference(j);
                    updatePreference(singlePref);
                }
            } else {
                updatePreference(preference);
            }
        }

    }
     */
    /**
     *
     */
    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        //sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    /**
     * @param sharedPreferences
     * @param key

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged");
        //updatePreference(findPreference(key));
    }
     */
    /**
     * @param preference
     */
    private void updatePreference(Preference preference) {
        Log.d(TAG,"updatePreference");
        if (preference == null) return;
        if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) preference;
            preference.setSummary(editTextPref.getText());
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
            return;
        } else {
            return;
        }
        //SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();
        //preference.setSummary(sharedPrefs.getString(preference.getKey(), "Default"));
    }


}