package playground.sebhoerl.renault;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.*;
import org.matsim.facilities.algorithms.FacilityAlgorithm;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

public class RunAssignFacilityLinks {
    static public class AssignmentRunner implements Runnable {
        final private QuadTree<Id<Link>> qt;
        final private Collection<ActivityFacilityImpl> facilities;
        final private AtomicLong counter;

        public AssignmentRunner(QuadTree<Id<Link>> qt, Collection<ActivityFacilityImpl> facilities, AtomicLong counter) {
            this.qt = qt;
            this.facilities = facilities;
            this.counter = counter;
        }

        @Override
        public void run() {
            for (ActivityFacilityImpl facility : facilities) {
                Coord coord = facility.getCoord();
                facility.setLinkId(qt.getClosest(coord.getX(), coord.getY()));
                counter.incrementAndGet();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int numberOfThreads = Integer.parseInt(args[1]);
        String target = args[2];

        Config config = ConfigUtils.loadConfig(args[0]);
        ((PlansConfigGroup) config.getModules().get(PlansConfigGroup.GROUP_NAME)).setInputFile(null);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network originalNetwork = scenario.getNetwork();
        Network carOnlyNetwork = NetworkUtils.createNetwork();

        // Filter car network
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(originalNetwork);
        filter.filter(carOnlyNetwork, new HashSet<>(Arrays.asList("car")));

        // Build quadtree
        double[] bounds = NetworkUtils.getBoundingBox(carOnlyNetwork.getNodes().values());
        QuadTree<Id<Link>> qt = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);

        for (Link link : carOnlyNetwork.getLinks().values()) {
            Coord coord = link.getFromNode().getCoord();
            qt.put(coord.getX(), coord.getY(), link.getId());
        }

        // Partition facilities
        ActivityFacilities facilities = scenario.getActivityFacilities();

        ArrayList<Set<ActivityFacilityImpl>> segments = new ArrayList<>(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) segments.add(new HashSet<ActivityFacilityImpl>());

        int index = 0;
        long numberOfFacilities = 0;

        for (ActivityFacility facility : facilities.getFacilities().values()) {
            ActivityFacilityImpl fac = (ActivityFacilityImpl) facility;
            segments.get(index).add(fac);
            numberOfFacilities += 1;

            index++;
            if (index == numberOfThreads) index = 0;
        }

        Set<Thread> threads = new HashSet<>();
        AtomicLong counter = new AtomicLong();

        // Run assignment
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(new AssignmentRunner(qt, segments.get(i), counter));
            threads.add(thread);
            thread.run();
        }

        int running = numberOfThreads;

        while (running > 0) {
            System.out.println(String.format("%d / %d", counter.get(), numberOfFacilities));
            Thread.sleep(10000);

            running = 0;

            for (Thread thread : threads) {
                if (thread.isAlive()) running += 1;
            }
        }

        new FacilitiesWriter(facilities).writeV1(args[2]);
    }
}
