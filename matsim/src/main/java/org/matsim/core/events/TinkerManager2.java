package org.matsim.core.events;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TinkerManager2 implements EventsManager {

    private static final Logger log = Logger.getLogger(TinkerManager2.class);

    private final ExecutorService executorService;
    private final Phaser phaser = new Phaser();
    private final List<EventsManager> managers = new ArrayList<>();
    private final int numberOfThreads;
    private final AtomicBoolean hasThrown = new AtomicBoolean(false);

    private double currentTimestep = Double.NEGATIVE_INFINITY;
    private int handlerCount;
    private List<EventsProcessor> eventsProcessors;

    @Inject
    public TinkerManager2(ParallelEventHandlingConfigGroup config) {
        this(config.getNumberOfThreads() != null ? config.getNumberOfThreads() : 2);
    }

    public TinkerManager2(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        executorService = Executors.newWorkStealingPool(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++)
            managers.add(new EventsManagerImpl());
    }

    @Override
    public void processEvent(Event event) {

        if (event.getTime() < currentTimestep) {
            throw new RuntimeException("Event with time step: " + event.getTime() + " was submitted. But current timestep was: " + currentTimestep + ". Events must be ordered chronologically");
        }

        // make sure all events of the previous time step are processed
        // testing the condition here already, to minimize the number of calls to synchronized method
        if (event.getTime() > currentTimestep)
            setCurrentTimestep(event.getTime());

        var lastQueue = eventsProcessors.get(eventsProcessors.size() - 1);
        lastQueue.addEvent(event);

            executorService.execute(lastQueue);
    }

    @Override
    public void addHandler(EventHandler handler) {
        managers.get(handlerCount % numberOfThreads).addHandler(handler);
        handlerCount++;
    }

    @Override
    public void removeHandler(EventHandler handler) {
        for (var manager : managers)
            manager.removeHandler(handler);
    }

    @Override
    public void resetHandlers(int iteration) {

        currentTimestep = Double.NEGATIVE_INFINITY;

        for (var manager : managers)
            manager.resetHandlers(iteration);
    }

    @Override
    public void initProcessing() {
        log.info("register main thread at phaser");
        phaser.register();

        eventsProcessors = new ArrayList<>();
        EventsProcessor nextQueue = null;

        for (var manager : managers) {
            var queue = new EventsProcessor(manager, phaser, nextQueue, executorService);
            nextQueue = queue;
            eventsProcessors.add(queue);
            manager.initProcessing();
        }
    }

    @Override
    public void afterSimStep(double time) {

        // wait for event handlers to finish processing events generated during time step
        phaser.arriveAndAwaitAdvance();
        throwExceptionIfAnyThreadCrashed();

        for (var manager : managers) {
            manager.afterSimStep(time);
        }
    }

    @Override
    public void finishProcessing() {

        log.info("finishProcessing: Before awaiting all event processes");
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
        log.info("finishProcessing: After waiting for all events processes.");

        if (!hasThrown.get())
            throwExceptionIfAnyThreadCrashed();

        for (var manager : managers) {
            manager.finishProcessing();
        }
    }

    private synchronized void setCurrentTimestep(double time) {

        // test again whether timestep needs to be updated inside the synchronized block, to make sure the await
        // is called only once
        if (time > currentTimestep) {
            // wait for event handlers to process all events from previous time step
            // this waits for events being generated after 'afterSimStep' was called but before the first event of the
            // new time step is thrown.
            phaser.arriveAndAwaitAdvance();
            throwExceptionIfAnyThreadCrashed();
            currentTimestep = time;
        }
    }

    private void throwExceptionIfAnyThreadCrashed() {
        eventsProcessors.stream()
                .filter(EventsProcessor::hadException)
                .findAny()
                .ifPresent(process -> {
                    hasThrown.set(true);
                    throw new RuntimeException(process.getCaughtException());
                });
    }

    private static class EventsProcessor implements Runnable {

        private final EventsManager manager;
        private final Phaser phaser;
        private final EventsProcessor nextProcessor;
        private final ExecutorService executorService;
        private final Queue<Event> eventQueue = new ConcurrentLinkedQueue<>();

        private Exception caughtException;

        private EventsProcessor(EventsManager manager, Phaser phaser, EventsProcessor nextProcessor, ExecutorService executorService) {
            this.manager = manager;
            this.phaser = phaser;
            this.nextProcessor = nextProcessor;
            this.executorService = executorService;
        }

        private boolean hadException() {
            return caughtException != null;
        }

        private Exception getCaughtException() {
            return caughtException;
        }

        void addEvent(Event event) {
            phaser.register();
            eventQueue.add(event);
        }

        @Override
        public synchronized void run() {
            var event = eventQueue.poll();
            notifyNextProcess(event);
            tryProcessEvent(event);
        }

        private void notifyNextProcess(Event event) {
            if (nextProcessor != null) {
                nextProcessor.addEvent(event);
                executorService.execute(nextProcessor);
            }
        }

        private void tryProcessEvent(Event event) {
            try {
                manager.processEvent(event);
            } catch (Exception e) {
                caughtException = e;
            } finally {
                phaser.arriveAndDeregister();
            }
        }
    }
}
