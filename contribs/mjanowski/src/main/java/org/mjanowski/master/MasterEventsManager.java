package org.mjanowski.master;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.ParallelEventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;

public class MasterEventsManager implements EventsManager {

    private EventsManager delegate;

    @Inject
    public MasterEventsManager(Config config) {
        ParallelEventHandlingConfigGroup eventHandlingConfig = config.parallelEventHandling();
        int threadsNumber = eventHandlingConfig != null && eventHandlingConfig.getNumberOfThreads() != null
                ? eventHandlingConfig.getNumberOfThreads() : 2;
        delegate = new ParallelEventsManagerImpl(threadsNumber);
    }

    @Override
    public void processEvent(Event event) {
        delegate.processEvent(event);
    }

    @Override
    public void addHandler(EventHandler handler) {
        delegate.addHandler(handler);
    }

    @Override
    public void removeHandler(EventHandler handler) {
        delegate.removeHandler(handler);
    }

    @Override
    public void resetHandlers(int iteration) {
        delegate.resetHandlers(iteration);
    }

    @Override
    public void initProcessing() {
        delegate.initProcessing();
    }

    @Override
    public void afterSimStep(double time) {
        delegate.afterSimStep(time);
    }

    @Override
    public void finishProcessing() {
        delegate.finishProcessing();
    }
}
