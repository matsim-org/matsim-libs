package playground.sebhoerl.analysis.scenario;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import playground.sebhoerl.analysis.aggregate_events.DefaultAggregateHandler;
import playground.sebhoerl.analysis.aggregate_events.Trip;
import playground.sebhoerl.analysis.bins.TimeModeBinContainer;

public class TripStatisticHandler extends DefaultAggregateHandler {
    final TimeModeBinContainer<TripStatisticBinData> container = new TimeModeBinContainer<>(
            Arrays.asList("av", "pt", "car", "walk"), 
            0.0, 30.0 * 3600.0, 120.0,
            new TripStatisticBinData.Factory()
            );
    
    final ArrayList<String> modes = new ArrayList<>(Arrays.asList("av", "pt", "car", "walk"));
    
    final long departures[] = new long[modes.size()];
    final double accumulatedTravelTime[] = new double[modes.size()];
    final double accumulatedTravelDistance[] = new double[modes.size()];
    long totalDepartures = 0;
    
    double accumulatedTransitTime = 0.0;
    double accumulatedTransitDistance = 0.0;
    
    public TripStatisticHandler() {
        for (String mode : container.getModes()) {
            int index = modes.indexOf(mode);
            departures[index] = 0;
            accumulatedTravelTime[index] = 0.0;
            accumulatedTravelDistance[index] = 0.0;
        }
    }
    
    @Override
    public void handleTrip(Trip trip) {
        if (trip.getPerson().toString().contains("pt")) return;
        if (trip.getPerson().toString().contains("av")) return;
        
        double end = trip.getEndTime();
        double time = trip.getStartTime();
        
        container.getBinByModeAndTime(trip.getMode(), time).getData().numberOfDepartures += 1;
        container.getBinByModeAndTime(trip.getMode(), end).getData().numberOfArrivals += 1;
        
        while (time <= end) {
            TripStatisticBinData data = container.getBinByModeAndTime(trip.getMode(), time).getData();
            data.accumulatedTravelDistance += trip.getDistance();
            data.accumulatedTravelTime += trip.getEndTime() - trip.getStartTime();
            data.numberOfTrips += 1;
            time += container.getInterval();
        }
        
        int index = modes.indexOf(trip.getMode());
        departures[index] += 1;
        accumulatedTravelTime[index] += trip.getEndTime() - trip.getStartTime();
        accumulatedTravelDistance[index] += trip.getDistance();
        totalDepartures += 1;
        
        accumulatedTransitTime += trip.getWalkTime();
        accumulatedTransitDistance += trip.getWalkDistance();
    }
    
    public static class TripStatisticData {
        public Map<String, LinkedList<Long>> trips = new HashMap<>();
        public Map<String, LinkedList<Long>> departures = new HashMap<>();
        public Map<String, LinkedList<Long>> arrivals = new HashMap<>();
        public Map<String, LinkedList<Double>> shares = new HashMap<>();
        
        public Map<String, Double> averageShare = new HashMap<>();
        public Map<String, Double> averageTravelTime = new HashMap<>();
        public Map<String, Double> averageTravelDistance = new HashMap<>();
        
        public double averageTransitDistance = 0.0;
        public double averageTransitTime = 0.0;
    }
    
    public TripStatisticData getData() {
        TripStatisticData output = new TripStatisticData();

        // Ongoing trips per time
        long tripsPerBin[] = new long[container.getNumberOfTimeBins()];
        
        for (String mode : container.getModes()) {
            output.trips.put(mode, new LinkedList<Long>());
            output.departures.put(mode, new LinkedList<Long>());
            output.arrivals.put(mode, new LinkedList<Long>());
            
            for (int i = 0; i < container.getNumberOfTimeBins(); i++) {
                TripStatisticBinData data = container.getBinByModeAndTimeIndex(mode, i).getData();
                
                tripsPerBin[i] += data.numberOfTrips;
                output.trips.get(mode).add(data.numberOfTrips);
                output.departures.get(mode).add(data.numberOfDepartures);
                output.arrivals.get(mode).add(data.numberOfArrivals);
            }
        }
        
        // Shares per time
        for (String mode : container.getModes()) {
            output.shares.put(mode, new LinkedList<Double>());
            
            for (int i = 0; i < container.getNumberOfTimeBins(); i++) {
                TripStatisticBinData data = container.getBinByModeAndTimeIndex(mode, i).getData();
                output.shares.get(mode).add((double)data.numberOfTrips / (double)tripsPerBin[i]);
            }
        }
        
        // Averages
        for (String mode : modes) {
            int index = modes.indexOf(mode);
            output.averageShare.put(mode, (double) departures[index] / (double) totalDepartures);
            output.averageTravelTime.put(mode, (double) accumulatedTravelTime[index] / (double) departures[index]);
            output.averageTravelDistance.put(mode, (double) accumulatedTravelDistance[index] / (double) departures[index]);
        }
        
        int index = modes.indexOf("pt");
        output.averageTransitDistance = accumulatedTransitDistance / (double) departures[index];
        output.averageTransitTime = accumulatedTransitTime / (double) departures[index];
        
        return output;
        
        // Write
        
        
        
        /*for (String mode : modes) {
            writer.write("trips " + mode);
            for (long _trips : trips.get(mode)) writer.write(" " + String.valueOf(_trips)); 
            writer.write("\n");
            
            writer.write("shares " + mode);
            for (double _share : shares.get(mode)) writer.write(" " + String.valueOf(_share)); 
            writer.write("\n");
            
            writer.write(String.format("averages %s %f %f %f\n", mode, averageShare.get(mode), averageTravelTime.get(mode), averageTravelDistance.get(mode)));
        }*/
    }
}




