package playground.sebhoerl.analysis.scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.bins.BinDataFactory;
import playground.sebhoerl.analysis.bins.TimeBin;
import playground.sebhoerl.analysis.bins.TimeBinContainer;

public class RunAnalyzeFlow {   
    final static double RESOLUTION = 120.0;
    
    static class Occupancy {
        public Id<Link> link;
        public double enter = 0.0;
        public double leave = 0.0;
        public String state = null;
    }
    
    static class MeasurementHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, ActivityStartEventHandler {
        final Map<Id<Link>, TimeBinContainer<Long>> map = new HashMap<>();
        final Map<Id<Vehicle>, Occupancy> occupancies = new HashMap<>();
        final Map<Id<Vehicle>, String> avStates = new HashMap<>();
        
        public double totalPickupDistance = 0.0;
        public double totalDropoffDistance = 0.0;
        public double totalDistance = 0.0;
        
        int numberOfBins;
        double flowCapacityFactor;
        
        final Network network;
        
        public MeasurementHandler(Network network, double flowCapacityFactor) {
            for (Link link : network.getLinks().values()) {
                map.put(link.getId(), new TimeBinContainer<Long>(0.0, 30.0 * 3600.0, RESOLUTION, new BinDataFactory<Long>() {
                    @Override
                    public Long createData() {
                        return (long) 0;
                    }
                }));
                
                numberOfBins = map.get(link.getId()).getNumberOfBins();
            }
            
            this.network = network;
            this.flowCapacityFactor = flowCapacityFactor;
        }
        
        @Override
        public void reset(int iteration) {}

        @Override
        public void handleEvent(LinkEnterEvent event) {
            Occupancy occupancy = new Occupancy();
            occupancy.enter = event.getTime();
            occupancy.link = event.getLinkId();
            occupancy.state = avStates.get(event.getVehicleId());

            occupancies.put(event.getVehicleId(), occupancy);
        }
        
        @Override
        public void handleEvent(LinkLeaveEvent event) {
            Occupancy occupancy = occupancies.remove(event.getVehicleId());
            
            if (occupancy != null) {
                occupancy.leave = event.getTime();
                handleOccupancy(occupancy);
            }
        }
        
        private void handleOccupancy(Occupancy occupancy) {
            double time = occupancy.enter; // + RESOLUTION;
            
            // Density
            //while (time < occupancy.leave + RESOLUTION) {
            //    TimeBin<Long> bin = map.get(occupancy.link).getBinByTime(time);
            //    bin.setData(bin.getData() + 1);
            //    time += RESOLUTION;
            //}
            
            // Capacity
            TimeBin<Long> bin = map.get(occupancy.link).getBinByTime(time);
            bin.setData(bin.getData() + 1);
            
            double distance = network.getLinks().get(occupancy.link).getLength();
            
            if (occupancy.state != null) {
                if (occupancy.state.equals("AVPickupDrive")) {
                    totalPickupDistance += distance;
                } else {
                    totalDropoffDistance += network.getLinks().get(occupancy.link).getLength();
                }
            }
            
            totalDistance += distance;
            
            /*while (time <= occupancy.leave) {
                TimeBin<Long> bin = map.get(occupancy.link).getBinByTime(time);
                bin.setData(bin.getData() + 1);
                time += RESOLUTION;
            }*/
        }
        
        public ArrayList<Double> getStatistics() {
            ArrayList<Double> congestion = new ArrayList<Double>(numberOfBins);

            for (int i = 0; i < numberOfBins; i++) {
                double averageCongestion = 0.0;
                long numberOfLinks = 0;
                
                for (Id<Link> link : network.getLinks().keySet()) {
                    double local = map.get(link).getBinByIndex(i).getData();
                    
                    // Capacity
                    double capacity = network.getLinks().get(link).getCapacity() * flowCapacityFactor * RESOLUTION / 3600.0;
                    
                    if (network.getLinks().get(link).getCapacity() > 5000.0) {
                        averageCongestion += local / capacity;
                        numberOfLinks += 1;
                    }
                    
                    
                    
                    // Density
                    //double maximumDensity = 1.0 / 7.5;
                    //averageCongestion += local / (network.getLinks().get(link).getLength() * network.getLinks().get(link).getNumberOfLanes()) / maximumDensity;
                    
                    
                    
                    
                    
                }
                
                congestion.add(averageCongestion / (double)numberOfLinks);
            }

            return congestion;
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if (event.getPersonId().toString().contains("av")) {
                avStates.put(Id.createVehicleId(event.getPersonId()), event.getActType());
            }
        }
    }

    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        Config config = ConfigUtils.loadConfig(args[0]);
        Scenario scenario = ScenarioUtils.createScenario(config);
        EventsManagerImpl events = new EventsManagerImpl();
        
        (new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
        MatsimEventsReader reader = new MatsimEventsReader(events);
        
        MeasurementHandler handler = new MeasurementHandler(scenario.getNetwork(), config.qsim().getFlowCapFactor());
        events.addHandler(handler);
        reader.readFile(args[2]);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[3]), Arrays.asList(handler.getStatistics(), handler.totalDistance, handler.totalPickupDistance, handler.totalDropoffDistance));
    }

}
