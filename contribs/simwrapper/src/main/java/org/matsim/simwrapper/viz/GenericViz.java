package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic viz element that can hold any properties.
 */
public final class GenericViz extends Viz {

	public String type;
	@JsonIgnore
	private final Map<String, Object> properties = new HashMap<>();

	public GenericViz() {
		super(null);
	}

	/**
	 * Set a property of the element.
	 */
	public GenericViz set(String key, Object value) {
		properties.put(key, value);
		return this;
	}

	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}

}
