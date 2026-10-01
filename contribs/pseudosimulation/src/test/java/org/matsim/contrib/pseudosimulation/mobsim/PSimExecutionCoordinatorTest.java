package org.matsim.contrib.pseudosimulation.mobsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
    void usesOnlyAsManyWorkersAsThereArePlans() {
        TestFixture fixture = new TestFixture(4);
        Network network = NetworkUtils.createNetwork();
        EventsManager events = EventsUtils.createEventsManager();

        fixture.coordinator.execute(List.of(createPlan("one"), createPlan("two")), network, events);

        assertEquals(List.of(2), fixture.executorSizes);
        assertEquals(1, fixture.workers.get(0).runs.get());
        assertEquals(1, fixture.workers.get(1).runs.get());
        assertEquals(0, fixture.workers.get(2).runs.get());
        assertEquals(1, fixture.workers.get(0).plans.size());
        assertSame(network, fixture.workers.get(0).network);
        assertSame(events, fixture.workers.get(0).events);
        assertTrue(fixture.executors.get(0).isTerminated());
    }

    @Test
    void returnsWithoutCreatingAnExecutorForNoPlans() {
        TestFixture fixture = new TestFixture(2);

        fixture.coordinator.execute(List.of(), NetworkUtils.createNetwork(), EventsUtils.createEventsManager());

        assertTrue(fixture.executorSizes.isEmpty());
        assertEquals(0, fixture.workers.get(0).runs.get());
        assertEquals(0, fixture.workers.get(1).runs.get());
    }

    @Test
    void supportsRepeatedExecutionWithoutRetainingExecutorThreads() {
        TestFixture fixture = new TestFixture(2);
        List<Plan> plans = List.of(createPlan("one"), createPlan("two"));

        fixture.coordinator.execute(plans, NetworkUtils.createNetwork(), EventsUtils.createEventsManager());
        fixture.coordinator.execute(plans, NetworkUtils.createNetwork(), EventsUtils.createEventsManager());

        assertEquals(List.of(2, 2), fixture.executorSizes);
        assertEquals(2, fixture.workers.get(0).runs.get());
        assertEquals(2, fixture.workers.get(1).runs.get());
        assertTrue(fixture.executors.stream().allMatch(ExecutorService::isTerminated));
    }

    @Test
    @Timeout(5)
    void propagatesWorkerFailureAndInterruptsOtherWorkers() throws InterruptedException {
        CountDownLatch siblingStarted = new CountDownLatch(1);
        AtomicBoolean siblingInterrupted = new AtomicBoolean();
        RuntimeException failure = new RuntimeException("boom");
        List<PSimExecutionCoordinator.Worker> workers = List.of(
                new ActionWorker(() -> {
                    await(siblingStarted);
                    throw failure;
                }),
                new ActionWorker(() -> {
                    siblingStarted.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException exception) {
                        siblingInterrupted.set(true);
                        Thread.currentThread().interrupt();
                    }
                }));
        AtomicInteger nextWorker = new AtomicInteger();
        List<ExecutorService> executors = new ArrayList<>();
        PSimExecutionCoordinator coordinator = new PSimExecutionCoordinator(2,
                () -> workers.get(nextWorker.getAndIncrement()), workerCount -> {
                    ExecutorService executor = Executors.newFixedThreadPool(workerCount);
                    executors.add(executor);
                    return executor;
                });

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> coordinator.execute(List.of(createPlan("one"), createPlan("two")),
                        NetworkUtils.createNetwork(), EventsUtils.createEventsManager()));

        assertEquals("PSim worker failed", exception.getMessage());
        assertSame(failure, exception.getCause());
        assertTrue(siblingInterrupted.get());
        assertTrue(executors.get(0).awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(5)
    void interruptionCancelsWorkersAndRestoresCallerInterruptStatus() throws InterruptedException {
        CountDownLatch workerStarted = new CountDownLatch(1);
        AtomicBoolean workerInterrupted = new AtomicBoolean();
        ActionWorker worker = new ActionWorker(() -> {
            workerStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException exception) {
                workerInterrupted.set(true);
                Thread.currentThread().interrupt();
            }
        });
        PSimExecutionCoordinator coordinator = new PSimExecutionCoordinator(1, () -> worker,
                Executors::newFixedThreadPool);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try {
                coordinator.execute(List.of(createPlan("one")), NetworkUtils.createNetwork(),
                        EventsUtils.createEventsManager());
            } catch (Throwable exception) {
                thrown.set(exception);
                interruptRestored.set(Thread.currentThread().isInterrupted());
            }
        });

        caller.start();
        assertTrue(workerStarted.await(1, TimeUnit.SECONDS));
        caller.interrupt();
        caller.join(Duration.ofSeconds(2));

        assertFalse(caller.isAlive());
        assertTrue(workerInterrupted.get());
        IllegalStateException exception = (IllegalStateException) thrown.get();
        assertEquals("Interrupted while waiting for PSim workers", exception.getMessage());
        assertTrue(exception.getCause() instanceof InterruptedException);
        assertTrue(interruptRestored.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static Plan createPlan(String id) {
        Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
        Plan plan = PopulationUtils.createPlan(person);
        person.addPlan(plan);
        return plan;
    }

    private static final class TestFixture {
        private final List<FakeWorker> workers = new ArrayList<>();
        private final List<Integer> executorSizes = new ArrayList<>();
        private final List<ExecutorService> executors = new ArrayList<>();
        private final PSimExecutionCoordinator coordinator;

        private TestFixture(int workerCount) {
            coordinator = new PSimExecutionCoordinator(workerCount, () -> {
                FakeWorker worker = new FakeWorker();
                workers.add(worker);
                return worker;
            }, size -> {
                executorSizes.add(size);
                ExecutorService executor = Executors.newFixedThreadPool(size);
                executors.add(executor);
                return executor;
            });
        }
    }

    private static class ActionWorker implements PSimExecutionCoordinator.Worker {
        private final Runnable action;

        private ActionWorker(Runnable action) {
            this.action = action;
        }

        @Override
        public void initialize(Collection<Plan> plans, Network network, EventsManager events) {
        }

        @Override
        public void run() {
            action.run();
        }
    }

    private static final class FakeWorker extends ActionWorker {
        private final AtomicInteger runs = new AtomicInteger();
        private Collection<Plan> plans;
        private Network network;
        private EventsManager events;

        private FakeWorker() {
            super(() -> {
            });
        }

        @Override
        public void initialize(Collection<Plan> plans, Network network, EventsManager events) {
            this.plans = plans;
            this.network = network;
            this.events = events;
        }

        @Override
        public void run() {
            runs.incrementAndGet();
        }
    }
}
