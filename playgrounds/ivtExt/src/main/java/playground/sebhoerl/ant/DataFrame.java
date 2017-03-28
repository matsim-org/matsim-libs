package playground.sebhoerl.ant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.*;

public class DataFrame {
    @JsonIgnore final public List<String> modes = Arrays.asList("car", "pt", "walk", "av");
    @JsonIgnore final public BinCalculator binCalculator;

    final public Map<String, List<Integer>> departureCount;
    final public Map<String, List<Integer>> arrivalCount;
    final public Map<String, List<Double>> travellerCount;

    final public List<Double> waitingCount;
    final public List<List<Double>> waitingTimes;
    final public List<List<Double>> travelTimes;

    public double vehicleDistance = 0.0;
    public double passengerDistance = 0.0;

    public double avVehicleDistance = 0.0;
    public double avPassengerDistance = 0.0;
    public double avEmptyRideDistance = 0.0;

    final public List<Double> idleAVs;

    final public Map<Integer, List<Double>> occupancy;

    final public List<Double> avDistances;
    final public Map<String, Long> chainCounts;

    final public String relevantOperator;

    public DataFrame(BinCalculator binCalculator) {
        this(binCalculator, null);
    }

    public DataFrame(BinCalculator binCalculator, String relevantOperator) {
        this.relevantOperator = relevantOperator;
        this.binCalculator = binCalculator;

        departureCount = initialize(modes, 0);
        arrivalCount = initialize(modes, 0);
        travellerCount = initialize(modes, 0.0);
        waitingCount = initialize(0.0);

        waitingTimes = initialize();
        travelTimes = initialize();

        idleAVs = initialize(0.0);

        occupancy = initialize(Arrays.asList(0, 1, 2, 3, 4), 0.0);

        avDistances = initialize(0.0);
        chainCounts = new HashMap<>();
    }

    public boolean isOrdinaryPerson(Id<Person> id) {
        if (id.toString().startsWith("av_")) return false;
        if (id.toString().startsWith("pt_")) return false;
        return true;
    }

    public boolean isAV(String id) {
        return id.startsWith("av_");
    }

    public boolean isRelevantOperator(String id) {
        return id.startsWith("av_") && (relevantOperator == null || id.contains(relevantOperator));
    }

    private <T extends Number> List<List<T>> initialize() {
        List<List<T>> list = new ArrayList<>(binCalculator.getBins());
        for (int i = 0; i < binCalculator.getBins(); i++) list.add(new ArrayList<>());
        return list;
    }

    private <U, T extends Number> Map<U, List<T>> initialize(List<U> modes, T initializer) {
        Map<U, List<T>> map = new HashMap<>();

        for (U mode : modes) {
            map.put(mode, new ArrayList<T>(Collections.nCopies(binCalculator.getBins(), initializer)));
        }

        return map;
    }

    private <T extends Number> List<T> initialize(T initializer) {
        return new ArrayList<T>(Collections.nCopies(binCalculator.getBins(), initializer));
    }
}
