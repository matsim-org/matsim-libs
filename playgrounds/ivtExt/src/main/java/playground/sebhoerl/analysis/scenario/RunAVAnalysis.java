package playground.sebhoerl.analysis.scenario;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import playground.sebhoerl.analysis.aggregate_events.AggregateReader;
import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.bins.BinDataFactory;
import playground.sebhoerl.analysis.bins.TimeBin;
import playground.sebhoerl.analysis.bins.TimeBinContainer;

public class RunAVAnalysis {
    final static double RESOLUTION = 120.0;
    
    static public class WaitingTimeData {
        public long numberOfJobs;
        public double accumulatedWaitingTime;
        
        public long numberOfOngoingJobs;
        public double accumulatedOngoingWaitingTime;
    }
    
    static public class AVStateData {
        public long numberOfDropoffDrive;
        public long numberOfPickupDrive;
        public long numberOfPickup;
        public long numberOfDropoff;
    }
    
    static public class AVHandler extends DefaultAggregateHandler {
        public final List<List<Double>> dispatcherModeSwitches = new LinkedList<>();
        
        final TimeBinContainer<WaitingTimeData> waitingContainer = new TimeBinContainer<>(0.0, 30.0 * 3600.0, RESOLUTION, new BinDataFactory<WaitingTimeData>(){
            @Override
            public WaitingTimeData createData() {
                return new WaitingTimeData();
            }
        });
        
        final TimeBinContainer<AVStateData> stateContainer = new TimeBinContainer<>(0.0, 30.0 * 3600.0, RESOLUTION, new BinDataFactory<AVStateData>(){
            @Override
            public AVStateData createData() {
                return new AVStateData();
            }
        });
        
        @Override
        public void handleWaiting(Id<Person> person, Id<Person> av, double start, double end) {
            WaitingTimeData current = waitingContainer.getBinByTime(start).getData();
            current.numberOfJobs += 1;
            current.accumulatedWaitingTime += end - start;
            
            double time = start;
            while (time <= end) {
                current = waitingContainer.getBinByTime(time).getData();
                current.numberOfOngoingJobs += 1;
                current.accumulatedOngoingWaitingTime += end - start;
                time += RESOLUTION;
            }
        }

        @Override
        public void handleAVState(Id<Person> av, double start, double end, String state) {
            if (state.equals("AVPickup")) {
                AVStateData current = stateContainer.getBinByTime(start).getData();
                current.numberOfPickup += 1;
            } else if (state.equals("AVDropoff")) {
                AVStateData current = stateContainer.getBinByTime(start).getData();
                current.numberOfDropoff += 1;
            } else if (state.equals("AVPickupDrive") || state.equals("AVDropoffDrive")) {
                double time = start;
                while (time <= end) {
                    AVStateData current = stateContainer.getBinByTime(time).getData();
                    
                    if (state.equals("AVPickupDrive")) {
                        current.numberOfPickupDrive += 1;
                    } else {
                        current.numberOfDropoffDrive += 1;
                    }

                    time += RESOLUTION;
                }
                
                AVStateData current = stateContainer.getBinByTime(start).getData();
                current.numberOfDropoff += 1;
            }
        }
        
        @Override
        public void handleAVDispatcherMode(String mode, double start, double end) {
            dispatcherModeSwitches.add(Arrays.asList(start, end));
        }
        
        public class AVStateStats {
            public LinkedList<Long> dropoffDrive = new LinkedList<>();
            public LinkedList<Long> pickupDrive = new LinkedList<>();
            public LinkedList<Long> pickup = new LinkedList<>();
            public LinkedList<Long> dropoff = new LinkedList<>();
        }
        
        public AVStateStats getAVStats() {
            AVStateStats stats = new AVStateStats();
            
            for (TimeBin<AVStateData> bin : stateContainer) {
                AVStateData data = bin.getData();
                
                stats.pickup.add(data.numberOfPickup);
                stats.dropoff.add(data.numberOfDropoff);
                stats.pickupDrive.add(data.numberOfPickupDrive);
                stats.dropoffDrive.add(data.numberOfDropoffDrive);
            }
            
            return stats;
        }
        
        public class WaitingTimeStats {
            public double averageWaitingTime = 0.0;
            public LinkedList<Double> averageWaitingTimes = new LinkedList<>();
            public LinkedList<Double> averageOngoingWaitingTimes = new LinkedList<>();
        }
        
        public WaitingTimeStats getWaitingTimeStats() {
            WaitingTimeStats stats = new WaitingTimeStats();
            
            long totalTrips = 0;
            double totalTime = 0.0;
            
            for (TimeBin<WaitingTimeData> bin : waitingContainer) {
                WaitingTimeData data = bin.getData();
                
                totalTrips += data.numberOfJobs;
                totalTime += data.accumulatedWaitingTime;
                
                stats.averageWaitingTimes.add(data.accumulatedWaitingTime / (double)data.numberOfJobs);
                stats.averageOngoingWaitingTimes.add(data.accumulatedOngoingWaitingTime / (double)data.numberOfOngoingJobs);
            }
            
            stats.averageWaitingTime = totalTime / (double)totalTrips;
            
            return stats;
        }
    }

    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        AggregateReader reader = new AggregateReader();
        
        AVHandler handler = new AVHandler();
        reader.addHandler(handler);
        reader.read(args[0]);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(args[1]), Arrays.asList(handler.getWaitingTimeStats(), handler.getAVStats(), handler.dispatcherModeSwitches));
    }
}
