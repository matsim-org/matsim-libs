package org.matsim.core.api.experimental.events;

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.core.api.internal.MatsimToplevelContainer;


public interface Events extends MatsimToplevelContainer {

	public EventsBuilder getBuilder();

	public void processEvent(final BasicEvent event);

}