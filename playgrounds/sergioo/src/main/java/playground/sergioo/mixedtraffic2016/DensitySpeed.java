package playground.sergioo.mixedtraffic2016;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sergioo on 1/2/17.
 */
public class DensitySpeed implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {

    private static class Mode {
        double lenght;
        double width;
        double speed;
        public Mode(double lenght, double width, double speed) {
            super();
            this.lenght = lenght;
            this.width = width;
            this.speed = speed;
        }
    }
    private static class VehicleInfo {
        double inTime;
        boolean inBuffer;
        String mode;
        int total;
        int numBuffer;
        int numBufferB=0;
        public VehicleInfo(double inTime, String mode, int total, int numBuffer) {
            this.inTime = inTime;
            this.inBuffer = false;
            this.mode = mode;
            this.total = total;
            this.numBuffer = numBuffer;
        }
        private void checkBuffer(double time, double speed, double length) {
            if(speed*(time-inTime)>=length) {
                for(VehicleInfo vehicleInfo:vehicles.values())
                    if(vehicleInfo.inBuffer)
                        numBufferB++;
                inBuffer = true;
            }
        }
    }

    private final Map<String, Mode> modes = new HashMap<>();
    private final Link link;
    private Map<Id<Vehicle>, String> modeVehicles = new HashMap<>();
    private static Map<Id<Vehicle>, VehicleInfo> vehicles = new HashMap<>();
    private Collection<double[]> points = new ArrayList<>();

    public DensitySpeed(Collection<VehicleType> vehicleTypes, Link link) {
        for(VehicleType vehicleType:vehicleTypes)
            modes.put(vehicleType.getId().toString(), new Mode(vehicleType.getLength(), vehicleType.getWidth(), vehicleType.getMaximumVelocity()));
        this.link = link;
    }
    @Override
    public void handleEvent(LinkEnterEvent event) {
        for (VehicleInfo vehicleInfo : vehicles.values())
            vehicleInfo.checkBuffer(event.getTime(), modes.get(vehicleInfo.mode).speed, link.getLength());
        if (event.getLinkId().equals(link.getId())) {
            VehicleInfo vehicleInfo = vehicles.remove(event.getVehicleId());
            points.add(new double[]{link.getLength()/(event.getTime() - vehicleInfo.inTime), vehicleInfo.total, vehicleInfo.numBuffer, vehicleInfo.numBufferB});
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event){
        for(VehicleInfo vehicleInfo:vehicles.values())
            vehicleInfo.checkBuffer(event.getTime(), modes.get(vehicleInfo.mode).speed, link.getLength());
        if(event.getLinkId().equals(link.getId())) {
            int numBuffer = 0;
            for(VehicleInfo vehicleInfo:vehicles.values())
                if(vehicleInfo.inBuffer)
                    numBuffer++;
            vehicles.put(event.getVehicleId(), new VehicleInfo(event.getTime(), modeVehicles.get(event.getVehicleId()), vehicles.size(), numBuffer));
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        for(VehicleInfo vehicleInfo:vehicles.values())
            vehicleInfo.checkBuffer(event.getTime(), modes.get(vehicleInfo.mode).speed, link.getLength());
        modeVehicles.put(event.getVehicleId(), event.getNetworkMode());
    }

    @Override
    public void reset(int iteration) {

    }

    public static void main(String[] args) throws FileNotFoundException {
        File configFile = new File(args[0]);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile.getAbsolutePath()));
        File networkFile = new File(configFile.getParentFile(),scenario.getConfig().network().getInputFile());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile.getAbsolutePath());
        File vehiclesFile = new File(configFile.getParentFile(),scenario.getConfig().vehicles().getVehiclesFile());
        new VehicleReaderV1(scenario.getVehicles()).readFile(vehiclesFile.getAbsolutePath());
        DensitySpeed densitySpeed = new DensitySpeed(scenario.getVehicles().getVehicleTypes().values(), scenario.getNetwork().getLinks().get(Id.createLinkId(args[1])));
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(densitySpeed);
        new MatsimEventsReader(events).readFile(args[2]);
        PrintWriter writer = new PrintWriter(args[3]);
        for (double[] point : densitySpeed.points) {
            writer.print(point[0]);
            for(int i=1; i<point.length; i++)
                writer.print(","+point[i]);
            writer.println();
        }
        writer.close();
    }

}
