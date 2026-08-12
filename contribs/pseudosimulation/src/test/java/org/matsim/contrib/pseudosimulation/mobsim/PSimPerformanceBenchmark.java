package org.matsim.contrib.pseudosimulation.mobsim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.contrib.pseudosimulation.distributed.SerializableLinkTravelTimes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;

/**
 * Explicitly selected performance probe; its name deliberately does not match the normal Surefire test patterns.
 */
class PSimPerformanceBenchmark {

    private static final int PLAN_COUNT = Integer.getInteger("psim.benchmark.plans", 20_000);
    private static final int WARMUP_ROUNDS = Integer.getInteger("psim.benchmark.warmups", 3);
    private static final int MEASUREMENT_ROUNDS = Integer.getInteger("psim.benchmark.rounds", 7);
    private static final double END_TIME = 86_400;
    private static final Set<String> TRANSIT_MODES = Set.of(TransportMode.pt);
    private static final TransitEmulator TRANSIT_EMULATOR = (leg, departure) ->
            new TransitEmulator.Trip(null, departure + 2, departure + 8);
    private static volatile double lookupResult;

    @Test
    void measureRepresentativeWorkloads() {
        Locale.setDefault(Locale.ROOT);
        System.out.printf("PSim benchmark: plans=%d warmups=%d rounds=%d java=%s processors=%d%n",
                PLAN_COUNT, WARMUP_ROUNDS, MEASUREMENT_ROUNDS, Runtime.version(),
                Runtime.getRuntime().availableProcessors());
        System.out.println("workload,median_ms,min_ms,events_per_run,median_events_per_second");

        BenchmarkNetwork benchmarkNetwork = createNetwork();
        measureSerializedTravelTimeLookup(benchmarkNetwork);
        measureCore("teleport", createPlans(Workload.TELEPORT, benchmarkNetwork), benchmarkNetwork.network(),
                PLAN_COUNT * 5L);
        measureCore("car", createPlans(Workload.CAR, benchmarkNetwork), benchmarkNetwork.network(), PLAN_COUNT * 10L);
        measureCore("transit", createPlans(Workload.TRANSIT, benchmarkNetwork), benchmarkNetwork.network(),
                PLAN_COUNT * 6L);
        Collection<Plan> mixedPlans = createPlans(Workload.MIXED, benchmarkNetwork);
        long mixedEventCount = mixedEventCount();
        measureCore("mixed", mixedPlans, benchmarkNetwork.network(), mixedEventCount);
        measureEndToEnd("mixed-end-to-end", mixedPlans, benchmarkNetwork.network(), mixedEventCount);
    }

