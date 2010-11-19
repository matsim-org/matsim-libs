package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

public class EventHandlerTaskManager extends TaskManager {

	private EventHandler eventHandler;

	public EventHandlerTaskManager(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		EventsManager eventsManager = pipeTasks.getEventsManager();
		eventsManager.addHandler(eventHandler);
	}

}
