package tonyg.example.com.examplebleperipheral.ble.callbacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * BlePeripheralCallback has callbacks to support notifications
 * related to advertising and data transmission
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-18
 */
public abstract class BlePeripheralCallback {

    /**
     * Advertising Started
     */
    public abstract void onAdvertisingStarted();

    /**
     * Advertising Could not Start
     */
    public abstract void onAdvertisingFailed(int errorCode);

    /**
     * Advertising Stopped
     */
    public abstract void onAdvertisingStopped();

    /**
     * Central Connected
     *
     * @param bluetoothDevice the BluetoothDevice representing the connected Central
     */
    public abstract void onCentralConnected(final BluetoothDevice bluetoothDevice);

    /**
     * Central Disconnected
     *
     * @param bluetoothDevice the BluetoothDevice representing the disconnected Central
     */
    public abstract void onCentralDisconnected(final BluetoothDevice bluetoothDevice);

    /**
     * Characteristic written to
     *
     * @param characteristic The Characteristic that was written to
     * @param value the byte value that was written
     */
    public abstract void onCharacteristicWritten(final BluetoothDevice connectedDevice, final BluetoothGattCharacteristic characteristic, final byte[] value);

    /**
     * Characteristic subscribed to
     *
     * @param characteristic The Characteristic that was subscribed to
     */
    public abstract void onCharacteristicSubscribedTo(final BluetoothGattCharacteristic characteristic);

    /**
     * Characteristic unsubscribed from
     *
     * @param characteristic The Characteristic that was unsubscribed from
     */
    public abstract void onCharacteristicUnsubscribedFrom(final BluetoothGattCharacteristic characteristic);
}
