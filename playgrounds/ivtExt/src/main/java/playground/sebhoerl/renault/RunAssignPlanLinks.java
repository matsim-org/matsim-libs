package playground.sebhoerl.renault;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class RunAssignPlanLinks {
    static public class AssignmentRunner implements Runnable {
        final private ActivityFacilities facilities;
        final private Collection<Plan> plans;
        final private AtomicLong counter;

        public AssignmentRunner(ActivityFacilities facilities, Collection<Plan> plans, AtomicLong counter) {
            this.facilities = facilities;
            this.plans = plans;
            this.counter = counter;
        }

        @Override
        public void run() {
            for (Plan plan : plans) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        ActivityFacility facility = facilities.getFacilities().get(activity.getFacilityId());
                        activity.setLinkId(facility.getLinkId());
                    }
                }

                counter.incrementAndGet();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int numberOfThreads = Integer.parseInt(args[1]);
        String target = args[2];

        Config config = ConfigUtils.loadConfig(args[0]);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        ActivityFacilities facilities = scenario.getActivityFacilities();
        Population population = scenario.getPopulation();

        // Partition population
        ArrayList<Set<Plan>> segments = new ArrayList<>(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) segments.add(new HashSet<Plan>());

        int index = 0;
        long numberOfPlans = 0;

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            segments.get(index).add(plan);
            numberOfPlans += 1;

            index++;
            if (index == numberOfThreads) index = 0;
        }

        Set<Thread> threads = new HashSet<>();
        AtomicLong counter = new AtomicLong();

        // Run assignment
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(new AssignmentRunner(facilities, segments.get(i), counter));
            threads.add(thread);
            thread.run();
        }

        int running = numberOfThreads;

        while (running > 0) {
            System.out.println(String.format("%d / %d", counter.get(), numberOfPlans));
            Thread.sleep(10000);

            running = 0;

            for (Thread thread : threads) {
                if (thread.isAlive()) running += 1;
            }
        }

        new PopulationWriter(population).write(args[2]);
    }
}
