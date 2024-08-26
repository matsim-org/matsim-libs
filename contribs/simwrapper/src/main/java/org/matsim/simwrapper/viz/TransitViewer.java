package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transit viewer for pt schedules.
 */
public class TransitViewer extends Viz {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String transitSchedule;

	public TransitViewer() {
		super("transit");
	}
}
