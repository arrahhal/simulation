import java.util.UUID;

public class Device {
    DeviceStatus status;
    boolean isAlwaysConnected;
    int wattUsage;
    String id;

    Device() {
        this.isAlwaysConnected = false;
        this.status = DeviceStatus.DISCONNECTED;
        this.id = UUID.randomUUID().toString();
    }

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