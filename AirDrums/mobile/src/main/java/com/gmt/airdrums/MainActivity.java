package com.gmt.airdrums;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sensoria.sensorialibrary.SAAnklet;
import com.sensoria.sensorialibrary.SAAnkletInterface;
import com.sensoria.sensorialibrary.SAFoundAnklet;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;
import com.thalmic.myo.Vector3;

import java.util.ArrayList;
import java.util.LinkedList;


public class MainActivity extends ActionBarActivity implements SAAnkletInterface {


    private SoundManager soundmanagerleft; /* Accesses sound library */
    private SoundManager soundmanagerright;

    /* Myo */
    private ArrayList<Myo> mKnownMyos = new ArrayList<Myo>();

    private TextView mLockStateView;
    private TextView mTextView;
    private TextView xString;
    private TextView yString;
    private TextView zString;

    private boolean raisedLeft = false;
    private boolean raisedRight = false;

    private float yawLeft;
    private float yawRight;

    private int buffer = 0;

    private static final String TAG = "MainActivity";

    private Context that = this;

    /* Variables for managing data from the sensoria */
    private SAAnklet anklet;


    private String selectedCode;
    private String selectedMac;


    private int counter=0,
                counterMax=5,
                soundbuffer=7;
    private int prevmtb1=0,
                prevmtb5=0,
                prevheel1=0,
                prevheel2=0;
    private int buffercurrSize = 5, /*  The amount of numbers to average in the present-value buffer */
            bufferpastSize = 10,    /* The amount of numbers to average in the past-value buffer */
            limit = 10;     /*  The minimum number of ticks to pass before another beat is produced. */
            //prevBeat = 0;   /*  The tick of the last time the beat was triggered. */
    /* Buffer Constants */
    private int thresholdHeel = 400,
            thresholdMtb5 = 4,
            thresholdMtb1 = 4;
    private LinkedList<Integer> bufferHeel, /*  LinkedList Buffers to dampen values. */
            bufferpastMtb5,
            buffercurrMtb5,
            bufferMtb1;

    private float ogAngleLeft = 0;
    private float ogAngleRight = 0;
    private float currAngleLeft = 0;
    private float currAngleRight = 0;

    private DeviceListener mListener = new AbstractDeviceListener() {

        @Override
        public void onAttach(Myo myo, long timestamp) {
            Log.e(TAG, "connected");
            mKnownMyos.add(myo);
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {

            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            Log.e("AirDrums", pose.toString());
            if(pose == Pose.FIST){
                myo.vibrate(Myo.VibrationType.SHORT);
                if(identifyMyo(myo)==1) {
                    ogAngleLeft = 0;
                }
                else {
                    ogAngleRight = 0;
                }
            }
        }

        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            myo.vibrate(Myo.VibrationType.SHORT);

        }

        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            myo.vibrate(Myo.VibrationType.SHORT);
            if(identifyMyo(myo)==1) {
                ogAngleLeft = 0;
            }
            else {
                ogAngleRight = 0;
            }
        }


        @Override
        public void onOrientationData( Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the Quaternion.
            if (identifyMyo(myo) == 1) {
                float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
                if (ogAngleLeft == 0) {
                    ogAngleLeft = yaw;
                }

                currAngleLeft = ogAngleLeft - yaw;
                mTextView.setText(Float.toString(ogAngleLeft - yaw));
            } else {
                float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
                if (ogAngleRight == 0) {
                    ogAngleRight = yaw;
                }

                currAngleRight = ogAngleRight - yaw;
                mTextView.setText(Float.toString(ogAngleRight - yaw));
            }
        }

