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

	public CustomRouteType addCustomRouteType(String label, String color, boolean hide) {
		CustomRouteType crt = new CustomRouteType();
		crt.label = label;
		crt.color = color;
		crt.hide = hide;
		customRouteTypes.add(crt);
		return crt;
	}

	public static class CustomRouteType {
		public String label;
		public String color;
		public Boolean hide;
		Match match;

		public CustomRouteType addMatchTransportMode(String... transportMode) {
			if (match == null)
				match = new Match();

			match.transportMode = transportMode;
			return this;
		}

		public CustomRouteType addMatchId(String... id) {
			if (match == null)
				match = new Match();

			match.id = id;
			return this;
		}

		public CustomRouteType addMatchGtfsRouteType(Integer... gtfsRouteType) {
			if (match == null)
				match = new Match();

			match.gtfsRouteType = gtfsRouteType;
			return this;
		}
	}

	private static class Match {
		Object transportMode;
		Object id;
		Object gtfsRouteType;
	}
}
