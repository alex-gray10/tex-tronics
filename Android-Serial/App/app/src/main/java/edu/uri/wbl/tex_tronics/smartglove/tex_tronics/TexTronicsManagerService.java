package edu.uri.wbl.tex_tronics.smartglove.tex_tronics;

import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import edu.uri.wbl.tex_tronics.smartglove.ble.BluetoothLeConnectionService;
import edu.uri.wbl.tex_tronics.smartglove.ble.GattCharacteristics;
import edu.uri.wbl.tex_tronics.smartglove.ble.GattServices;
import edu.uri.wbl.tex_tronics.smartglove.mqtt.MqttConnectionService;
import edu.uri.wbl.tex_tronics.smartglove.tex_tronics.devices.SmartGlove;
import edu.uri.wbl.tex_tronics.smartglove.tex_tronics.devices.TexTronicsDevice;
import edu.uri.wbl.tex_tronics.smartglove.tex_tronics.enums.Action;
import edu.uri.wbl.tex_tronics.smartglove.tex_tronics.enums.DeviceType;
import edu.uri.wbl.tex_tronics.smartglove.tex_tronics.enums.ExerciseMode;
import edu.uri.wbl.tex_tronics.smartglove.tex_tronics.exceptions.IllegalDeviceType;

/**
 * Created by mcons on 2/27/2018.
 *
 * @author Matthew Constant
 * @version 1.0, 02/28/2018
 */

public class TexTronicsManagerService extends Service {
    /**
     * The tag used to log messages to the LogCat.
     *
     * @since 1.0
     */
    private static final String TAG = "TexTronics Service";

    /**
     * Used to identity the device address to connect to.
     *
     * @since 1.0
     */
    private static final String EXTRA_DEVICE = "tex_tronics.wbl.uri.ble.device";

    /**
     * Used to identify the transmit mode.
     *
     * @since 1.0
     */
    private static final String EXTRA_MODE = "tex_tronics.wbl.uri.ble.mode";

    /**
     * Used to identify the device type.
     *
     * @since 1.0
     */
    private static final String EXTRA_TYPE = "tex_tronics.wbl.uri.ble.type";
    /**
     * The packet ID for the first packet transmitted when communicating in Flex+IMU mode.
     * This will be the first byte of the packet.
     *
     * @since 1.0
     */
    private static final byte PACKET_ID_1 = 0x01;
    /**
     * The packet ID for the second packet transmitted when communicating in Flex+IMU mode.
     * This will be the first byte of the packet.
     *
     * @since 1.0
     */
    private static final byte PACKET_ID_2 = 0x02;

    /**
     * The value to return in onStartCommand
     *
     * https://developer.android.com/reference/android/app/Service.html
     *
     * @since 1.0
     */
    private static final int INTENT_RETURN_POLICY = START_STICKY;

    /**
     * This static method is provided for other components to use in order to interact with this
     * service. The connect method requests this service attempts to connect to the BLE device
     * with the given device address.
     *
     * TODO Convert Context to WeakReference<Context>
     *
     * @param context Context of the calling component
     * @param deviceAddress Device Address of BLE Device to connect to.
     *
     * @since 1.0
     */
    public static void connect(Context context, String deviceAddress, ExerciseMode exerciseMode, DeviceType deviceType) {
        Intent intent = new Intent(context, TexTronicsManagerService.class);
        intent.putExtra(EXTRA_DEVICE, deviceAddress);
        intent.putExtra(EXTRA_MODE, exerciseMode);
        intent.putExtra(EXTRA_TYPE, deviceType);
        intent.setAction(Action.connect.toString());
        context.startService(intent);
    }

