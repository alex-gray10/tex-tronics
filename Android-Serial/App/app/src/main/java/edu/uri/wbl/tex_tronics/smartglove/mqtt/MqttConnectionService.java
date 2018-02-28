package edu.uri.wbl.tex_tronics.smartglove.mqtt;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by mcons on 2/28/2018.
 *
 * @author Matthew Constant
 * @version 1.0, 02/28/2018
 */

public class MqttConnectionService extends Service {
    private static final String TAG = "MQTT Service";
    private static final String EXTRA_DATA = "uri.wbl.tex_tronics.mqtt.data";

    public static void start(Context context) {
        Intent intent = new Intent(context, MqttConnectionService.class);
        context.startService(intent);
    }

    public static void publish(Context context, String data) {
        Intent intent = new Intent(context, MqttConnectionService.class);
        intent.putExtra(EXTRA_DATA, data);
        context.startService(intent);
    }

    public static String generateJson(String date, String sensorId, String data) {
        JsonData jsonData = new JsonData(date, sensorId, data);
        Log.d(TAG,"JSON Data: " + jsonData.toString());
        return jsonData.toString();
    }

    private final String SERVER_URI = "tcp://131.128.51.213:1883";
    private final String PUBLISH_TOPIC = "kaya/patient/data";

    private MqttAndroidClient mMqttAndroidClient;
    private boolean mConnected;
    private String mClientId;


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service Created");

        mConnected = false;
        mClientId = "Patient";

        mMqttAndroidClient = new MqttAndroidClient(getApplicationContext(), SERVER_URI, mClientId);
        mMqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Lost connection to Server
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // Do Nothing, for now
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Do Nothing, for now
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            mMqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Successfully Connected");
                    sendUpdate(UpdateType.connected);
                    mConnected = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to Connect");
                    sendUpdate(UpdateType.disconnected);
                    mConnected = false;
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!intent.hasExtra(EXTRA_DATA)) {
            Log.w(TAG,"Invalid Data Packet");
            return START_STICKY;
        }

        String data = intent.getStringExtra(EXTRA_DATA);
        Log.d(TAG, "DATA: " + data);
        publishMessage(data);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service Destroyed");

        super.onDestroy();
    }

    public void publishMessage(String data){
        if(mConnected) {
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(data.getBytes());
                mMqttAndroidClient.publish(PUBLISH_TOPIC, message);
                if (!mMqttAndroidClient.isConnected()) {
                    Log.w(TAG, "MQTT Not Connected");
                    return;
                }
            } catch (MqttException e) {
                Log.w(TAG, "Error Publishing: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Not Connected Yet!");
        }
    }

    private void sendUpdate(UpdateType updateType) {
        Intent intent = new Intent(MqttUpdateReceiver.INTENT_FILTER_STRING);
        intent.putExtra(MqttUpdateReceiver.UPDATE_TYPE, updateType);
        sendBroadcast(intent);
    }
}
