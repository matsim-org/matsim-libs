package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;

public class EventsUtils {

    public static EventsManager createEventsManager() {
        return new EventsManagerImpl();
    }

    public static EventsManager createEventsManager(Config config) {
        Integer numberOfThreads = config.parallelEventHandling().getNumberOfThreads();
        Long estimatedNumberOfEvents = config.parallelEventHandling().getEstimatedNumberOfEvents();
        Boolean synchronizeOnSimSteps = config.parallelEventHandling().getSynchronizeOnSimSteps();
        Boolean oneThreadPerHandler = config.parallelEventHandling().getOneThreadPerHandler();
        if (oneThreadPerHandler != null && oneThreadPerHandler) {
        	 if (synchronizeOnSimSteps != null) return new ParallelEventsManager(synchronizeOnSimSteps);
        	 else return new ParallelEventsManager(true);
        }
        if (numberOfThreads != null) {
            if (synchronizeOnSimSteps != null && synchronizeOnSimSteps) {
                return new SimStepParallelEventsManagerImpl(numberOfThreads);
            } else {
                if (estimatedNumberOfEvents != null) {
                    return new ParallelEventsManagerImpl(numberOfThreads, estimatedNumberOfEvents);
                } else {
                    return new ParallelEventsManagerImpl(numberOfThreads);
                }
            }
        }
        return new SimStepParallelEventsManagerImpl();
    }

    /**
     * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
     * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
     * SynchronizedEventsManagerImpl.
     */
    public static EventsManager getParallelFeedableInstance(EventsManager events) {
    	if (events instanceof SimStepParallelEventsManagerImpl) {
    		return events;
    	} else if (events instanceof ParallelEventsManager) {
    		return events;
    	}
    	else if (events instanceof SynchronizedEventsManagerImpl) {
    		return events;
    	} else {
    		return new SynchronizedEventsManagerImpl(events);
    	}
    }
}