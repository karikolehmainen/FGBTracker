package com.example.fgbtracker;

import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

class MQTTClient implements IMqttActionListener {
    private final String TAG = MQTTClient.class.getSimpleName();
    private final MainActivity mainAct;

    private String topic = "rosetta";
    private MqttAndroidClient mqttClient;

    MQTTClient(MainActivity context,String serverURI, String clientID) {
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

    void setTopic(String topic)
    {
        this.topic = topic;
    }
    void connect() {
        Log.d(TAG, "Connecting MQTTClient");
        try {
            IMqttToken token = this.mqttClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "connected!!");
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

    void publish(String msg, IMqttActionListener cbPublish) {
        if(this.mqttClient.isConnected()) {
            Log.d(TAG, "publish" + msg);
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

    void publish(byte[] msg, IMqttActionListener cbPublish) {
        if(this.mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(msg);
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

    //    void disconnect(IMqttActionListener cbDisconnect) {
    void disconnect() {

        try {
            this.mqttClient.disconnect();
        } catch (MqttException e) {
            Log.e(TAG,e.toString());
        }
    }
    public void subscribeMqttChannel(String topic) {
        try {
            Log.d("tag","mqtt topic name>>>>>>>>" + topic);
            Log.d("tag","client.isConnected()>>>>>>>>" + mqttClient.isConnected());
            if (mqttClient.isConnected()) {
                mqttClient.subscribe(topic, 0);
                mqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        Log.d("tag","message>>" + new String(message.getPayload()));
                        Log.d("tag","topic>>" + topic);
                        mainAct.parseMqttMessage(new String(message.getPayload()));
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
}
