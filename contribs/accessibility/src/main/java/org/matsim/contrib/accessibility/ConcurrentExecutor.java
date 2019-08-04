package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Utility class to run multithreaded tasks
 * @author Nico
 * @param <T> object type of expected results of tasks. I.e. Future < T >
 */
public class ConcurrentExecutor<T> {

    private final List<Callable<T>> tasks = new ArrayList<>();
    private final ExecutorService service;

    public static <T> ConcurrentExecutor<T> cachedService() {
        return new ConcurrentExecutor<>(Executors.newCachedThreadPool());
    }

    public static <T> ConcurrentExecutor<T> fixedPoolService(int numberOfThreads) {
        return new ConcurrentExecutor<>(Executors.newFixedThreadPool(numberOfThreads));
    }

    public static <T> List<T> runTasks(List<Callable<T>> tasks) {
        return new ConcurrentExecutor<T>(Executors.newCachedThreadPool()).submitTasksAndWaitForCompletion(tasks);
    }

    private ConcurrentExecutor(ExecutorService service) {
        this.service = service;
    }

    public void addTaskToQueue(Callable<T> task) {
        this.tasks.add(task);
    }

    public List<T> submitTasksAndWaitForCompletion(Collection<Callable<T>> tasks) {
        try {
            return service.invokeAll(tasks).stream().map(tFuture -> {
                try {
                    return tFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Future<T> submitTask(Callable<T> task) {
        return service.submit(task);
    }

    public T submitTaskAndWaitForCompletion(Callable<T> task) {
        Future<T> result = service.submit(task);
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Future<T>> execute() {
        try {
            return service.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}