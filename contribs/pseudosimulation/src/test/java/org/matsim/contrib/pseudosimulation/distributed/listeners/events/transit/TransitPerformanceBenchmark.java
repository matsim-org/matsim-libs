package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/** Explicitly selected transit-history performance probe. */
class TransitPerformanceBenchmark {

    private static final int HISTORY_SIZE = Integer.getInteger("psim.transit.benchmark.history", 10_000);
    private static final int LOOKUPS = Integer.getInteger("psim.transit.benchmark.lookups", 10_000);
    private static final int WARMUPS = Integer.getInteger("psim.benchmark.warmups", 3);
    private static final int ROUNDS = Integer.getInteger("psim.benchmark.rounds", 7);
    private static final Id<TransitLine> LINE = Id.create("benchmark-line", TransitLine.class);
    private static final Id<TransitRoute> ROUTE = Id.create("benchmark-route", TransitRoute.class);
    private static final Id<TransitStopFacility> ORIGIN = Id.create("benchmark-origin", TransitStopFacility.class);
    private static final Id<TransitStopFacility> DESTINATION =
            Id.create("benchmark-destination", TransitStopFacility.class);
    private static volatile double resultGuard;

    @Test
    void compareLegacyAndIndexedHistoryLookup() {
        Locale.setDefault(Locale.ROOT);
        List<DwellEvent> history = createHistory();
        TransitPerformance performance = createPerformance(history);
        double queryTime = HISTORY_SIZE - 1_000;
        double expectedInVehicleTime = queryTime - 5;

        System.out.printf("Transit history benchmark: history=%d lookups=%d warmups=%d rounds=%d%n",
                HISTORY_SIZE, LOOKUPS, WARMUPS, ROUNDS);
        System.out.println("workload,median_ms,min_ms,lookups,median_lookups_per_second");
        measure("legacy-transit-history", () -> legacyLookup(history, queryTime), expectedInVehicleTime);
        measure("indexed-transit-history",
                () -> performance.getRouteTravelTime(LINE, ROUTE, ORIGIN, DESTINATION, queryTime),
                expectedInVehicleTime);
    }

    private static void measure(String name, Lookup lookup, double expectedInVehicleTime) {
        for (int round = 0; round < WARMUPS; round++) {
            executeLookups(lookup);
        }
        long[] samples = new long[ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            long start = System.nanoTime();
            executeLookups(lookup);
            samples[round] = System.nanoTime() - start;
        }
        assertEquals(expectedInVehicleTime * LOOKUPS, resultGuard);
        Arrays.sort(samples);
        long median = samples[samples.length / 2];
        System.out.printf("%s,%.3f,%.3f,%d,%.0f%n", name, median / 1_000_000.0,
                samples[0] / 1_000_000.0, LOOKUPS, LOOKUPS * 1_000_000_000.0 / median);
    }

    private static void executeLookups(Lookup lookup) {
        double result = 0;
        for (int index = 0; index < LOOKUPS; index++) {
            result += lookup.get().getSecond();
        }
        resultGuard = result;
    }

    private static TransitPerformance createPerformance(List<DwellEvent> history) {
        TransitPerformance performance = new TransitPerformance(new BoardingModelIgnoringOccupancy(),
                new ZeroRandom());
        for (DwellEvent dwellEvent : history) {
            performance.addVehicleDwellEventAtStop(LINE, ROUTE, ORIGIN, dwellEvent);
        }
        return performance;
    }

    private static List<DwellEvent> createHistory() {
        List<DwellEvent> history = new ArrayList<>(HISTORY_SIZE);
        for (int index = 0; index < HISTORY_SIZE; index++) {
            double inVehicleTime = index;
            VehicleTracker vehicle = new VehicleTracker(null, null, 1) {
                @Override
                public double getInVehicleTime(DwellEvent dwellEvent, Id<TransitStopFacility> destinationStop) {
                    return inVehicleTime;
                }
            };
            history.add(new DwellEvent(index, ORIGIN.toString(), vehicle, 0));
        }
        return history;
    }

    private static Tuple<Double, Double> legacyLookup(List<DwellEvent> history, double time) {
        LinkedList<Double> recent = new LinkedList<>();
        for (DwellEvent dwellEvent : history) {
            recent.add(dwellEvent.getVehicle().getInVehicleTime(dwellEvent, DESTINATION));
            if (recent.size() > 6) {
                recent.removeFirst();
            }
            if (dwellEvent.getArrivalTime() >= time) {
                return new Tuple<>(dwellEvent.getArrivalTime() - time, recent.getFirst());
            }
        }
        return new Tuple<>(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    private interface Lookup {
        Tuple<Double, Double> get();
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
