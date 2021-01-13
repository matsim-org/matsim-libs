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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

public class TinkerManager2 implements EventsManager {

    private static final Logger log = Logger.getLogger(TinkerManager2.class);

    private final ExecutorService executorService;
    private final Phaser phaser = new Phaser(1);
    private final List<EventsManager> managers = new ArrayList<>();
    private final List<EventsProcessor> eventsProcessors = new ArrayList<>();
    private final int numberOfThreads;
    private final AtomicBoolean hasThrown = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private double currentTimestep = Double.NEGATIVE_INFINITY;
    private int handlerCount;

    @Inject
    public TinkerManager2(ParallelEventHandlingConfigGroup config) {
        this(config.getNumberOfThreads() != null ? config.getNumberOfThreads() : 2);
    }

    public TinkerManager2(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        this.executorService = Executors.newWorkStealingPool(numberOfThreads);

        EventsProcessor nextProcessor = null;

        for (int i = 0; i < numberOfThreads; i++){
            var manager = new EventsManagerImpl();
            managers.add(manager);
            var processor = new EventsProcessor(manager, phaser, nextProcessor, executorService);
            nextProcessor = processor;
            eventsProcessors.add(processor);
        }
    }

    @Override
    public void processEvent(Event event) {

        // only test for order, if we are initialized. Some code in some contribs emmits unordered events in between iterations
        if (event.getTime() < currentTimestep && isInitialized.get()) {
            throw new RuntimeException("Event with time step: " + event.getTime() + " was submitted. But current timestep was: " + currentTimestep + ". Events must be ordered chronologically");
        }

        // testing the condition here already, to minimize the number of calls to synchronized method
        if (event.getTime() > currentTimestep)
            setCurrentTimestep(event.getTime());

        // taking last processor because of initialization logic
        var processor = eventsProcessors.get(eventsProcessors.size() - 1);
        processor.addEvent(event);
        executorService.execute(processor);
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
        for (var manager : managers)
            manager.resetHandlers(iteration);
    }

    @Override
    public void initProcessing() {

        // wait for processing of events which were emitted in between iterations
        awaitProcessingOfEvents();
        isInitialized.set(true);
        currentTimestep = Double.NEGATIVE_INFINITY;

        for (var manager : managers) {
            manager.initProcessing();
        }
    }

    @Override
    public void afterSimStep(double time) {

        // await processing of events emitted during a simulation step
        awaitProcessingOfEvents();

        for (var manager : managers) {
            manager.afterSimStep(time);
        }
    }

    @Override
    public void finishProcessing() {

        log.info("finishProcessing: Before awaiting all event processes");
        phaser.arriveAndAwaitAdvance();
        log.info("finishProcessing: After waiting for all events processes.");

        isInitialized.set(false);

        if (!hasThrown.get())
            throwExceptionIfAnyThreadCrashed();

        for (var manager : managers) {
            manager.finishProcessing();
        }
    }

    private synchronized void setCurrentTimestep(double time) {

        // this test must be inside synchronized block to make sure await is only called once
        if (time > currentTimestep) {
            // wait for event handlers to process all events from previous time step including events emitted after 'afterSimStep' was called
            awaitProcessingOfEvents();
            currentTimestep = time;
        }
    }

    private void awaitProcessingOfEvents() {
        phaser.arriveAndAwaitAdvance();
        throwExceptionIfAnyThreadCrashed();
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

        /**
         * This method must be synchronized to make sure the events are passed to the events manager in the order we've
         * received them.
         */
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
