public class Device {
    DeviceStatus status;
    boolean isAlwaysConnected;
    int wattUsage;

    public void connect() {
        this.status = DeviceStatus.CONNECTED;
    }
    public void disconnect() {
        this.status = DeviceStatus.DISCONNECTED;
    }
    public void onhold() {
        this.status = DeviceStatus.ONHOLD;
    }
}