package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.api.experimental.events.EventsManager;

final class PSimExecutionCoordinator {

    interface Worker extends Runnable {
        void initialize(Collection<Plan> plans, Network network, EventsManager eventManager);
    }

    interface WorkerFactory {
        Worker create(Runnable completion);
    }

    interface ThreadStarter {
        void start(Runnable worker);
    }

    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }

    private final Worker[] workers;
    private final ThreadStarter threadStarter;
    private final Sleeper sleeper;
    private AtomicInteger remainingWorkers;

    static PSimExecutionCoordinator create(int workerCount, WorkerFactory workerFactory) {
        return new PSimExecutionCoordinator(workerCount, workerFactory, worker -> new Thread(worker).start(), Thread::sleep);
    }

    PSimExecutionCoordinator(int workerCount, WorkerFactory workerFactory, ThreadStarter threadStarter, Sleeper sleeper) {
        this.threadStarter = threadStarter;
        this.sleeper = sleeper;
        workers = new Worker[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = workerFactory.create(this::workerCompleted);
        }
    }

    void execute(Collection<Plan> plans, Network network, EventsManager eventManager) {
        int segmentCount = Math.min(plans.size(), workers.length);
        List<Plan>[] segments = CollectionUtils.split(plans, segmentCount);

        // Deliberately count configured workers, not launched segments, to retain legacy behavior.
        remainingWorkers = new AtomicInteger(workers.length);
        for (int i = 0; i < segments.length; i++) {
            workers[i].initialize(segments[i], network, eventManager);
            threadStarter.start(workers[i]);
        }

        while (remainingWorkers.get() > 0) {
            try {
                sleeper.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void workerCompleted() {
        remainingWorkers.decrementAndGet();
    }
}
