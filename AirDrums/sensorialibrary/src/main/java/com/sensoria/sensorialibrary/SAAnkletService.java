package com.sensoria.sensorialibrary;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SAAnkletService extends Service implements SAAnkletInterface {

    public final static String broadcastAction = "com.sensoria.sensorialibrary.BroadcastAction";

    public static final int GENERIC_MESSAGE = 0;
    public static final int DISCOVER_MESSAGE = 1;
    public static final int CONNECT_MESSAGE = 2;
    public static final int DATA_MESSAGE = 3;
    public static final int ERROR_MESSAGE = 4;

    private final static String TAG = SAAnkletService.class.getSimpleName();

    private SAAnklet anklet;

    public SAAnkletService() {
    }

    private void broadcastUpdate(Intent broadcastIntent) {
        broadcastIntent.setAction(broadcastAction);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(broadcastIntent);
    }


    private void broadcastErrorMessage(final String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("Type", ERROR_MESSAGE);
        broadcastIntent.putExtra("Message", message);

        broadcastUpdate(broadcastIntent);
    }

    private void broadcastSimpleMessage(int type) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("Type", type);

        broadcastUpdate(broadcastIntent);
    }

    private void broadcastDataMessage(int tick, int mtb1, int mtb5, int heel, float accX, float accY, float accZ) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("Type", DATA_MESSAGE);
        broadcastIntent.putExtra("tick", tick);
        broadcastIntent.putExtra("mtb1", mtb1);
        broadcastIntent.putExtra("mtb5", mtb5);
        broadcastIntent.putExtra("heel", heel);
        broadcastIntent.putExtra("accX", accX);
        broadcastIntent.putExtra("accY", accY);
        broadcastIntent.putExtra("accZ", accZ);

        broadcastUpdate(broadcastIntent);
    }


    @Override
    public void didDiscoverDevice() {
        broadcastSimpleMessage(DISCOVER_MESSAGE);
    }

    @Override
    public void didConnect() {
        broadcastSimpleMessage(CONNECT_MESSAGE);
    }

    @Override
    public void didError(String message) {
        broadcastErrorMessage(message);
    }

    @Override
    public void didUpdateData() {
        broadcastDataMessage(anklet.tick, anklet.mtb1, anklet.mtb5, anklet.heel, anklet.accX, anklet.accY, anklet.accZ);
    }

    public class LocalBinder extends Binder {
        public SAAnkletService getService() {
            return SAAnkletService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {


        anklet = new SAAnklet(this);


        return true;
    }

    public boolean connect(final String address) {

        anklet.deviceCode = null;
        anklet.deviceMac = address;
        anklet.connect();

        return true;
    }

    public void disconnect() {

        anklet.disconnect();

    }

}
