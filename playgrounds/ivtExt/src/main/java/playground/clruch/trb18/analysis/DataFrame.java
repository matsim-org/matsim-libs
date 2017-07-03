package playground.clruch.trb18.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.*;

public class DataFrame {
    public List<Double> vehicleDistance = new LinkedList<>();
    public List<Double> passengerDistance = new LinkedList<>();
    public List<Double> withPassengerTime = new LinkedList<>();

    public List<List<Double>> waitingTimes = new LinkedList<>();
    public List<List<Double>> travelTimes = new LinkedList<>();;
    public List<Long> numberOfServedRequests = new LinkedList<>();

    public long numberOfUnservedRequests = 0;

    public List<List<Double>> travelTimeDelaysCar = new LinkedList<>();
    public List<List<Double>> travelTimeDelaysPt = new LinkedList<>();
    public long numberOfUnmeasurableDelays = 0;

    public DataFrame(BinCalculator binCalculator) {
        vehicleDistance.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));
        passengerDistance.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));
        withPassengerTime.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));
        numberOfServedRequests.addAll(Collections.nCopies(binCalculator.getBins(), 0L));

        for (int i = 0; i < binCalculator.getBins(); i++) waitingTimes.add(new LinkedList<>());
        for (int i = 0; i < binCalculator.getBins(); i++) travelTimes.add(new LinkedList<>());
        for (int i = 0; i < binCalculator.getBins(); i++) travelTimeDelaysCar.add(new LinkedList<>());
        for (int i = 0; i < binCalculator.getBins(); i++) travelTimeDelaysPt.add(new LinkedList<>());
    }
}
