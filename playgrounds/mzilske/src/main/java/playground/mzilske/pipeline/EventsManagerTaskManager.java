package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;

public class EventsManagerTaskManager extends TaskManager {

	private EventsManager events;

	public EventsManagerTaskManager(EventsManager events) {
		this.events = events;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		pipeTasks.setEventsManager(events);
	}

}
