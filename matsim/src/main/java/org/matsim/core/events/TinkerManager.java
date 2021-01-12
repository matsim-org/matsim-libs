package org.matsim.core.events;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

public class TinkerManager implements EventsManager {

    private static final Logger log = Logger.getLogger(TinkerManager.class);

    private final ExecutorService executorService;
    private final Phaser phaser = new Phaser();
    private final List<EventsManager> managers = new ArrayList<>();
    private final int numberOfThreads;
    private final AtomicBoolean hasThrown = new AtomicBoolean(false);

    private double currentTimestep;
    private int handlerCount;
    private List<ProcessEvents> processEvents;

    @Inject
    public TinkerManager(ParallelEventHandlingConfigGroup config) {
        this(config.getNumberOfThreads() != null ? config.getNumberOfThreads() : 1);
    }

    public TinkerManager(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        executorService = Executors.newFixedThreadPool(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++)
            managers.add(new EventsManagerImpl());
    }

    private static class ProcessEvents implements Runnable {

        private final LinkedBlockingQueue<Event> queue = new LinkedBlockingQueue<>();
        private final ProcessEvents nextProcessor;
        private final EventsManager manager;
        private final Phaser phaser;

        private Exception caughtException;

        boolean hadException() {
            return caughtException != null;
        }

        Exception getCaughtException() {
            return caughtException;
        }

        void addEvent(Event event) {
            phaser.register();
            this.queue.add(event);
        }

        private ProcessEvents(EventsManager manager, Phaser phaser, ProcessEvents nextProcessor) {
            this.nextProcessor = nextProcessor;
            this.manager = manager;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    var event = queue.take();
                    if (event.getEventType().equals(TerminateEvent.EVENT_TYPE)) {

                        phaser.arriveAndDeregister();
                        break;
                    } else {
                        notifyNextProcess(event);
                        tryProcessEvent(event);
                    }
                }
            } catch (InterruptedException e) {
                caughtException = e;
            }
        }

        private void notifyNextProcess(Event event) {
            if (nextProcessor != null)
                nextProcessor.addEvent(event);
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

    private static class TerminateEvent extends Event {

        private static final String EVENT_TYPE = "terminate";

        public TerminateEvent(double time) {
            super(time);
        }

        @Override
        public String getEventType() {
            return EVENT_TYPE;
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
        processEvents.stream()
                .filter(ProcessEvents::hadException)
                .findAny()
                .ifPresent(process -> {
                    hasThrown.set(true);
                    throw new RuntimeException(process.getCaughtException());
                });
    }

    private void shutDownThreadPool() {
        processEvent(new TerminateEvent(currentTimestep));
        executorService.shutdown();
    }

    @Override
    public void processEvent(Event event) {

        if (event.getTime() < currentTimestep) {
            throw new RuntimeException("Event with time step: " + event.getTime() + " was submitted. But current timestep was: " + currentTimestep + ". Events must be ordered chronologically");
        }

        if (event.getTime() > currentTimestep)
            setCurrentTimestep(event.getTime());

        processEvents.get(processEvents.size() - 1).addEvent(event);
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

        log.info("register main thread at phaser");
        phaser.register();

        ProcessEvents nextProcessor = null;
        processEvents = new ArrayList<>();

        for (var manager : managers) {
            var process = new ProcessEvents(manager, phaser, nextProcessor);
            nextProcessor = process;
            executorService.execute(process);
            processEvents.add(process);
            manager.initProcessing();
        }
    }

    @Override
    public void afterSimStep(double time) {

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

        shutDownThreadPool();
        if (!hasThrown.get())
            throwExceptionIfAnyThreadCrashed();

        for (var manager : managers) {
            manager.finishProcessing();
        }
    }
}
