package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for map-based visualizations that support background layers.
 * Extends {@link Viz} with functionality to add and manage background layers.
 *
 * @param <T> The specific type of VizMap for method chaining.
 */
public abstract class VizMap<T extends VizMap<T>> extends Viz {

	/**
	 * Background layers that can be displayed on the map.
	 * Each layer is identified by a unique name (the map key).
	 */
	@JsonProperty(required = false)
	public Map<String, BackgroundLayer> backgroundLayers;

	protected VizMap(String type) {
		super(type);
	}

	/**
	 * Adds a background layer to this visualization.
	 *
	 * @param name  Unique identifier for this layer
	 * @param layer The background layer configuration
	 * @return this VizMap for method chaining
	 */
	@SuppressWarnings("unchecked")
	public T addBackgroundLayer(String name, BackgroundLayer layer) {
		if (backgroundLayers == null) {
			backgroundLayers = new LinkedHashMap<>();
		}
		backgroundLayers.put(name, layer);
		return (T) this;
	}
}
