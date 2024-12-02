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

    // store on hold devices indexes from oldest to newest
    List<Device> onHoldDevices = new ArrayList<Device>();

    int totalWattUsed = 0;
    int wattWasted = 0;
    int wattNeeded = 0;
    double simTime = 0.0;

    int numOfDevices = 0;

    public void input() throws IOException {
        BufferedReader inFile = new BufferedReader(new FileReader("data/solar.in"));

        String[] params = inFile.readLine().trim().split("\\s+");
        for (int idx = 0; idx++ < params.length; idx++) {
            int param = Integer.parseInt(params[idx]);
            if (idx == 0) {
                solarInput = param;
            }
            else if (idx == 1) {
                numOfDevices = param;
            } else if (idx <= 1 + numOfDevices) {
                Device device = new Device();
                devices.add(device);
            } else {
                int index = idx - numOfDevices + 2;
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
        for (Device d : onHoldDevices) {
            if (d.status == DeviceStatus.ONHOLD) {
                if (d.wattUsage + totalWattUsed <= solarInput) {
                    onHoldDevices.remove(d);
                    d.connect();
                    return true;
                }
            }
        }
        return false;
    }

    private void connectingEvent() {
        if (connectingOnHoldEvent()) return;

        List<Device> disconnectedDevices = devices.stream()
                .filter(d -> d.status == DeviceStatus.DISCONNECTED)
                .toList();
        if (disconnectedDevices.isEmpty()) return;

        Device randomDevice = disconnectedDevices.get(rand.nextInt(disconnectedDevices.size()));
        if (totalWattUsed + randomDevice.wattUsage <= solarInput) {
            randomDevice.connect();
            totalWattUsed += randomDevice.wattUsage;
        } else {
            // NOTE: i should handle on hold exceeding MAX_ONHOLD
            randomDevice.onhold();
            onHoldDevices.add(randomDevice);
        }
    }

    private void disconnectingEvent() {
        List<Device> connectedDevices = devices.stream()
                .filter(d -> d.status == DeviceStatus.CONNECTED)
                .toList();

        List<Device> notConnectedDevices = new ArrayList<Device>(connectedDevices);
        notConnectedDevices.addAll(onHoldDevices);

        if (notConnectedDevices.isEmpty()) return;
        Device randomDevice = notConnectedDevices.get(rand.nextInt(notConnectedDevices.size()));

        if (randomDevice.status == DeviceStatus.CONNECTED && !randomDevice.isAlwaysConnected){
            totalWattUsed -= randomDevice.wattUsage;
        }
        randomDevice.disconnect();
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
