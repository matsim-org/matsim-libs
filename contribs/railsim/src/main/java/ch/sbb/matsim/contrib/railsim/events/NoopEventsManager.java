package ch.sbb.matsim.contrib.railsim.events;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * This is a dummy events manager that does nothing.
 */
public class NoopEventsManager implements EventsManager {
	@Override
	public void processEvent(Event event) {

	}

	@Override
	public void addHandler(EventHandler handler) {

	}

	@Override
	public void removeHandler(EventHandler handler) {

	}

	@Override
	public void resetHandlers(int iteration) {

	}

	@Override
	public void initProcessing() {

	}

	@Override
	public void afterSimStep(double time) {

	}

	@Override
	public void finishProcessing() {

	}
}
