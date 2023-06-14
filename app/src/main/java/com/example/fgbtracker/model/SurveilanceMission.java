package com.example.fgbtracker.model;

import static java.lang.Thread.sleep;

import android.util.Log;

import com.example.fgbtracker.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class SurveilanceMission {
    private static final String TAG = SurveilanceMission.class.getName();
    private final MainActivity mainActivity;
    private VirtualPilot virtualPilot;
    private int state;
    private String targetID;
    private String targetMQTT;
    private double surveilanceAltitude;
    private int batteryLimit;
    private double homeLat;
    private double homeLon;
    private double homeGotoAlt;

    public SurveilanceMission(MainActivity mainAct, VirtualPilot pilot)
    {
        mainActivity = mainAct;
        virtualPilot = pilot;
        state = MissionStatus.PLANNED;
    }
    public void parseMissionMessage(String topic, String message)
    {
        String[] topicElems = topic.split("/");
        if(topicElems[topicElems.length-1].toLowerCase().compareTo("mission")==0) {
            parseMission(message);
        }
    }

    public void parseMission(String mission)
    {
        try {
            JSONObject reader = new JSONObject(mission);
            JSONObject target = reader.getJSONObject("target");
            JSONObject parameters = reader.getJSONObject("parameters");

            targetMQTT = target.getString("MQTTtopic");
            surveilanceAltitude = parameters.getDouble("altitude");
            batteryLimit = parameters.getInt("battery_limit");
            homeLat = parameters.getDouble("home_lat");
            homeLon = parameters.getDouble("home_lon");
            homeGotoAlt = parameters.getDouble("home_gotoalt");
            targetID = target.getString("id");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public String getTargetID(){
        return targetID;
    }

    public double getTargetAltitude()
    {
        if (state == MissionStatus.COMPLETED || state == MissionStatus.CANCELLED) {
            return homeGotoAlt;
        }
        else {
            return surveilanceAltitude;
        }
    }
    public void startMission() {
        Log.e(TAG, "startMission");
        mainActivity.mMQTTclient.subscribeMqttChannel(targetMQTT);
        if (virtualPilot != null) {
            Log.e(TAG, "Virtual pilot OK, starting");
            virtualPilot.startPilot();
            state = MissionStatus.RUNNING;
        }
        else {
            Log.e(TAG, "Virtual pilot was null");
        }

    }
    public void stopMission() {
        mainActivity.mMQTTclient.unsubscribeMqttChannel(targetMQTT);
        if (virtualPilot.isPilotActive()) {
            virtualPilot.stopPilot();
        }
        state = MissionStatus.COMPLETED;
    }
    public void pauseMission() {
        state = MissionStatus.PAUSED;
    }
    public void abortMission() {
        state = MissionStatus.RUNNING;
    }

    public int getState() {
        return state;
    }
}
