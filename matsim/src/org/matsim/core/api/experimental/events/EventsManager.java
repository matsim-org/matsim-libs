package org.matsim.core.api.experimental.events;

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.events.handler.EventHandler;


public interface EventsManager extends MatsimToplevelContainer {

	public EventsFactory getFactory();

	public void processEvent(final BasicEvent event);

	public void addHandler(final EventHandler handler);

}