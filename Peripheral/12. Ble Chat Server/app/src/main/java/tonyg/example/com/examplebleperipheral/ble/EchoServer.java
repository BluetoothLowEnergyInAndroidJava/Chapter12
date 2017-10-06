package tonyg.example.com.examplebleperipheral.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import tonyg.example.com.examplebleperipheral.ble.callbacks.BlePeripheralCallback;
import tonyg.example.com.examplebleperipheral.ble.callbacks.EchoServerCallback;


/**
 * This class creates a local Bluetooth Peripheral
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class EchoServer {
    /** Constants **/
    private static final String TAG = EchoServer.class.getSimpleName();

    public static final String CHARSET = "ASCII";

    private static final String MODEL_NUMBER = "1AB2";
    private static final String SERIAL_NUMBER = "1234";

    /** Peripheral and GATT Profile **/
    public static final String ADVERTISING_NAME =  "EchoServer";

    public static final UUID SERVICE_UUID = UUID.fromString("0000180c-0000-1000-8000-00805f9b34fb");
    public static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("00002a56-0000-1000-8000-00805f9b34fb");
    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("00002a57-0000-1000-8000-00805f9b34fb");


    private static final int READ_CHARACTERISTIC_LENGTH = 20;
    private static final int WRITE_CHARACTERISTIC_LENGTH = 20;



    /** Callback Handlers **/
    public EchoServerCallback mEchoServerCallback;

    /** Bluetooth Stuff **/
    private BlePeripheral mBlePeripheral;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mReadCharacteristic, mWriteCharacteristic;




    /**
     * Construct a new Peripheral
     *
     * @param context The Application Context
     * @param blePeripheralCallback The callback handler that interfaces with this Peripheral
     * @throws Exception Exception thrown if Bluetooth is not supported
     */
    public EchoServer(final Context context, EchoServerCallback blePeripheralCallback) throws Exception {
        mEchoServerCallback = blePeripheralCallback;

        mBlePeripheral = new BlePeripheral(context, mBlePeripheralCallback);

        setupDevice();
    }


    /**
     * Set up the Advertising name and GATT profile
     */
    private void setupDevice() throws Exception {
        mBlePeripheral.setModelNumber(MODEL_NUMBER);
        mBlePeripheral.setSerialNumber(SERIAL_NUMBER);

        mBlePeripheral.setupDevice();

        mService = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mReadCharacteristic = new BluetoothGattCharacteristic(
                READ_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        // add Notification support to Characteristic
        BluetoothGattDescriptor notifyDescriptor = new BluetoothGattDescriptor(BlePeripheral.NOTIFY_DESCRIPTOR_UUID, BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        mReadCharacteristic.addDescriptor(notifyDescriptor);


        mWriteCharacteristic = new BluetoothGattCharacteristic(
                WRITE_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        mService.addCharacteristic(mReadCharacteristic);
        mService.addCharacteristic(mWriteCharacteristic);


        mBlePeripheral.addService(mService);
    }



    /**
     * Start Advertising
     *
     * @throws Exception Exception thrown if Bluetooth Peripheral mode is not supported
     */
    public void startAdvertising() throws Exception {
        // set the device name
        mBlePeripheral.setPeripheralAdvertisingName(ADVERTISING_NAME);

        mBlePeripheral.startAdvertising();
    }


    /**
     * Stop advertising
     */
    public void stopAdvertising() {
        mBlePeripheral.stopAdvertising();
    }

    /**
     * Get the BlePeripheral
     */
    public BlePeripheral getBlePeripheral() {
        return mBlePeripheral;
    }


    private BlePeripheralCallback mBlePeripheralCallback = new BlePeripheralCallback() {
        @Override
        public void onAdvertisingStarted() {

        }

        @Override
        public void onAdvertisingFailed(int errorCode) {

        }

        @Override
        public void onAdvertisingStopped() {

        }

        @Override
        public void onCentralConnected(BluetoothDevice bluetoothDevice) {
            mEchoServerCallback.onCentralConnected(bluetoothDevice);
        }

        @Override
        public void onCentralDisconnected(BluetoothDevice bluetoothDevice) {
            mEchoServerCallback.onCentralDisconnected(bluetoothDevice);
        }

        @Override
        public void onCharacteristicWritten(BluetoothDevice connectedDevice, BluetoothGattCharacteristic characteristic, byte[] value) {
            // copy value to the read Characteristic
            mReadCharacteristic.setValue(value);
            Log.v(TAG, "setting readCharacteristic: "+ Arrays.toString(value));
            // send a notification
            mBlePeripheral.getGattServer().notifyCharacteristicChanged(connectedDevice, mReadCharacteristic, true);
            mEchoServerCallback.onMessageWritten(value);
        }

        @Override
        public void onCharacteristicSubscribedTo(BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onCharacteristicUnsubscribedFrom(BluetoothGattCharacteristic characteristic) {

        }
    };
}
