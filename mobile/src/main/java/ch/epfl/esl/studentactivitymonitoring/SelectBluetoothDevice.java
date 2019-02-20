package ch.epfl.esl.studentactivitymonitoring;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Node;

import ch.epfl.esl.commons.Constants;

import java.util.List;

/**
 * Created by Lara on 2/19/2018.
 * Prompt user to select either a previously-connected Bluetooth device or a new one
 * Attempt to connect to the device and throw an error if the device is out of range and/or not collecting data
 */

public class SelectBluetoothDevice extends AppCompatActivity {

    private static final String TAG = "SelectBluetoothDevice";

    public static BluetoothLeService mBluetoothLeService;

    public static String EXTRAS_DEVICE_NAME; //Selected Bluetooth device name
    public static String EXTRAS_DEVICE_ADDRESS; //Selected BT device address
    public static String EXTRAS_ORTHOSTAT_TEST_TYPE; //Type of orthostatic test to be performed

    public static final String nonexistentPreviousDevice = "nonexistent";

    private boolean mConnected = false;
    private static boolean connectingToOldDevice; //Are you connecting to the previously connected device or a new one?

    SharedPreferences sharedPref;
    private static Context context;

    //UI
    private static Button reconnectButton;
    private static Button connectButton;
    private static ProgressBar progressBar;
    BluetoothScanListFragment btFragment;
    LinearLayout connectToNewDeviceView;
    ScrollView connectToOldDeviceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_bluetooth_device);

        context = getApplicationContext();
        mConnected = false;

        //Try to get last connected BT device from shared preferences
        sharedPref = this.getSharedPreferences("strings", Context.MODE_PRIVATE);
        EXTRAS_DEVICE_NAME = sharedPref.getString("bleDeviceName",nonexistentPreviousDevice);
        EXTRAS_DEVICE_ADDRESS = sharedPref.getString("bleDeviceAddress",nonexistentPreviousDevice);

        //Get type of orthostatic test to pass into next activity
        Intent intent = getIntent();
        EXTRAS_ORTHOSTAT_TEST_TYPE = intent.getStringExtra(Constants.OrthostatTestType.ID);

