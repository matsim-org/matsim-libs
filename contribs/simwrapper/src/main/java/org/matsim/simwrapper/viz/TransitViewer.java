package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transit viewer for pt schedules.
 */
public class TransitViewer extends Viz {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String transitSchedule;

	@JsonProperty
	public String demand;

	public List<CustomRouteType> customRouteTypes;

	/**
	 * Background layers that can be displayed on the map.
	 * Each layer is identified by a unique name (the map key).
	 */
	@JsonProperty(required = false)
	public Map<String, BackgroundLayer> backgroundLayers;

	public TransitViewer() {
		super("transit");
	}

	/**
	 * Adds a background layer to this visualization.
	 *
	 * @param name  Unique identifier for this layer
	 * @param layer The background layer configuration
	 * @return this TransitViewer for method chaining
	 */
	public TransitViewer addBackgroundLayer(String name, BackgroundLayer layer) {
		if (backgroundLayers == null) {
			backgroundLayers = new LinkedHashMap<>();
		}
		backgroundLayers.put(name, layer);
		return this;
	}

	public static CustomRouteType customRouteType(String label, String color) {
		return new CustomRouteType(label, color);
	}

	public static class CustomRouteType {
		public String label;
		public String color;
		public Boolean hide;
		Match match;

		private CustomRouteType(String label, String color) {
			this.label = label;
			this.color = color;
		}

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
