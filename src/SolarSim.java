import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SolarSim {
    final double EVENT_INTERVAL = 30;
    final Random rand = new Random();

    int solarInput;
    List<Device> devices = new ArrayList<>();

    // store on hold devices indexes from oldest to newest
    List<Device> onHoldDevices = new ArrayList<>();

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
                d.isAlwaysConnected = true;
                if (d.wattUsage + currentWatt() <= solarInput) {
                    d.connect();
                } else {
                    d.onhold();
                    onHoldDevices.add(d);
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
                "Average Overload (Watts): " + wattNeeded / 24+ "\n" +
                "Average Wasted Solar Input (Watts): " + wattWasted / 24 + "\n");

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

    private void connectingOnHoldEvent() {
        Iterator<Device> iterator = onHoldDevices.iterator();
        while (iterator.hasNext()) {
            Device d = iterator.next();
            if (d.status == DeviceStatus.ONHOLD) {
                if (d.wattUsage + currentWatt() <= solarInput) {
                    d.connect();
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }

    private void connectingEvent() {
        List<Device> disconnectedDevices = devices.stream()
                .filter(d -> d.status == DeviceStatus.DISCONNECTED)
                .toList();
        if (disconnectedDevices.isEmpty()) return;

        Device randomDevice = disconnectedDevices.get(rand.nextInt(disconnectedDevices.size()));
        if (currentWatt() + randomDevice.wattUsage <= solarInput) {
            randomDevice.connect();
        } else {
            randomDevice.onhold();
            onHoldDevices.add(randomDevice);
        }
    }

    private void disconnectingEvent() {
        List<Device> connectedDevices = devices.stream()
                .filter(d -> d.status == DeviceStatus.CONNECTED)
                .toList();

        List<Device> notConnectedDevices = new ArrayList<>(connectedDevices);
        notConnectedDevices.addAll(onHoldDevices);

        if (notConnectedDevices.isEmpty()) return;
        Device randomDevice = notConnectedDevices.get(rand.nextInt(notConnectedDevices.size()));

        randomDevice.disconnect();
}

    private void update() {
        if (!onHoldDevices.isEmpty()) {
            int overload = onHoldDevices.stream().mapToInt(d -> d.wattUsage).sum();
            wattNeeded += overload;
        }
        else {
            wattWasted += solarInput - currentWatt();
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
