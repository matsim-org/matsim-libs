package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.api.experimental.events.EventsManager;

final class PSimExecutionCoordinator {

    interface Worker extends Runnable {
        void initialize(Collection<Plan> plans, Network network, EventsManager eventManager);
    }

    interface WorkerFactory {
        Worker create();
    }

    interface ExecutorFactory {
        ExecutorService create(int workerCount);
    }

    private final Worker[] workers;
    private final ExecutorFactory executorFactory;

    static PSimExecutionCoordinator create(int workerCount, WorkerFactory workerFactory) {
        return new PSimExecutionCoordinator(workerCount, workerFactory, Executors::newFixedThreadPool);
    }

    PSimExecutionCoordinator(int workerCount, WorkerFactory workerFactory, ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
        workers = new Worker[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = workerFactory.create();
        }
    }

    void execute(Collection<Plan> plans, Network network, EventsManager eventManager) {
        int segmentCount = Math.min(plans.size(), workers.length);
        if (segmentCount == 0) {
            return;
        }
        List<Plan>[] segments = CollectionUtils.split(plans, segmentCount);
        ExecutorService executor = executorFactory.create(segmentCount);
        CompletionService<Boolean> completions = new ExecutorCompletionService<>(executor);
        List<Future<?>> tasks = new ArrayList<>(segmentCount);
        try {
            for (int i = 0; i < segments.length; i++) {
                workers[i].initialize(segments[i], network, eventManager);
                tasks.add(completions.submit(workers[i], Boolean.TRUE));
            }
            executor.shutdown();
            awaitTasks(completions, segmentCount);
        } catch (RuntimeException exception) {
            boolean interrupted = Thread.interrupted();
            cancel(tasks);
            executor.shutdownNow();
            awaitTermination(executor);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            throw exception;
        }
        awaitTermination(executor);
    }

    private static void awaitTasks(CompletionService<Boolean> completions, int taskCount) {
        for (int i = 0; i < taskCount; i++) {
            try {
                completions.take().get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for PSim workers", exception);
            } catch (ExecutionException exception) {
                throw new IllegalStateException("PSim worker failed", exception.getCause());
            }
        }
    }

    private static void cancel(List<Future<?>> tasks) {
        for (Future<?> task : tasks) {
            task.cancel(true);
        }
    }

    private static void awaitTermination(ExecutorService executor) {
        boolean interrupted = false;
        while (!executor.isTerminated()) {
            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException exception) {
                interrupted = true;
                executor.shutdownNow();
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
