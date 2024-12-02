import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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

        solarInput = Integer.parseInt(params[0]);
        numOfDevices = Integer.parseInt(params[1]);
        for (int idx = 2; idx < numOfDevices + 2; idx++) {
            int wattUsage = Integer.parseInt(params[idx]);
            Device device = new Device(wattUsage);
            devices.add(device);
        }

        for (int idx = numOfDevices + 2; idx < params.length; idx++) {
            if (params[idx].equals( "1")) {
                Device d = devices.get(idx - (numOfDevices + 2));
                if (d.wattUsage + currentWatt() <= solarInput) {
                    d.connect();
                }
            }
        }
        inFile.close();
    }

    public int currentWatt() {
        int used = 0;
        for (Device device : devices) {
            if (device.status == DeviceStatus.CONNECTED) {
                used += device.wattUsage;
            }
        }
        return used;
    }

    public void report() throws IOException {
        BufferedWriter outFile = new BufferedWriter(new FileWriter("data/solar.out"));
        outFile.write("Solar Input: " + solarInput + " watts\n" +
                "Number of Devices: " + numOfDevices + "\n" +
                "Device Power Consumption: " +
                devices.stream().map(d -> String.valueOf(d.wattUsage)).collect(Collectors.joining(" ")) + "\n" +
                "Always Connected: " +
                devices.stream().map(d -> String.valueOf(d.isAlwaysConnected)).collect(Collectors.joining(" ")) + "\n\n");

        outFile.write("Simulation Report\n" +
                "Overload (Watts): " + wattNeeded + "\n" +
                "Wasted Solar Input (Watts): " + wattWasted + "\n");

        outFile.close();
    }

    public void run() throws IOException {
        input();
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
            connectingOnHoldEvent();
        }
    }

    private boolean connectingOnHoldEvent() {
        boolean deviceConnected = false;
        Iterator<Device> iterator = onHoldDevices.iterator();
        while (iterator.hasNext()) {
            Device d = iterator.next();
            if (d.status == DeviceStatus.ONHOLD) {
                if (d.wattUsage + currentWatt() <= solarInput) {
                    d.connect();
                    iterator.remove();
                    deviceConnected = true;
                }
            } else {
                iterator.remove();
            }
        }
        return deviceConnected;
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
        if (!onHoldDevices.isEmpty()) {
            int overload = onHoldDevices.stream().mapToInt(d -> d.wattUsage).sum();
            wattNeeded += overload;
        }
        else {
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
