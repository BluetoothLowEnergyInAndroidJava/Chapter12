package tonyg.example.com.examplebleperipheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import tonyg.example.com.examplebleperipheral.ble.EchoServer;
import tonyg.example.com.examplebleperipheral.ble.callbacks.EchoServerCallback;
import tonyg.example.com.examplebleperipheral.utilities.DataConverter;


/**
 * Create a Bluetooth Peripheral.  Android 5 required
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class MainActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /** Bluetooth Stuff **/
    private EchoServer mEchoServer;

    /** UI Stuff **/
    private TextView mAdvertisingNameTV, mCharacteristicLogTV;
    private Switch mBluetoothOnSwitch,
            mCentralConnectedSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // notify when bluetooth is turned on or off
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBleBroadcastReceiver, filter);


        loadUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        // stop advertising when the activity pauses
        mEchoServer.stopAdvertising();
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeBluetooth();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBleBroadcastReceiver);
    }


    /**
     * Load UI components
     */
    public void loadUI() {
        mAdvertisingNameTV = (TextView)findViewById(R.id.advertising_name);
        mCharacteristicLogTV = (TextView)findViewById(R.id.characteristic_log);
        mBluetoothOnSwitch = (Switch)findViewById(R.id.bluetooth_on);
        mCentralConnectedSwitch = (Switch)findViewById(R.id.central_connected);

        mAdvertisingNameTV.setText(EchoServer.ADVERTISING_NAME);
    }

    /**
     * Initialize the Bluetooth Radio
     */
    public void initializeBluetooth() {
        // reset connection variables
        try {
            mEchoServer = new EchoServer(this, mBlePeripheralCallback);
        } catch (Exception e) {
            Log.e(TAG, "Could not initialize bluetooth");
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            finish();
        }

        Log.v(TAG, "bluetooth switch: "+mBluetoothOnSwitch);
        Log.v(TAG, "mEchoServer: "+mEchoServer);
        Log.v(TAG, "peripheral: "+mEchoServer.getBlePeripheral());

        mBluetoothOnSwitch.setChecked(mEchoServer.getBlePeripheral().getBluetoothAdapter().isEnabled());

        // should prompt user to open settings if Bluetooth is not enabled.
        if (!mEchoServer.getBlePeripheral().getBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startAdvertising();
        }

    }

    /**
     * Start advertising Peripheral
     */
    public void startAdvertising() {
        Log.v(TAG, "starting advertising...");
        try {
            mEchoServer.startAdvertising();
        } catch (Exception e) {
            Log.e(TAG, "problem starting advertising");
        }
    }


    /**
     * Event trigger when Central has connected
     *
     * @param bluetoothDevice
     */
    public void onBleCentralConnected(final BluetoothDevice bluetoothDevice) {
        mCentralConnectedSwitch.setChecked(true);
    }

    /**
     * Event trigger when Central has disconnected
     * @param bluetoothDevice
     */
    public void onBleCentralDisconnected(final BluetoothDevice bluetoothDevice) {
        mCentralConnectedSwitch.setChecked(false);
    }

    /**
     * Event trigger when Characteristic has been written to
     *
     * @param value the byte value being written
     */
    public void onBleMessageWritten(final byte[] value) {
        mCharacteristicLogTV.append("\n");
        try {
            mCharacteristicLogTV.append(new String(value, EchoServer.CHARSET));
        } catch (Exception e) {
            Log.e(TAG, "error converting byte array to string");
        }

        // scroll to bottom of TextView
        final int scrollAmount = mCharacteristicLogTV.getLayout().getLineTop(mCharacteristicLogTV.getLineCount()) - mCharacteristicLogTV.getHeight();
        if (scrollAmount > 0) {
            mCharacteristicLogTV.scrollTo(0, scrollAmount);
        } else {
            mCharacteristicLogTV.scrollTo(0, 0);
        }
    }

    /**
     * When the Bluetooth radio turns on, initialize the Bluetooth connection
     */
    private final BroadcastReceiver mBleBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.v(TAG, "Bluetooth turned off");
                        initializeBluetooth();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.v(TAG, "Bluetooth turned on");
                        startAdvertising();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    /**
     * Respond to changes to the Bluetooth Peripheral state
     */
    private final EchoServerCallback mBlePeripheralCallback = new EchoServerCallback() {

        public void onCentralConnected(final BluetoothDevice bluetoothDevice) {
            Log.v(TAG, "Central connected");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleCentralConnected(bluetoothDevice);
                }
            });

        }
        public void onCentralDisconnected(final BluetoothDevice bluetoothDevice) {
            Log.v(TAG, "Central disconnected");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleCentralDisconnected(bluetoothDevice);
                }
            });

        }
        public void onMessageWritten(final byte[] value) {
            Log.v(TAG, "Characteristic written: "+ DataConverter.bytesToHex(value));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBleMessageWritten(value);
                }
            });

        }
    };
}
