package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Transit viewer for pt schedules.
 */
public class TransitViewer extends Viz {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String transitSchedule;

	public List<CustomRouteType> customRouteTypes;

	public TransitViewer() {
		super("transit");
	}

	public void addCustomRouteType(String name, String color, List<Integer> routeTypes) {
		CustomRouteType crt = new CustomRouteType();
		crt.label = name;
		crt.color = color;
		customRouteTypes.add(crt);
	}

	private class CustomRouteType {
		String label;
		String color;
		Match match;
	}

	public void addMatch(String transportMode, String id, List<Integer> gtfsRouteType) {
		Match m = new Match();
		m.transportMode = transportMode;
		m.id = id;
		m.gtfsRouteType = gtfsRouteType;
	}

	private class Match {
		String transportMode;
		String id;
		List<Integer> gtfsRouteType;
	}
}