    /**
     * This static method is provided for other components to use in order to interact with this
     * service. The disconnect method requests this service attempts to disconnect from the
     * previously connected BLE device with the given device address. This method will only work
     * if the given devicehas already been successfully connected to using the connect method
     * provided by this service.
     *
     * TODO Convert Context to WeakReference<Context>
     *
     * @param context Context of the calling component
     * @param deviceAddress Device Address of BLE Device to disconnect from.
     *
     * @since 1.0
     */
    public static void disconnect(Context context, String deviceAddress) {
        Intent intent = new Intent(context, TexTronicsManagerService.class);
        intent.putExtra(EXTRA_DEVICE, deviceAddress);
        intent.setAction(Action.disconnect.toString());
        context.startService(intent);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TexTronicsManagerService.class);
        intent.setAction(Action.start.toString());
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, TexTronicsManagerService.class);
        intent.setAction(Action.stop.toString());
        context.startService(intent);
    }

    /**
     * Used by inner classes to refer to this Service's Context. Weak Reference should not be needed
     * unless this Service implements multi-threading in future.
     */
    private Context mContext;
    private boolean mServiceBound;
    private BluetoothLeConnectionService mService;
    private ServiceConnection mServiceConnection;

    /**
     * Contains reference to each connected Tex-Tronics Device.
     */
    private HashMap<String, TexTronicsDevice> mTexTronicsList;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service Created");

        /* Store reference to this Service's context (good practice so that all methods/inner
         *  classes use a consistent Context object). This will need to be converted to a
         *  WeakReference object if multi-threading is implemented.
         *
         *  https://developer.android.com/reference/java/lang/ref/WeakReference.html
         *  https://community.oracle.com/blogs/enicholas/2006/05/04/understanding-weak-references
         *
         *  Possible Alternatives to WeakReference: https://medium.com/google-developer-experts/weakreference-in-android-dd1e66b9be9d
         */
        mContext = this;

        // Initialize the Connection Service to interface to BluetoothLeService
        mServiceConnection = new BleServiceConnection();

        // Initialize Container for Tex-Tronic Connected Devices (set the initial capacity to 4 - 2 gloves, 2 socks)
        mTexTronicsList = new HashMap<>(4);

        // Register BLE Update Receiver to Receive Information back from BluetoothLeService
        registerReceiver(mBLEUpdateReceiver, new IntentFilter(BluetoothLeConnectionService.INTENT_FILTER_STRING));
        // Bind to BluetoothLeService. This Service provides the methods required to interact with BLE devices.
        bindService(new Intent(this, BluetoothLeConnectionService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Do not allow binding. All components should interact with this Service via the static methods provided.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        // Initial Check of Action Packet to make sure it contains the device address and Action
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Invalid Action Packet Received");
            return INTENT_RETURN_POLICY;
        }

        // Action to be performed on the BLE Device
        Action action = Action.getAction(intent.getAction());
        if(action == Action.start) {
            // Do Nothing

            // Start MQTT Service As Well
            MqttConnectionService.start(mContext);

            return INTENT_RETURN_POLICY;
        }

        // Device Address of the BLE Device corresponding to this Action Packet
        String deviceAddress = intent.getStringExtra(EXTRA_DEVICE);

        // Make sure it is a valid Action
        if(action == null) {
            Log.w(TAG, "Invalid Action Packet Received");
            return INTENT_RETURN_POLICY;
        }

        // Execute Action Packet (this can be done with multi-threading to be able to Service multiple Action Packets at once)
        switch (action) {
            case connect: {
                // Attempt to connect to BLE Device (Device Type and Transmitting Mode should be obtained during scan)
                if (!intent.hasExtra(EXTRA_TYPE) || !intent.hasExtra(EXTRA_MODE)) {
                    Log.w(TAG, "Invalid connect Action Packet Received");
                    return INTENT_RETURN_POLICY;
                }
                ExerciseMode exerciseMode = (ExerciseMode) intent.getSerializableExtra(EXTRA_MODE);
                DeviceType deviceType = (DeviceType) intent.getSerializableExtra(EXTRA_TYPE);
                connect(deviceAddress, exerciseMode, deviceType);
            }
            break;
            case disconnect:
                // Attempt to disconnect from a currently connected BLE Device
                disconnect(deviceAddress);
                break;
            case stop:
                // TODO Disconnect from Connected Devices First
                stopSelf();
        }

        return INTENT_RETURN_POLICY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBLEUpdateReceiver);
        unbindService(mServiceConnection);

        Log.d(TAG,"Service Destroyed");

        super.onDestroy();
    }

    private void connect(String deviceAddress, ExerciseMode exerciseMode, DeviceType deviceType) {
        if (mServiceBound) {
            // TODO Modify TexTronicsDevice to have static method to determine DeviceType to Use
            switch (deviceType) {
                case SMART_GLOVE:
                    // TODO Assume connection will be successful, if connection fails we must remove it from list.
                    SmartGlove smartGlove = new SmartGlove(deviceAddress, exerciseMode);
                    mTexTronicsList.put(deviceAddress, smartGlove);
                    break;
                // Add Different Devices Here
                default:

                    break;
            }

            mService.connect(deviceAddress);
        } else {
            Log.w(TAG,"Cannot Connect - BLE Connection Service is not bound yet!");
        }
    }

    private void disconnect(String deviceAddress) {
        if (mServiceBound) {
            mService.disconnect(deviceAddress);
        } else {
            Log.w(TAG,"Could not Disconnect - BLE Connection Service is not bound!");
        }
    }

    private class BleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mServiceBound = true;
            mService = ((BluetoothLeConnectionService.BLEConnectionBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceBound = false;
        }
    }

    private BroadcastReceiver mBLEUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received BLE Update");
            String deviceAddress = intent.getStringExtra(BluetoothLeConnectionService.INTENT_DEVICE);
            String action = intent.getStringExtra(BluetoothLeConnectionService.INTENT_EXTRA);
            switch (action) {
                case BluetoothLeConnectionService.GATT_STATE_CONNECTED:
                    mService.discoverServices(deviceAddress);
                    break;
                case BluetoothLeConnectionService.GATT_STATE_DISCONNECTED:
                    mTexTronicsList.remove(deviceAddress);
                    break;
                case BluetoothLeConnectionService.GATT_DISCOVERED_SERVICES:
                    BluetoothGattCharacteristic characteristic = mService.getCharacteristic(deviceAddress, GattServices.UART_SERVICE, GattCharacteristics.RX_CHARACTERISTIC);
                    if (characteristic != null) {
                        mService.enableNotifications(deviceAddress, characteristic);
                    }
                    break;
                case BluetoothLeConnectionService.GATT_CHARACTERISTIC_NOTIFY:
                    UUID characterUUID = UUID.fromString(intent.getStringExtra(BluetoothLeConnectionService.INTENT_CHARACTERISTIC));
                    if(characterUUID.equals(GattCharacteristics.RX_CHARACTERISTIC)) {
                        Log.d(TAG, "Data Received");
                        byte[] data = intent.getByteArrayExtra(BluetoothLeConnectionService.INTENT_DATA);

                        TexTronicsDevice device = mTexTronicsList.get(deviceAddress);
                        ExerciseMode exerciseMode = device.getExerciseMode();

                        try {
                            switch (exerciseMode) {
                                case FLEX_IMU:
                                    if (data[0] == PACKET_ID_1) {
                                        device.clear();
                                        device.setTimestamp(((data[1] & 0x00FF) << 24) | ((data[2] & 0x00FF) << 16) | ((data[3] & 0x00FF) << 8) | (data[4] & 0x00FF));
                                        device.setThumbFlex((((data[5] & 0x00FF) << 8) | ((data[6] & 0x00FF))));
                                        device.setIndexFlex((((data[7] & 0x00FF) << 8) | ((data[8] & 0x00FF))));
                                        // TODO: Add rest of fingers
                                    } else if (data[0] == PACKET_ID_2) {
                                        device.setAccX(((data[1] & 0x00FF) << 8) | ((data[2] & 0x00FF)));
                                        device.setAccY(((data[3] & 0x00FF) << 8) | ((data[4] & 0x00FF)));
                                        device.setAccZ(((data[5] & 0x00FF) << 8) | ((data[6] & 0x00FF)));
                                        device.setGyrX(((data[7] & 0x00FF) << 8) | ((data[8] & 0x00FF)));
                                        device.setGyrY(((data[9] & 0x00FF) << 8) | ((data[10] & 0x00FF)));
                                        device.setGyrZ(((data[11] & 0x00FF) << 8) | ((data[12] & 0x00FF)));
                                        device.setMagX(((data[13] & 0x00FF) << 8) | ((data[14] & 0x00FF)));
                                        device.setMagY(((data[15] & 0x00FF) << 8) | ((data[16] & 0x00FF)));
                                        device.setMagZ(((data[17] & 0x00FF) << 8) | ((data[18] & 0x00FF)));

                                        device.logData(mContext);
                                    } else {
                                        Log.w(TAG, "Invalid Data Packet");
                                        return;
                                    }
                                    break;
                                case FLEX_ONLY:
                                    // First Data Set
                                    device.setTimestamp((((data[0] & 0x00FF) << 8) | ((data[1] & 0x00FF))));
                                    device.setThumbFlex((((data[2] & 0x00FF) << 8) | ((data[3] & 0x00FF))));
                                    device.setIndexFlex((((data[4] & 0x00FF) << 8) | ((data[5] & 0x00FF))));

                                    device.logData(mContext);

                                    // Second Data Set
                                    device.setTimestamp((((data[6] & 0x00FF) << 8) | ((data[7] & 0x00FF))));
                                    device.setThumbFlex((((data[8] & 0x00FF) << 8) | ((data[9] & 0x00FF))));
                                    device.setIndexFlex((((data[10] & 0x00FF) << 8) | ((data[11] & 0x00FF))));

                                    device.logData(mContext);

                                    // Third Data Set
                                    device.setTimestamp((((data[12] & 0x00FF) << 8) | ((data[13] & 0x00FF))));
                                    device.setThumbFlex((((data[14] & 0x00FF) << 8) | ((data[15] & 0x00FF))));
                                    device.setIndexFlex((((data[16] & 0x00FF) << 8) | ((data[17] & 0x00FF))));

                                    device.logData(mContext);
                                    break;
                            }
                        } catch (IllegalDeviceType | IOException e) {
                            Log.e(TAG, e.toString());
                            // TODO Handle Error Event
                            return;
                        }
                    }
                    break;
                case BluetoothLeConnectionService.GATT_CHARACTERISTIC_READ:
                    break;
                case BluetoothLeConnectionService.GATT_DESCRIPTOR_WRITE:
                    break;
                case BluetoothLeConnectionService.GATT_NOTIFICATION_TOGGLED:
                    break;
                case BluetoothLeConnectionService.GATT_DEVICE_INFO_READ:
                    break;
            }
        }
    };
}