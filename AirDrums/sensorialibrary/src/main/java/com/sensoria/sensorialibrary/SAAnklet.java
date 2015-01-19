package com.sensoria.sensorialibrary;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Jacopo Mangiavacchi on 12/8/14.
 */
public class SAAnklet extends SAFoundAnklet implements BluetoothAdapter.LeScanCallback  {

    public boolean connected = false;

    public int mtb1 = 0;
    public int mtb5 = 0;
    public int heel = 0;
    public int tick = 0;
    public float accX = 0.0f;
    public float accY = 0.0f;
    public float accZ = 0.0f;

    public ArrayList<SAFoundAnklet> deviceDiscoveredList = new ArrayList<SAFoundAnklet>();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mConnectedGatt; //mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_SCANNING = 3;

    public final static UUID UUID_SENSORIA_FAST_STREAMING_DATA = UUID.fromString("1cac2e60-0201-11e3-898d-0002a5d5c51b");

    private SAAnkletInterface iAnklet;
    private Context callerContext;

    Handler handler;

    private final static String TAG = SAAnklet.class.getSimpleName();

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public SAAnklet(SAAnkletInterface delegate)
    {
        // Save the event object for later use.
        iAnklet = delegate;
        callerContext = (Context)delegate;

        handler = new Handler();

        BluetoothManager manager = (BluetoothManager) callerContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            iAnklet.didError("No LE Support.");
            return;
        }
    }

    public void resume() {
        if (!callerContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            iAnklet.didError("No LE Support.");
            return;
        }
    }

    public void pause() {
        mBluetoothAdapter.stopLeScan(this);
    }

    public void connect() {
        if (mConnectionState == STATE_SCANNING) {
            mBluetoothAdapter.stopLeScan(this);
        }
        mConnectionState = STATE_CONNECTING;

        if (deviceMac == null) {
            //Connect re-scanning devices
            deviceDiscoveredList.clear();
            mBluetoothAdapter.startLeScan(this);
        }
        else {
            //Connect to passed Mac Address
//            // Previously connected device.  Try to reconnect.
//            if (mBluetoothGatt != null) {
//                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//                if (mBluetoothGatt.connect()) {
//                    return;
//                } else {
//                    return;
//                }
//            }

            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceMac);
            if (device == null) {
                iAnklet.didError("Device not found.  Unable to connect.");
                return;
            }

            connectOnTheMainThread(callerContext, device);
        }
    }

    public void disconnect() {
        mConnectionState = STATE_DISCONNECTED;

        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    public void startScan() {
        mConnectionState = STATE_SCANNING;
        deviceDiscoveredList.clear();
        mBluetoothAdapter.startLeScan(this);
    }

    public void stopScan() {
        mConnectionState = STATE_DISCONNECTED;
        mBluetoothAdapter.stopLeScan(this);
    }

    /* BluetoothAdapter.LeScanCallback */

    private boolean foundDeviceCodeInArray(String foundDeviceCode) {

        for (SAFoundAnklet storedDeviceDiscovered : deviceDiscoveredList) {
            if (storedDeviceDiscovered.deviceCode.equals(foundDeviceCode)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        String deviceName = device.getName();
        String foundDeviceMac = device.getAddress();

        if (deviceName.startsWith("Sensoria-F1-")) {
            String foundDeviceCode = deviceName.substring(12);

            if (!foundDeviceCodeInArray(foundDeviceCode)) {
                SAFoundAnklet deviceDiscovered = new SAFoundAnklet();

                deviceDiscovered.deviceCode = foundDeviceCode;
                deviceDiscovered.deviceMac = foundDeviceMac;

                deviceDiscoveredList.add(deviceDiscovered);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iAnklet.didDiscoverDevice();
                    }
                });

                if (mConnectionState == STATE_CONNECTING && deviceMac == null && deviceCode.equals(foundDeviceCode)) {
                    stopScan();

                    Log.i(TAG, "Connecting to " + foundDeviceCode + " after scan");
                    connectOnTheMainThread(callerContext, device);
                }
            }
        }
    }

    private void connectOnTheMainThread(final Context context, final BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectedGatt = device.connectGatt(context, false, mGattCallback);
            }
        });
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
                gatt.discoverServices();
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Loops through available GATT Services to look for Sensoria Fitness Streaming Service
                //UUID.fromString("1cac2e60-0200-11e3-898d-0002a5d5c51b")
                for (BluetoothGattService gattService : gatt.getServices())
                {
                    if (gattService.getUuid().toString().compareToIgnoreCase("1cac2e60-0200-11e3-898d-0002a5d5c51b") == 0) {
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                        // Loops through available Characteristics to find streaming service
                        //UUID.fromString("1cac2e60-0201-11e3-898d-0002a5d5c51b")
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            if (gattCharacteristic.getUuid().toString().compareToIgnoreCase("1cac2e60-0201-11e3-898d-0002a5d5c51b") == 0) {
                                final int charaProp = gattCharacteristic.getProperties();

                                // Confirm that this supports notify
                                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                    setCharacteristicNotification(gattCharacteristic, true);

                                    mConnectionState = STATE_CONNECTED;  //TODO: JACOPO: Check if not just set in onConnectionStateChange

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            iAnklet.didConnect();
                                        }
                                    });
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Enables or disables notification on a give characteristic.
         *
         * @param characteristic Characteristic to act on.
         * @param enabled If true, enable notification.  False otherwise.
         */
        private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                   boolean enabled) {
            if (mBluetoothAdapter == null || mConnectedGatt == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iAnklet.didError("BluetoothAdapter not initialized");
                    }
                });

                return;
            }

            mConnectedGatt.setCharacteristicNotification(characteristic, enabled);

            byte[] enableNotification = (enabled) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

            UUID uuidCharacteristic = characteristic.getUuid();

            Log.d(TAG, "setCharacteristicNotification: UUID: " + uuidCharacteristic.toString());

            // This is specific to Heart Rate Measurement.
            //if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            //BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

            List<BluetoothGattDescriptor> bluetoothGattDescriptors = characteristic.getDescriptors();

            if(bluetoothGattDescriptors == null || bluetoothGattDescriptors.size() == 0) {
                return;
            }

            //BluetoothGattDescriptor descriptor = bluetoothGattDescriptors.get(0);
            BluetoothGattDescriptor descriptor = bluetoothGattDescriptors.get(1);

            descriptor.setValue(enableNotification);
            mConnectedGatt.writeDescriptor(descriptor);
            //}
        }


        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: " + rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            Log.d(TAG, "onCharacteristicRead");

            if (status == BluetoothGatt.GATT_SUCCESS) {


            }
        }

        private float byteToGs(int b) {
            float val;

            if (b < 128) {
                val = 0.03125f * (float)b;
            }
            else {
                val = 0.03125f * -(256 - (float)b);
            }

            return val;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            if (UUID_SENSORIA_FAST_STREAMING_DATA.equals(characteristic.getUuid())) {

                mtb1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
                mtb5 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
                heel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
                tick = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 9);
                accX = byteToGs(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                accY = byteToGs(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1));
                accZ = byteToGs(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iAnklet.didUpdateData();
                    }
                });
            }
            else {
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    Log.d(TAG, new String(data) + "\n" + stringBuilder.toString());
                }
            }
        }
    };
}
