import java.util.UUID;

public class Device {
    DeviceStatus status;
    boolean isAlwaysConnected;
    int wattUsage;
    String id;

    Device(int wattUsage) {
        this.isAlwaysConnected = false;
        this.status = DeviceStatus.DISCONNECTED;
        this.id = UUID.randomUUID().toString();
        this.wattUsage = wattUsage;
    }

    public void connect() {
        this.status = DeviceStatus.CONNECTED;
    }
    public void disconnect() {
        if (!this.isAlwaysConnected)
            this.status = DeviceStatus.DISCONNECTED;
    }
    public void onhold() {
        this.status = DeviceStatus.ONHOLD;
    }
}