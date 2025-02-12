package org.matsim.contrib.profiling.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Profiling events are created with MATSim EventListeners and started / committed during separate methods.
 * This error event is created in case the call order of such methods is different than expected.
 */
@Label("Error with events")
@Description("Something unexpected occurred when trying to create events")
@Category("MATSim")
public class JFRProfilingErrorEvent extends Event {

	@Label("Error description")
	private final String error;

	public JFRProfilingErrorEvent(String error) {
		this.error = error;
	}

}