//        //UI elements
        connectToNewDeviceView = findViewById(R.id.connectToNewDeviceView);
        connectToOldDeviceView = findViewById(R.id.connectToOldDeviceView);
        TextView previousDeviceTV = (TextView) findViewById(R.id.previousDevicePrompt);
        TextView connectedDeviceName = (TextView) findViewById(R.id.bleDeviceName);
        reconnectButton = (Button) findViewById(R.id.connectToPreviousDevice);
        connectButton = (Button) findViewById(R.id.connectToNewDevice);
        connectButton.setEnabled(false); //connect button disabled until new device is chosen

        btFragment = (BluetoothScanListFragment) getFragmentManager().findFragmentById(R.id.bt_list_fragment);

        progressBar = findViewById(R.id.bluetoothConnectionProgressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        progressBar.setVisibility(View.INVISIBLE);

        //Determine if there was a previously connected device and update UI accordingly
        if (EXTRAS_DEVICE_NAME.equals(nonexistentPreviousDevice)) {
            previousDeviceTV.setText(R.string.not_saved_bt_device);
            connectedDeviceName.setText("");
            reconnectButton.setEnabled(false);

        } else {
            previousDeviceTV.setText(R.string.saved_bt_device);
            connectedDeviceName.setText(EXTRAS_DEVICE_NAME);
        }

        reconnectButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View view) {
                connectingToOldDevice = true;
                connectToOldBleDevice();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View view) {
                connectingToOldDevice = false;
                connectToNewBleDevice();
            }
        });

        // Toolbar

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        //Create an instance of the tab layout from the view
//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
//
//        //Set the text for each tab
//        tabLayout.addTab(tabLayout.newTab().setText("Top Stories"));
//        tabLayout.addTab(tabLayout.newTab().setText("Tech News"));
//
//        //Set the tabs to fill the entire layout
//        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
//
//        //Use PagerAdapter to manage page views in fragments
//        //Each page is represented by its own fragment
//        //This is another example of the adapter pattern
//        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
//        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
//        viewPager.setAdapter(adapter);
//
//        //Setting a listener for clicks
//        viewPager.addOnPageChangeListener(new
//                TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                viewPager.setCurrentItem(tab.getPosition());
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {
//
//            }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//
//            }
//        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_select_bt);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connectToNewBleDevice() {
        if (EXTRAS_DEVICE_ADDRESS == null && EXTRAS_DEVICE_NAME == null) {
            Toast.makeText(getApplicationContext(),"No BT device selected", Toast.LENGTH_SHORT);
        } else {
            connectService();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connectToOldBleDevice() {
        if (EXTRAS_DEVICE_ADDRESS == null && EXTRAS_DEVICE_NAME == null) {
            Toast.makeText(getApplicationContext(),"No BT device saved", Toast.LENGTH_SHORT);
        } else {
            connectService();
        }
    }

    public void enableConnectButton() {
        connectButton.setEnabled(true);
    }

    public void startOrthoActivity() {
        final Intent intent = new Intent(this, StartOrthostaticTestActivity.class);
        intent.putExtra("bleDeviceName",EXTRAS_DEVICE_NAME);
        intent.putExtra("bleDeviceAddress",EXTRAS_DEVICE_ADDRESS);
        intent.putExtra(Constants.OrthostatTestType.ID,EXTRAS_ORTHOSTAT_TEST_TYPE);
        startActivity(intent);
    }

    //Show loading view on UI to indicate that BT connection attempt is ongoing
    private void startLoading() {
        connectToOldDeviceView.setVisibility(View.INVISIBLE);
        connectToNewDeviceView.setVisibility(View.INVISIBLE);
        connectButton.setEnabled(false);
        reconnectButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        progressBar.animate().setDuration(shortAnimTime).alpha(1);
    }

    private void stopLoading() {
        connectToOldDeviceView.setVisibility(View.VISIBLE);
        connectToNewDeviceView.setVisibility(View.VISIBLE);
        String savedName = sharedPref.getString("bleDeviceName",nonexistentPreviousDevice);
        if (savedName.equals(nonexistentPreviousDevice)) {
            reconnectButton.setEnabled(false);
        } else {
            reconnectButton.setEnabled(true);
        }
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.clearAnimation();
    }


    //Run bluetooth service to see if the device connects successfully
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connectService() {
        Log.v(TAG, "Starting to connect service");
        startLoading(); //start loading view on UI
        btFragment.onPause(); //pause scanning in BT fragment
        Intent serviceIntent = new Intent(this, BluetoothLeService.class);
        bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE); //bind bluetooth connection service
        mConnected = true;
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");

            }
            // Automatically connects to the device upon successful start-up initialization.
            boolean result = mBluetoothLeService.connect(EXTRAS_DEVICE_ADDRESS);
            Log.d(TAG, "BLE Connect request result = " + result);
        }


        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    //Broadcast receiver determining state of bluetooth connection

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.v(TAG, "BLE Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //If the device is out of range or cannot connect for some reason, unbind the connection service and prompt user to connect to a different device
                mConnected = false;
                Log.v(TAG, "BLE Disconnected");
                Toast.makeText(context,getString(R.string.problem_connecting) + " " + EXTRAS_DEVICE_NAME + getString(R.string.select_other_device), Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
                stopLoading();
                unbindService(mServiceConnection);
                mBluetoothLeService = null;
                EXTRAS_DEVICE_NAME = sharedPref.getString("bleDeviceName",nonexistentPreviousDevice);
                EXTRAS_DEVICE_ADDRESS = sharedPref.getString("bleDeviceAddress",nonexistentPreviousDevice);
                btFragment.onResume();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.v(TAG, "BLE Services Discovered");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //If data is successfully discovered, start the orthostatic test activity
                Log.v(TAG, "BLE Data Available");
                if (connectingToOldDevice) {
                    Log.v(TAG,"Old device successfully connected");
                    Toast.makeText(context,"Successfully reconnected to " + EXTRAS_DEVICE_NAME, Toast.LENGTH_SHORT).show();
                    startOrthoActivity();
                    stopLoading();
                } else {
                    Log.v(TAG,"New device successfully connected");
                    Toast.makeText(context,getString(R.string.successful_connection) + EXTRAS_DEVICE_NAME, Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("bleDeviceName", EXTRAS_DEVICE_NAME);
                    editor.putString("bleDeviceAddress", EXTRAS_DEVICE_ADDRESS);
                    editor.commit();
                    startOrthoActivity();
                    stopLoading();
                }

            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            // Find Heart Rate service (0x180D)
            if (SampleGattAttributes.lookup(uuid, "unknown").equals("Heart Rate Service")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                // Loops through available Characteristics
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    // Find Heart rate measurement (0x2A37)
                    if (SampleGattAttributes.lookup(uuid, "unknown").equals("Heart Rate Measurement")) {
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(EXTRAS_DEVICE_ADDRESS);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnected) {
            unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
    }

}
