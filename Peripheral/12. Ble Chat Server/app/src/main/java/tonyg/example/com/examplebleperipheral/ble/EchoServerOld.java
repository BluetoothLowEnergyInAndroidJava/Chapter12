package tonyg.example.com.examplebleperipheral.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import tonyg.example.com.examplebleperipheral.ble.callbacks.EchoServerCallback;

/**
 * This class creates a local Bluetooth Peripheral
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class EchoServerOld {
    /** Constants **/
    private static final String TAG = EchoServer.class.getSimpleName();

    /** Peripheral and GATT Profile **/
    public static final String ADVERTISING_NAME =  "EchoServer";
    public static final UUID PERIPHERAL_UUID = UUID.fromString("ca06ea56-9f42-4fc3-8b75-e31212c97123");

    public static final UUID SERVICE_UUID = UUID.fromString("0000180c-0000-1000-8000-00805f9b34fb");
    public static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("00002a56-0000-1000-8000-00805f9b34fb");
    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("00002a57-0000-1000-8000-00805f9b34fb");


    private static final int READ_CHARACTERISTIC_LENGTH = 20;
    private static final int WRITE_CHARACTERISTIC_LENGTH = 20;


    public static final UUID NOTIFY_DISCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /** Data formatting **/
    public static final String CHARSET = "ASCII";

    /** Advertising settings **/
    int mAdvertisingMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
    int mTransmissionPower = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;



    /** Callback Handlers **/
    public EchoServerCallback mEchoServerCallback;

    /** Bluetooth Stuff **/
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothAdvertiser;

    private BluetoothGattServer mGattServer;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mReadCharacteristic, mWriteCharacteristic;




    /**
     * Construct a new Peripheral
     *
     * @param context The Application Context
     * @param blePeripheralCallback The callback handler that interfaces with this Peripheral
     * @throws Exception Exception thrown if Bluetooth is not supported
     */
    public EchoServerOld(final Context context, EchoServerCallback blePeripheralCallback) throws Exception {
        mEchoServerCallback = blePeripheralCallback;

        // make sure Android device supports Bluetooth Low Energy
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new Exception("Bluetooth Not Supported");
        }

        // get a reference to the Bluetooth Manager class, which allows us to talk to talk to the BLE radio
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mGattServer = bluetoothManager.openGattServer(context, mGattServerCallback);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        setupDevice();
    }

    /**
     * Get the system Bluetooth Adapter
     *
     * @return BluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    /**
     * Set up the Advertising name and GATT profile
     */
    private void setupDevice() {
        // set the device name
        mBluetoothAdapter.setName(ADVERTISING_NAME);

        mService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mReadCharacteristic = new BluetoothGattCharacteristic(
                READ_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        mWriteCharacteristic = new BluetoothGattCharacteristic(
                WRITE_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        mService.addCharacteristic(mReadCharacteristic);
        mService.addCharacteristic(mWriteCharacteristic);


        mGattServer.addService(mService);

    }



    /**
     * Start Advertising
     *
     * @throws Exception Exception thrown if Bluetooth Peripheral mode is not supported
     */
    public void startAdvertising() throws Exception {
        // Beware: this function doesn't work on some systems
        /*
        if(!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            throw new Exception ("Peripheral mode not supported");
        }
        */

        mBluetoothAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        // Use this method instead for better support
        if (mBluetoothAdvertiser == null) {
            throw new Exception ("Peripheral mode not supported");
        }

        // Set up the Peripheral UUID
        ParcelUuid pUuid = new ParcelUuid(PERIPHERAL_UUID);

        // Build Advertise settings with transmission power and advertise speed
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(mAdvertisingMode)
                .setTxPowerLevel(mTransmissionPower)
                .setConnectable(true)
                .build();


        AdvertiseData.Builder advertiseBuilder = new AdvertiseData.Builder();
        // set advertising name
        advertiseBuilder.setIncludeDeviceName(true);

        // add Services
        //advertiseBuilder.addServiceUuid(pUuid);

        AdvertiseData advertiseData = advertiseBuilder.build();

        // begin advertising
        try {
            mBluetoothAdvertiser.startAdvertising( advertiseSettings, advertiseData, mAdvertiseCallback );
        } catch (Exception e) {
            Log.e(TAG, "could not start advertising");
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

    }


    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        if (mBluetoothAdvertiser != null) {
            mBluetoothAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }


    /**
     * Check if a Characetristic supports write permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * Check if a Characetristic supports write wuthout response permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritableWithoutResponse(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
    }


    /**
     * Check if a Characetristic supports write with permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritableWithResponse(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
    }

    /**
     * Check if a Characteristic supports Notifications
     *
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }


    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.v(TAG, "Connected");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mEchoServerCallback.onCentralConnected(device);
                    stopAdvertising();


                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mEchoServerCallback.onCentralDisconnected(device);
                    try {
                        startAdvertising();
                    } catch (Exception e) {
                        Log.e(TAG, "error starting advertising");
                    }
                }
            }

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));


            mEchoServerCallback.onMessageWritten(value);

            if (isCharacteristicWritableWithResponse(characteristic)) {
                characteristic.setValue(value);
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
            }

            // set the Read characteristic to the current written value
            mReadCharacteristic.setValue(value);
            /*
            if (isCharacteristicNotifiable(mReadCharacteristic)) {
                mGattServer.notifyCharacteristicChanged(device, mReadCharacteristic, true);
            }
            */

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);

            // determine which Characteristic is being requested
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

            //characteristic.getDescriptor(NOTIFY_DISCRIPTOR_UUID).setValue(value);


            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);


        }
    };


    public AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Advertising started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.v(TAG, "advertising failed");
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "Failed to start advertising as the advertising is already started.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "This feature is not supported on this platform.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "Operation failed due to an internal error.");
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG, "Failed to start advertising because no advertising instance is available.");
                    break;
                default:
                    Log.e(TAG, "unknown problem");
            }
        }
    };


}
