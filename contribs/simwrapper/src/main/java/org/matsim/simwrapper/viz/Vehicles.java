package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a vehicle (DRT) animation visualization for simwrapper.
 */
public class Vehicles extends Viz {

	/**
	 * Sets the path of the processed trips file in JSON format
	 */
	@JsonProperty(required = true)
	public String drtTrips;

	/**
	 * Sets the projection of the map. E.g. EPSG:31468
	 */
	public String projection;

	/**
	 * Sets the long/lat center coordinates.
	 */
	public double[] center;

	/**
	 * Sets the map zoom level.
	 */
	public Double zoom;

	/**
	 * Set to true for this map to have independent center/zoom/motion
	 */
	public Boolean mapIsIndependent;

	/**
	 * Set to true to animate vehicles on the left side of road centerlines
	 */
	public Boolean leftside;

	/**
	 * Background layers that can be displayed on the map.
	 * Each layer is identified by a unique name (the map key).
	 */
	@JsonProperty(required = false)
	public Map<String, BackgroundLayer> backgroundLayers;

	public Vehicles() {
		super("vehicles");
	}

	/**
	 * Adds a background layer to this visualization.
	 *
	 * @param name  Unique identifier for this layer
	 * @param layer The background layer configuration
	 * @return this Vehicles for method chaining
	 */
	public Vehicles addBackgroundLayer(String name, BackgroundLayer layer) {
		if (backgroundLayers == null) {
			backgroundLayers = new LinkedHashMap<>();
		}
		backgroundLayers.put(name, layer);
		return this;
	}
}
