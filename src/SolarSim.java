import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SolarSim {
    final int MAX_ON_HOLD = 10;
    final double EVENT_INTERVAL = 30;
    final Random rand = new Random();

    int solarInput;
    List<Device> devices = new ArrayList<Device>();

    int totalWattUsed = 0;
    int wattWasted = 0;
    int wattNeeded = 0;
    double simTime = 0.0;

    public void input() throws IOException {
        BufferedReader inFile = new BufferedReader(new FileReader("data/solar.in"));
        int numberOfDevices = 0;

        String[] params = inFile.readLine().trim().split("\\s+");
        for (int idx = 0; idx++ < params.length; idx++) {
            int param = Integer.parseInt(params[idx]);
            if (idx == 0) {
                solarInput = param;
            }
            else if (idx == 1) {
                numberOfDevices = param;
            } else if (idx <= 1 + numberOfDevices) {
                Device device = new Device();
                devices.add(device);
            } else {
                int index = idx - numberOfDevices + 2;
                if (param == 1) {
                    devices.get(index).connect();
                }
            }

        }
        inFile.close();
    }

    public int currentWatt() {
        int total = 0;
        for (Device device : devices) {
            total += device.wattUsage;
        }
        return total;
    }

    public void report() throws IOException {
        BufferedWriter outFile = new BufferedWriter(new FileWriter("data/solar.out"));
        // Solar Input: 1000 watts
        //Number of Devices: 7
        //Device Power Consumption: 200 200 100 400 200 300 500
        //Always Connected: 1 1 0 0 0 0 1

        outFile.write("Simulation Report\n" +
                "Overload (Watts): " + wattNeeded + "\n" +
                "Wasted Solar Input (Watts): " + wattWasted + "\n");

        outFile.close();
    }

    public void run() throws IOException {
        while (simTime < 24 * 60) {
            handleEvent();
            update();
        }
        report();
    }

    private void handleEvent() {
        simTime += expo(EVENT_INTERVAL);
        boolean isConnectEvent = rand.nextBoolean();

        if (isConnectEvent) {
            connectingEvent();
        } else {
            disconnectingEvent();
        }
    }

    private boolean connectingOnHoldEvent() {
        for (Device device : devices.stream()
                .filter(d -> d.status == DeviceStatus.ONHOLD)
                .toList()) {
                if (totalWattUsed + device.wattUsage <= solarInput) {
                    device.connect();
                    totalWattUsed += device.wattUsage;
                    return true;
                }
        }
        return false;
    }

    private void connectingEvent() {
        if (connectingOnHoldEvent()) return;
        for (Device device : devices.stream()
                .filter(d -> d.status == DeviceStatus.DISCONNECTED)
                .toList()) {
                if (totalWattUsed + device.wattUsage <= solarInput) {
                    device.connect();
                    totalWattUsed += device.wattUsage;
                } else {
                    // NOTE: i should handle on hold exceeding MAX_ONHOLD
                    device.onhold();
                }
                break;
            }
        }

    private void disconnectingEvent() {
        for (Device device : devices.stream()
                .filter(d -> d.status == DeviceStatus.DISCONNECTED)
                .toList()) {
            if (device.status != DeviceStatus.DISCONNECTED && !device.isAlwaysConnected) {
                if (device.status == DeviceStatus.CONNECTED) {
                    totalWattUsed -= device.wattUsage;
                }
                device.disconnect();
                break;
            }
        }
    }

    private void update() {
        if (totalWattUsed > solarInput) {
            wattNeeded += totalWattUsed - solarInput;
        } else {
            wattWasted += solarInput - totalWattUsed;
        }
    }

    private double expo(double mean) {
        return -mean * Math.log(1 - rand.nextDouble());
    }

    public static void main(String[] args) {
        try {
            new SolarSim().run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
