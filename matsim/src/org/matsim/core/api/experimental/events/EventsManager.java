package org.matsim.core.api.experimental.events;

import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.events.handler.EventHandler;


public interface EventsManager extends MatsimToplevelContainer {

	public EventsFactory getFactory();

	public void processEvent(final Event event);

	public void addHandler(final EventHandler handler);
	
	public void removeHandler(final EventHandler handler);

}