package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public class CarrierViewer extends Viz {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String carriers;

	/**
	 * Background layers that can be displayed on the map.
	 * Each layer is identified by a unique name (the map key).
	 */
	@JsonProperty(required = false)
	public Map<String, BackgroundLayer> backgroundLayers;

	public CarrierViewer() {
		super("carriers");
	}

	/**
	 * Adds a background layer to this visualization.
	 *
	 * @param name  Unique identifier for this layer
	 * @param layer The background layer configuration
	 * @return this CarrierViewer for method chaining
	 */
	public CarrierViewer addBackgroundLayer(String name, BackgroundLayer layer) {
		if (backgroundLayers == null) {
			backgroundLayers = new LinkedHashMap<>();
		}
		backgroundLayers.put(name, layer);
		return this;
	}

}
