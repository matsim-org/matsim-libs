package org.matsim.core.events;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class TinkerManager implements EventsManager {

    private static final Logger log = Logger.getLogger(TinkerManager.class);

    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final Phaser phaser = new Phaser();
    private final Map<EventHandler, EventsManager> managers = new HashMap<>();
    private final AtomicDouble currentTimestep = new AtomicDouble();

    private synchronized void awaitProcessingAndSetNewTimestep(double time) {
        if (time > currentTimestep.get()) {

            // This is the first event for a new timestep. Wait for processing tasks to finish events of the
            // previous timestep.
            log.info("New timestep: " + time + " awaiting events processing to finish");
            phaser.arriveAndAwaitAdvance();
            log.info("all events processed new timestep: " + time);

            // then set up the next timestep and keep processing events.
            currentTimestep.set(time);
        }
    }

    @Override
    public void processEvent(Event event) {

        awaitProcessingAndSetNewTimestep(event.getTime());

        log.info("submitting event to managers: " + event.getEventType() + " " + event.getTime());
        for (EventsManager manager : managers.values()) {

            // register the new task before submitting it to the thread pool
            phaser.register();
            executorService.execute(() -> {

                // process event and tell phaser that this task has been processed
                log.info("processing event: " + event.getEventType() + " " + event.getTime());
                manager.processEvent(event);
                log.info("finished processing event: " + event.getEventType() + " " + event.getTime() + " deregistering from phaser");
                phaser.arriveAndDeregister();
            });
        }
    }

    @Override
    public void addHandler(EventHandler handler) {
        var manager = new EventsManagerImpl();
        manager.addHandler(handler);
        managers.put(handler, manager);
    }

    @Override
    public void removeHandler(EventHandler handler) {
        managers.remove(handler);
    }

    @Override
    public void resetHandlers(int iteration) {
        for (var manager : managers.values())
            manager.resetHandlers(iteration);
    }

    @Override
    public void initProcessing() {

        log.info("register main thread at phaser");
        phaser.register();
        for (var manager : managers.values()) {
            manager.initProcessing();
        }
    }

    @Override
    public void afterSimStep(double time) {

        // wait for all events being processed after the mobsim has finished
        // some mobsimaftersimstep listeners rely on all mobsim events being processed.
        log.info("afterSimStep awaiting all events to be processed");
        phaser.arriveAndAwaitAdvance();
        log.info("all events are processed. calling afteSimStep for each manager");

        for (var manager: managers.values()) {
            manager.afterSimStep(time);
        }
    }

    @Override
    public void finishProcessing() {

        log.info("finish processing. Awaiting all events to be processed");
        phaser.awaitAdvance(phaser.arriveAndDeregister());
        log.info("all events are processed. Deregistered main thread from phaser. Calling finish processing on all managers");
        for (var manager: managers.values()) {
            manager.finishProcessing();
        }
    }
}
