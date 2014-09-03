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
        return new SimStepParallelEventsManagerImpl(1);
	}

}
