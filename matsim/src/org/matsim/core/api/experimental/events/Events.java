package org.matsim.core.api.experimental.events;

import org.matsim.api.basic.v01.events.BasicEvent;


public interface Events {

	public EventsBuilder getBuilder();

	public void processEvent(final BasicEvent event);

}