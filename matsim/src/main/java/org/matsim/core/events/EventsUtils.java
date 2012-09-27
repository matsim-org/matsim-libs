package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;

public class EventsUtils {

	public static EventsManager createEventsManager() {
		return new EventsManagerImpl();
	}
	
	public static EventsManager createEventsManager(Config config) {
		EventsManager events;

		Integer numberOfThreads = config.parallelEventHandling().getNumberOfThreads() ;
		Long estimatedNumberOfEvents = config.parallelEventHandling().getEstimatedNumberOfEvents() ;
		Boolean synchronizeOnSimSteps = config.parallelEventHandling().getSynchronizeOnSimSteps();		
		
		if (numberOfThreads != null) {
			if (synchronizeOnSimSteps != null && synchronizeOnSimSteps) {
				events = new SimStepParallelEventsManagerImpl(numberOfThreads);
			} else {
				if (estimatedNumberOfEvents != null) {
					events = new ParallelEventsManagerImpl(numberOfThreads, estimatedNumberOfEvents);
				} else {
					events = new ParallelEventsManagerImpl(numberOfThreads);
				}				
			}
		} else {
			events = new EventsManagerImpl();
		}

		return events;
	}

}
