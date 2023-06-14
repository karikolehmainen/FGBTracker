package com.example.fgbtracker.control;

import android.util.Log;

import com.example.fgbtracker.MainActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

public class MQTTClient implements IMqttActionListener {
    private final String TAG = MQTTClient.class.getSimpleName();
    private final MainActivity mainAct;

    private String topic = "rosetta";
    private String statusTopic = "rosetta/attr";
    private MqttAndroidClient mqttClient;

    public MQTTClient(MainActivity context, String serverURI, String clientID) {
        mainAct = context;
        Log.d(TAG, "Create MQTTClient");
        this.mqttClient = new MqttAndroidClient(context.getApplicationContext(), serverURI, clientID);

        this.mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.w(TAG, "connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Log.d(TAG, message.getPayload().toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //Log.d(TAG, "deliveryComplete");
            }
        });

        Log.d(TAG, "Created MQTTClient");
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
        String[] topicElems = topic.split("/");
        // if robot is reporting its status
        this.statusTopic = "/";
        for (int i = 1; i < topicElems.length-1; i++) {
            this.statusTopic += topicElems[i]+"/";
        }
        this.statusTopic += "attr";
    }

    public void connect() {
        Log.d(TAG, "Connecting MQTTClient");
        try {
            IMqttToken token = this.mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "connected!!");
                    subscribeMqttChannel(topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG,"connection failed!!");
                    Log.e(TAG,exception.toString());
                }
            });
            //while(!token.isComplete());
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        Log.d(TAG, "Connected MQTTClient");
    }

    public void publish(String msg, IMqttActionListener cbPublish) {
        if(this.mqttClient.isConnected()) {
            //Log.d(TAG, "publish: " + msg);
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                message.setQos(1);
                message.setRetained(false);
                this.mqttClient.publish(this.topic, message, null, cbPublish);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }
        else
        {
            Log.d(TAG, "MQTT client not connected");
        }
    }
    public void publishDroneLocation(String msg, IMqttActionListener cbPublish) {
        if(this.mqttClient.isConnected()) {
            //Log.d(TAG, "publishDroneLocation: " + msg);
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                message.setQos(1);
                message.setRetained(false);
                this.mqttClient.publish(this.statusTopic, message, null, cbPublish);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }
        else
        {
            Log.d(TAG, "MQTT client not connected");
        }
    }
    public void disconnect() {

        try {
            this.mqttClient.disconnect();
        } catch (MqttException e) {
            Log.e(TAG,e.toString());
        }
    }
    public void unsubscribeMqttChannel(String topic) {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.unsubscribe(topic);
            }
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void subscribeMqttChannel(String topic) {
        try {
            Log.d(TAG,"mqtt topic name: " + topic);
            Log.d(TAG,"client.isConnected(): " + mqttClient.isConnected());
            if (mqttClient.isConnected()) {
                mqttClient.subscribe(topic, 0);
                mqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        //Log.d(TAG,"message>>" + new String(message.getPayload()));
                        //Log.d(TAG,"topic>>" + topic);
                        mainAct.parseMqttMessage(topic, new String(message.getPayload()));

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        } catch (Exception e) {
            Log.d("tag","Error :" + e);
        }
    }
    
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        ;
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        ;
    }

    public void publishPosition(double latitude, double longitude, float altitude, float mDroneHeading, double v) {

    }
}
