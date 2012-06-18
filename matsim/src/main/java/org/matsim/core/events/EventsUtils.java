package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;

public class EventsUtils {

	public static EventsManager createEventsManager() {
		return new EventsManagerImpl();
	}
	
	
	/**
	 * select if single cpu handler to use or parallel
	 */
	public static EventsManager createEventsManager(Config config) {
		EventsManager events;
		final String PARALLEL_EVENT_HANDLING = "parallelEventHandling";
		final String NUMBER_OF_THREADS = "numberOfThreads";
		final String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
		String numberOfThreads = config.findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
		String estimatedNumberOfEvents = config.findParam(PARALLEL_EVENT_HANDLING, ESTIMATED_NUMBER_OF_EVENTS);

		if (numberOfThreads != null) {
			int numOfThreads = Integer.parseInt(numberOfThreads);
			// the user wants to user parallel events handling
			if (estimatedNumberOfEvents != null) {
				int estNumberOfEvents = Integer.parseInt(estimatedNumberOfEvents);
				events = new ParallelEventsManagerImpl(numOfThreads, estNumberOfEvents);
			} else {
				events = new ParallelEventsManagerImpl(numOfThreads);
			}
		} else {
			events = (EventsManagerImpl) EventsUtils.createEventsManager();
		}
		return events;
	}

}
