package tonyg.example.com.bleechoclient.ble.callbacks;

/**
 * Relay state changes from Echo Server
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public abstract class EchoServerCallback {
    /**
     * Echo Server connected
     */
    public abstract void connected();

    /**
     * Echo Server disconnected
     */
    public abstract void disconnected();

    /**
     * Message sent to Echo Server
     */

    public abstract void messageSent();

    /**
     * Message received from Echo Server
     *
     * @param messageText the incoming text
     */
    public abstract void messageReceived(final String messageText);
}
