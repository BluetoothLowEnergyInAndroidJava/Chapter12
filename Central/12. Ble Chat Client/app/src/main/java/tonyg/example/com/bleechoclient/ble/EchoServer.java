package tonyg.example.com.bleechoclient.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.UUID;

import tonyg.example.com.bleechoclient.ble.callbacks.EchoServerCallback;

/**
 * This class allows us to share Bluetooth resources
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class EchoServer {
    private static final String TAG = EchoServer.class.getSimpleName();

    public static final String CHARACTER_ENCODING = "ASCII";

    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private EchoServerCallback mEchoServerCallback;
    private BluetoothGattCharacteristic mReadCharacteristic, mWriteCharacteristic;

    /** Bluetooth Device stuff **/
    public static final String BROADCAST_NAME = "EchoServer";
    public static final UUID SERVICE_UUID = UUID.fromString("0000180c-0000-1000-8000-00805f9b34fb");
    public static final UUID READ_CHARACTERISTIC_UUID = UUID.fromString("00002a56-0000-1000-8000-00805f9b34fb");
    public static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("00002a57-0000-1000-8000-00805f9b34fb");

    public static final UUID NOTIFY_DISCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context mContext;

    /** Flow control stuff **/
    private int mNumPacketsTotal;
    private int mNumPacketsSent;
    private int mCharacteristicLength = 20;
    private String mQueuedCharactersticValue;

    /**
     * Create a new EchoServer
     *
     * @param context the Activity context
     * @param echoServerCallback the EchoServerCallback
     */
    public EchoServer(Context context, EchoServerCallback echoServerCallback) {
        mContext = context;
        mEchoServerCallback = echoServerCallback;
    }

    /**
     * Connect to a Peripheral
     *
     * @param bluetoothDevice the Bluetooth Device
     * @return a connection to the BluetoothGatt
     * @throws Exception if no device is given
     */
    public BluetoothGatt connect(BluetoothDevice bluetoothDevice) throws Exception {
        if (bluetoothDevice == null) {
            throw new Exception("No bluetooth device provided");
        }
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);
        //refreshDeviceCache();
        return mBluetoothGatt;
    }

    /**
     * Disconnect from a Peripheral
     */
    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * A connection can only close after a successful disconnect.
     * Be sure to use the BluetoothGattCallback.onConnectionStateChanged event
     * to notify of a successful disconnect
     */
    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close(); // close connection to Peripheral
            mBluetoothGatt = null; // release from memory
        }
    }
    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    /**
     * Write next packet in queue if necessary
     *
     * @param value
     * @return the value being written
     * @throws Exception
     */
    public String processIncomingMessage(String value)  throws Exception {

        if (morePacketsAvailableInQueue()) {
            // I honestly don't know why, but this sends too quickly for client to process
            // so we have to delay it
            //final Handler handler = new Handler(Looper.getMainLooper());
            //handler.postDelayed(new Runnable() {
            // @Override
            //public void run() {
            try {
                writePartialValue(mQueuedCharactersticValue, mNumPacketsSent);
            } catch (Exception e) {
                Log.d(TAG, "Unable to send next chunk of message");
            }
            //}
            //}, 100);
        }

        return value;
    }

    /**
     * Clear the GATT Service cache.
     *
     * New in this chapter
     *
     * @return <b>true</b> if the device cache clears successfully
     * @throws Exception
     */
    public boolean refreshDeviceCache() throws Exception {
        Method localMethod = mBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
        if (localMethod != null) {
            return ((Boolean) localMethod.invoke(mBluetoothGatt, new Object[0])).booleanValue();
        }

        return false;
    }

    /**
     * Request a data/value read from a Ble Characteristic
     */
    public void readValue() {
        // Reading a characteristic requires both requesting the read and handling the callback that is
        // sent when the read is successful
        // http://stackoverflow.com/a/20020279
        mBluetoothGatt.readCharacteristic(mReadCharacteristic);
    }

    /**
     * Write a value to the Characteristic
     *
     * @param value
     * @throws Exception
     */
    public void writeValue(String value) throws Exception {
        // reset the queue counters, prepare the message to be sent, and send the value to the Characteristic
        mQueuedCharactersticValue = value;
        mNumPacketsSent = 0;
        byte[] byteValue = value.getBytes();
        mNumPacketsTotal = (int) Math.ceil((float) byteValue.length / mCharacteristicLength);
        writePartialValue(value, mNumPacketsSent);

    }

    /**
     * Subscribe or unsubscribe from Characteristic Notifications
     *
     * @param characteristic
     * @param enabled <b>true</b> for "subscribe" <b>false</b> for "unsubscribe"
     */
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        // modified from http://stackoverflow.com/a/18011901/5671180
        // This is a 2-step process
        // Step 1: set the Characteristic Notification parameter locally
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        // Step 2: Write a descriptor to the Bluetooth GATT enabling the subscription on the Perpiheral
        // turns out you need to implement a delay between setCharacteristicNotification and setvalue.
        // maybe it can be handled with a callback, but this is an easy way to implement
        Log.v(TAG, "characteristic: "+characteristic);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFY_DISCRIPTOR_UUID);
                Log.v(TAG, "descriptor: "+descriptor);
                if (enabled) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }, 10);
    }

    /**
     * Write a portion of a larger message to a Characteristic
     *
     * @param message The message being written
     * @param offset The current packet index in queue to be written
     * @throws Exception
     */
    public void writePartialValue(String message, int offset) throws Exception {
        byte[] temp = message.getBytes();

        mNumPacketsTotal = (int) Math.ceil((float) temp.length / mCharacteristicLength);
        int remainder = temp.length % mCharacteristicLength;

        int dataLength = mCharacteristicLength;
        if (offset >= mNumPacketsTotal) {
            dataLength = remainder;
        }

        byte[] packet = new byte[dataLength];
        for (int localIndex = 0; localIndex < packet.length; localIndex++) {
            int index = (offset * dataLength) + localIndex;
            if (index < temp.length) {
                packet[localIndex] = temp[index];
            } else {
                packet[localIndex] = 0x00;
            }
        }

        // a simpler way to write this might be:
        //System.arraycopy(getCurrentMessage().getBytes(), getCurrentOffset()*mCharacteristicLength, chunk, 0, mCharacteristicLength);
        //chunk[dataLength] = 0x00;

        Log.v(TAG, "Writing message: '" + new String(packet, "ASCII") + "' to " + mWriteCharacteristic.getUuid().toString());
        mWriteCharacteristic.setValue(packet);
        mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
        mNumPacketsSent++;
    }

    /**
     * Determine if a message has been completely wirtten to a Characteristic or if more data is in queue
     *
     * @return <b>false</b> if all of a message is has been written to a Characteristic, <b>true</b> otherwise
     */
    public boolean morePacketsAvailableInQueue() {
        boolean morePacketsAvailable = mNumPacketsSent < mNumPacketsTotal;
        Log.v(TAG, mNumPacketsSent + " of " + mNumPacketsTotal + " packets sent: "+morePacketsAvailable);
        return morePacketsAvailable;
    }

    /**
     * Determine how much of a message has been written to a Characteristic
     *
     * @return integer representing how many packets have been written so far to Characteristic
     */
    public int getCurrentOffset() {
        return mNumPacketsSent;
    }


    /**
     * Get the current message being written to a Characterstic
     *
     * @return the message in queue for writing to a Characteristic
     */
    public String getCurrentMessage() {
        return mQueuedCharactersticValue;
    }

    // http://stackoverflow.com/a/21300916/5671180
    // more options available at:
    // http://www.programcreek.com/java-api-examples/index.php?class=android.bluetooth.BluetoothGattCharacteristic&method=PROPERTY_NOTIFY

    /**
     * Check if a Characetristic supports write permissions
     * @return Returns <b>true</b> if property is writable
     */
    public static boolean isCharacteristicWritable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0;
    }

    /**
     * Check if a Characetristic has read permissions
     *
     * @return Returns <b>true</b> if property is Readable
     */
    public static boolean isCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    /**
     * Check if a Characteristic supports Notifications
     *
     * @return Returns <b>true</b> if property is supports notification
     */
    public static boolean isCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
    }

    /**
     *  Handle changes to connection and GATT profile
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // read more at http://developer.android.com/guide/topics/connectivity/bluetooth-le.html#notification
                final byte[] data = characteristic.getValue();

                String message = "";
                try {
                    message = new String(data, CHARACTER_ENCODING);
                } catch (Exception e) {
                    Log.d(TAG, "Could not convert message byte array to String");
                }

                Log.d(TAG, "received: "+message);

                final String messageText = message;

                mEchoServerCallback.messageReceived(messageText);

                try {
                    processIncomingMessage(message);
                } catch (Exception e) {
                    Log.d(TAG, "Could not send next message part");
                }

            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "characteristic written");
                mEchoServerCallback.messageSent();


            } else {
                Log.d(TAG, "problem writing characteristic");
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            readValue();

        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt bluetoothGatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device");

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mEchoServerCallback.disconnected();
                Log.d(TAG, "Disconnected from device");

                disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt bluetoothGatt, int status) {
            Log.d(TAG, "SERVICE DISCOVERED!: ");

            // if services were discovered, then let's iterate through them and display them on screen
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // check if there are matching services and characteristics
                BluetoothGattService service = bluetoothGatt.getService(EchoServer.SERVICE_UUID);
                if (service != null) {
                    Log.d(TAG, "service found");
                    mReadCharacteristic = service.getCharacteristic(EchoServer.READ_CHARACTERISTIC_UUID);
                    mWriteCharacteristic = service.getCharacteristic(EchoServer.WRITE_CHARACTERISTIC_UUID);

                    Log.v(TAG, "read descriptors: ");
                    for (BluetoothGattDescriptor descriptor : mReadCharacteristic.getDescriptors()){
                        Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                    }

                    Log.v(TAG, "write descriptors: ");
                    for (BluetoothGattDescriptor descriptor : mWriteCharacteristic.getDescriptors()){
                        Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                    }


                    if (isCharacteristicNotifiable(mReadCharacteristic)) {
                        setCharacteristicNotification(mReadCharacteristic, true);
                    }
                }
            } else {
                Log.d(TAG, "Something went wrong while discovering GATT services from this device");
            }

            mEchoServerCallback.connected();
        }
    };
}
