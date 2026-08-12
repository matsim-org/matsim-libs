package org.matsim.contrib.pseudosimulation.mobsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

class PSimExecutionCoordinatorTest {

    @Test
    void initializesAllSegmentsBeforeWaitingAndPollsEveryHundredMilliseconds() {
        List<FakeWorker> workers = new ArrayList<>();
        List<Runnable> started = new ArrayList<>();
        List<Long> sleeps = new ArrayList<>();
        Network network = NetworkUtils.createNetwork();
        EventsManager events = EventsUtils.createEventsManager();
        List<Plan> plans = List.of(createPlan("one"), createPlan("two"));

        PSimExecutionCoordinator coordinator = new PSimExecutionCoordinator(2, completion -> {
            FakeWorker worker = new FakeWorker(completion);
            workers.add(worker);
            return worker;
        }, started::add, milliseconds -> {
            sleeps.add(milliseconds);
            started.forEach(Runnable::run);
        });

        coordinator.execute(plans, network, events);

        assertEquals(2, workers.size());
        assertEquals(List.of(100L), sleeps);
        assertEquals(1, workers.get(0).plans.size());
        assertEquals(1, workers.get(1).plans.size());
        assertSame(network, workers.get(0).network);
        assertSame(events, workers.get(0).events);
    }

    private static Plan createPlan(String id) {
        Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
        Plan plan = PopulationUtils.createPlan(person);
        person.addPlan(plan);
        return plan;
    }

    private static final class FakeWorker implements PSimExecutionCoordinator.Worker {
        private final Runnable completion;
        private Collection<Plan> plans;
        private Network network;
        private EventsManager events;

        private FakeWorker(Runnable completion) {
            this.completion = completion;
        }

        @Override
        public void initialize(Collection<Plan> plans, Network network, EventsManager events) {
            this.plans = plans;
            this.network = network;
            this.events = events;
        }

        @Override
        public void run() {
            completion.run();
        }
    }
}
