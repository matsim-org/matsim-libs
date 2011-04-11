package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;

public class EventsUtils {

	public static EventsManager createEventsManager() {
		return new EventsManagerImpl();
	}

}