        //Called when an attached Myo has provided new accelerometer data.
        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
            if(identifyMyo(myo)==1){
                double x = accel.x();

                if (x > 0.6) {

                    raisedLeft = true;
                }

                if (raisedLeft && x < .1) {

                    if(currAngleLeft > 240 || currAngleLeft<-15) {
                        soundmanagerleft.play("oh2");
                    } else if (currAngleLeft>-15 && currAngleLeft<35) {
                        soundmanagerleft.play("sd4");
                    } else if (currAngleLeft>=35 && currAngleLeft < 180) {
                        soundmanagerleft.play("sd1");
                    }
                    raisedLeft = false;
                }
            }
            else{
                double x = accel.x();

                if (x > 0.6) {
                    raisedRight = true;
                }
                if (raisedRight && x < .2) {

                    if(currAngleRight> 240 || currAngleRight <-15) {
                        soundmanagerright.play("sd4");
                    } else if (currAngleRight>-15 && currAngleRight<35) {
                        soundmanagerright.play("cp");
                    } else if (currAngleRight>=35 && currAngleRight < 180) {
                        soundmanagerright.play("ch");
                    }

                    raisedRight = false;
                }
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundmanagerleft = new SoundManager(this);
        soundmanagerright = new SoundManager(this);

        anklet = new SAAnklet(this);
        anklet.connect();

        bufferHeel = new LinkedList<Integer>();
        buffercurrMtb5 = new LinkedList<Integer>();
        bufferpastMtb5 = new LinkedList<Integer>();
        bufferMtb1 = new LinkedList<Integer>();


        mLockStateView = (TextView) findViewById(R.id.lock_state);
        mTextView = (TextView) findViewById(R.id.text);
        yawLeft = 0;
        yawRight = 0;


        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final int attachingCount = 2;
        hub.setMyoAttachAllowance(attachingCount);
        hub.attachToAdjacentMyos(attachingCount);

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);
    }

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    @Override
    protected void onResume() {
        super.onResume();

        anklet.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        anklet.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();


        anklet.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }

    public void onStartScan(View view) {
        anklet.startScan();
    }

    public void onStopScan(View view) {
        anklet.stopScan();
    }

    public void onConnect(View view) {
        System.out.println(""+selectedCode + " " + selectedMac);
        Log.w("SensoriaLibrary", "Connect to " + selectedCode + " " + selectedMac);
        anklet.deviceCode = selectedCode;
        anklet.deviceMac = selectedMac;
        anklet.connect();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int identifyMyo(Myo myo) {

        return mKnownMyos.indexOf(myo) + 1;
    }

    @Override
    public void didDiscoverDevice() {

        Log.w("SensoriaLibrary", "Device Discovered!");

        Spinner s = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, anklet.deviceDiscoveredList);
        s.setAdapter(adapter);

        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SAFoundAnklet deviceDiscovered = anklet.deviceDiscoveredList.get(position);
                selectedCode = deviceDiscovered.deviceCode;
                selectedMac = deviceDiscovered.deviceMac;

                Log.d("SensoriaLibrary", selectedCode + " " + selectedMac);

                anklet.deviceCode = selectedCode;
                anklet.deviceMac = selectedMac;
                anklet.connect();
                anklet.startScan();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCode = null;
            }
        });
    }

    @Override
    public void didConnect() {

        Log.w("SensoriaLibrary", "Device Connected!");

    }

    @Override
    public void didError(String message) {

        Log.e("SensoriaLibrary", message);

    }

    @Override
    public void didUpdateData() {

        TextView tick = (TextView) findViewById(R.id.tickValue);
        TextView mtb1 = (TextView) findViewById(R.id.mtb1Value);
        TextView mtb5 = (TextView) findViewById(R.id.mtb5Value);
        TextView heel = (TextView) findViewById(R.id.heelValue);
        TextView accX = (TextView) findViewById(R.id.accXValue);
        TextView accY = (TextView) findViewById(R.id.accYValue);
        TextView accZ = (TextView) findViewById(R.id.accZValue);

        tick.setText(String.format("%d", anklet.tick));
        mtb1.setText(String.format("%d", anklet.mtb1));
        mtb5.setText(String.format("%d", anklet.mtb5));
        heel.setText(String.format("%d", anklet.heel));
        accX.setText(String.format("%f", anklet.accX));
        accY.setText(String.format("%f", anklet.accY));
        accZ.setText(String.format("%f", anklet.accZ));


        /* BEAT ALGORITHM FOR SENSORIA */
        /*
        int tempheel = anklet.heel;
        if( prevheel1 != 0 && prevheel2 != 0) {
            int acc1 = tempheel - prevheel1;
            int acc2 = prevheel1 - prevheel2;
            if (acc1 < 0 && acc2 > 0) {
                SoundManager.playSound(this);
            }
        }
        if(counter>=counterMax) {
            prevheel2 = prevheel2;
            prevheel1 = tempheel;
        }
        */

        int temp1 = anklet.mtb1;
        int temp2 = anklet.mtb5;

        if (temp1 != 0 || temp2 != 0) {
            System.out.println(anklet.accZ);

            if (temp1 - prevmtb1 > 4 ||
                    temp2 - prevmtb5 > 4) {
                if (counter >= soundbuffer) {
                    soundmanagerright.play("bd1");
                    counter = 0;
                }
            }

            /*
            if ( (temp1 - prevmtb1 > 4 ||
                  temp2 - prevmtb5 > 4) &&
                    (anklet.accX < -0.80 &&
                     anklet.accZ < 0.41)) {
                if(counter>=soundbuffer) {
                    soundmanagerright.play("bd1");
                    counter = 0;
                }
            }
            */
        }

        prevmtb1 = anklet.mtb1;
        prevmtb5 = anklet.mtb5;

        counter++;

        /*
        if (bufferpastMtb5.size() >= bufferpastSize &&
                buffercurrMtb5.size() >= buffercurrSize) {

            int valueMtb5 = anklet.mtb1;


            // Find the average of the values in the buffer representing the current reading.
            int buffercurrAverage = 0;
            for (int x = 0; x < buffercurrMtb5.size(); x++) {
                buffercurrAverage += buffercurrMtb5.get(x);
            }
            buffercurrAverage /= buffercurrMtb5.size();

            int bufferpastAverage = 0;
            for( int x=0; x < bufferpastMtb5.size(); x++) {
                bufferpastAverage += bufferpastMtb5.get(x);
            }
            bufferpastAverage /= bufferpastMtb5.size();

            // If the threshold is crossed, then play a sound.
            System.out.println("Buffer Averages: " + buffercurrAverage + "\t" + bufferpastAverage);
            if (buffercurrAverage - bufferpastAverage >= 0) {
                //SoundManager.playSound(this);

                // Remove 'buffercurrSize' elements from both buffers.
                buffercurrMtb5.clear();
                for( int x=0; x<buffercurrSize; x++) {
                    bufferpastMtb5.pop();
                }
            }

            // Make sure that the buffer linkedlist remains capped at bufferSize values.
            int overflowCurr = buffercurrMtb5.size() - buffercurrSize;
            for (int x = 0; x <= overflowCurr; x++) {
                // Remove latest history
                buffercurrMtb5.pop();
            }
            buffercurrMtb5.push(valueMtb5);

            int overflowPast = bufferpastMtb5.size() - bufferpastSize;
            for (int x = 0; x <= overflowPast; x++) {
                // Remove latest history
                bufferpastMtb5.pop();
            }
            bufferpastMtb5.push(valueMtb5);

        } else {

            buffercurrMtb5.push(anklet.mtb5);
            bufferpastMtb5.push(anklet.mtb5);



        }
        */
    }


    public void playTestSound(View view) {
        soundmanagerleft.play("bd1");
    }
    public void playTestSound2(View view) {
        soundmanagerleft.play("sd2");
    }
}
