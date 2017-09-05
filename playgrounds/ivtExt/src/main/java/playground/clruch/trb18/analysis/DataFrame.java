package playground.clruch.trb18.analysis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import playground.sebhoerl.av_paper.BinCalculator;

public class DataFrame {
    public List<Double> vehicleDistance = new LinkedList<>();
    public List<Double> passengerDistance = new LinkedList<>();
    public List<Double> withPassengerTime = new LinkedList<>();

    public List<List<Double>> waitingTimes = new LinkedList<>();
    public List<List<Double>> travelTimes = new LinkedList<>();;
    public List<Long> numberOfServedRequests = new LinkedList<>();
    public List<Double> numberOfWaitingRequests = new LinkedList<>();

    public long numberOfUnservedRequests = 0;

    //public List<List<Double>> travelTimeDelaysCar = new LinkedList<>();
    //public List<List<Double>> travelTimeDelaysPt = new LinkedList<>();
    //public List<List<Double>> relativeTravelTimeDelaysCar = new LinkedList<>();
    //public List<List<Double>> relativeTravelTimeDelaysPt = new LinkedList<>();
    //public long numberOfUnmeasurableDelays = 0;

    public DataFrame(BinCalculator binCalculator) {
        vehicleDistance.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));
        passengerDistance.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));
        withPassengerTime.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));
        numberOfServedRequests.addAll(Collections.nCopies(binCalculator.getBins(), 0L));
        numberOfWaitingRequests.addAll(Collections.nCopies(binCalculator.getBins(), 0.0d));

        for (int i = 0; i < binCalculator.getBins(); i++) waitingTimes.add(new LinkedList<>());
        for (int i = 0; i < binCalculator.getBins(); i++) travelTimes.add(new LinkedList<>());
        //for (int i = 0; i < binCalculator.getBins(); i++) travelTimeDelaysCar.add(new LinkedList<>());
        //for (int i = 0; i < binCalculator.getBins(); i++) travelTimeDelaysPt.add(new LinkedList<>());
        //for (int i = 0; i < binCalculator.getBins(); i++) relativeTravelTimeDelaysCar.add(new LinkedList<>());
        //for (int i = 0; i < binCalculator.getBins(); i++) relativeTravelTimeDelaysPt.add(new LinkedList<>());
    }
}