    private static void measureSerializedTravelTimeLookup(BenchmarkNetwork network) {
        SerializableLinkTravelTimes travelTimes = new SerializableLinkTravelTimes(
                (link, time, person, vehicle) -> link == network.start() ? 1 : 2,
                900, (int) END_TIME, List.of(network.start(), network.end()));
        int lookups = PLAN_COUNT * 100;
        Runnable lookup = () -> {
            double result = 0;
            for (int index = 0; index < lookups; index++) {
                Link link = index % 2 == 0 ? network.start() : network.end();
                result += travelTimes.getLinkTravelTime(link, index % 86_400, null, null);
            }
            lookupResult = result;
        };
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            lookup.run();
        }
        long[] samples = new long[MEASUREMENT_ROUNDS];
        for (int round = 0; round < MEASUREMENT_ROUNDS; round++) {
            long start = System.nanoTime();
            lookup.run();
            samples[round] = System.nanoTime() - start;
        }
        assertEquals(lookups * 1.5, lookupResult);
        printResult("serialized-link-lookup", samples, lookups);
    }

    private static void measureCore(String name, Collection<Plan> plans, Network network, long expectedEvents) {
        EventCounter eventCounter = new EventCounter();
        EventsManager eventManager = createEventManager(eventCounter);
        PSimPlanExecutor executor = new PSimPlanExecutor(END_TIME, (link, time, person, vehicle) -> 1,
                TRANSIT_EMULATOR, TRANSIT_MODES, () -> { });
        executor.initialize(plans, network, eventManager);

        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            runAndValidate(executor, eventCounter, expectedEvents);
        }

        long[] samples = new long[MEASUREMENT_ROUNDS];
        for (int round = 0; round < MEASUREMENT_ROUNDS; round++) {
            long start = System.nanoTime();
            runAndValidate(executor, eventCounter, expectedEvents);
            samples[round] = System.nanoTime() - start;
        }
        printResult(name, samples, expectedEvents);
    }

    private static void measureEndToEnd(String name, Collection<Plan> plans, Network network, long expectedEvents) {
        Config config = ConfigUtils.createConfig();
        config.global().setNumberOfThreads(1);
        config.qsim().setEndTime(END_TIME);
        ConfigUtils.addOrGetModule(config, TransitConfigGroup.class).setTransitModes(TRANSIT_MODES);
        var scenario = ScenarioUtils.createScenario(config);
        for (var node : network.getNodes().values()) {
            scenario.getNetwork().addNode(node);
        }
        for (var link : network.getLinks().values()) {
            scenario.getNetwork().addLink(link);
        }
        EventCounter eventCounter = new EventCounter();
        EventsManager eventManager = createEventManager(eventCounter);
        PSim psim = new PSim(scenario, eventManager, plans, (link, time, person, vehicle) -> 1, TRANSIT_EMULATOR);

        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            runAndValidate(psim::run, eventCounter, expectedEvents);
        }
        long[] samples = new long[MEASUREMENT_ROUNDS];
        for (int round = 0; round < MEASUREMENT_ROUNDS; round++) {
            long start = System.nanoTime();
            runAndValidate(psim::run, eventCounter, expectedEvents);
            samples[round] = System.nanoTime() - start;
        }
        printResult(name, samples, expectedEvents);
    }

    private static void runAndValidate(Runnable execution, EventCounter counter, long expectedEvents) {
        long before = counter.count();
        execution.run();
        assertEquals(expectedEvents, counter.count() - before, "benchmark event count changed");
    }

    private static long mixedEventCount() {
        long count = 0;
        int[] eventsPerPlan = { 5, 10, 6 };
        for (int index = 0; index < PLAN_COUNT; index++) {
            count += eventsPerPlan[index % eventsPerPlan.length];
        }
        return count;
    }

    private static EventsManager createEventManager(EventCounter counter) {
        EventsManager eventManager = EventsUtils.createEventsManager();
        eventManager.addHandler((BasicEventHandler) counter::record);
        return eventManager;
    }

    private static void printResult(String name, long[] samples, long events) {
        Arrays.sort(samples);
        long median = samples[samples.length / 2];
        long minimum = samples[0];
        double medianMilliseconds = median / 1_000_000.0;
        double minimumMilliseconds = minimum / 1_000_000.0;
        double eventsPerSecond = events * 1_000_000_000.0 / median;
        System.out.printf("%s,%.3f,%.3f,%d,%.0f%n", name, medianMilliseconds, minimumMilliseconds, events,
                eventsPerSecond);
    }

    private static Collection<Plan> createPlans(Workload workload, BenchmarkNetwork network) {
        List<Plan> plans = new ArrayList<>(PLAN_COUNT);
        for (int index = 0; index < PLAN_COUNT; index++) {
            Workload planWorkload = workload == Workload.MIXED ? Workload.values()[index % 3] : workload;
            plans.add(createPlan(index, planWorkload, network));
        }
        return plans;
    }

    private static Plan createPlan(int index, Workload workload, BenchmarkNetwork network) {
        Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(index));
        Plan plan = PopulationUtils.createPlan(person);
        person.addPlan(plan);
        Activity origin = PopulationUtils.createActivityFromLinkId("origin", network.start().getId());
        origin.setEndTime(10);
        String mode = switch (workload) {
            case TELEPORT -> TransportMode.walk;
            case CAR -> TransportMode.car;
            case TRANSIT -> TransportMode.pt;
            case MIXED -> throw new IllegalArgumentException("mixed must be expanded before creating a plan");
        };
        Leg leg = PopulationUtils.createLeg(mode);
        if (workload == Workload.CAR) {
            leg.setRoute(RouteUtils.createNetworkRoute(List.of(network.start().getId(), network.end().getId()),
                    network.network()));
        } else {
            var route = RouteUtils.createGenericRouteImpl(network.start().getId(), network.end().getId());
            route.setTravelTime(7);
            route.setDistance(42);
            leg.setRoute(route);
        }
        Activity destination = PopulationUtils.createActivityFromLinkId("destination", network.end().getId());
        plan.addActivity(origin);
        plan.addLeg(leg);
        plan.addActivity(destination);
        return plan;
    }

    private static BenchmarkNetwork createNetwork() {
        Network network = NetworkUtils.createNetwork();
        var first = NetworkUtils.createAndAddNode(network, Id.createNodeId("first"), new Coord(0, 0));
        var second = NetworkUtils.createAndAddNode(network, Id.createNodeId("second"), new Coord(1, 0));
        var third = NetworkUtils.createAndAddNode(network, Id.createNodeId("third"), new Coord(2, 0));
        Link start = NetworkUtils.createAndAddLink(network, Id.createLinkId("start"), first, second, 1, 1, 1, 1);
        Link end = NetworkUtils.createAndAddLink(network, Id.createLinkId("end"), second, third, 1, 1, 1, 1);
        return new BenchmarkNetwork(network, start, end);
    }

    private enum Workload {
        TELEPORT,
        CAR,
        TRANSIT,
        MIXED
    }

    private record BenchmarkNetwork(Network network, Link start, Link end) {
    }

    private static final class EventCounter {
        private final AtomicLong events = new AtomicLong();

        void record(Event event) {
            events.incrementAndGet();
        }

        long count() {
            return events.get();
        }
    }
}
