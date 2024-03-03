package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single visualization element.
 */
public abstract class Viz {

	@JsonProperty(index = 0)
	protected final String type;

	@JsonProperty(index = 1, required = true)
	public String title;

	@JsonProperty(index = 2)
	public String description;
	public Double height;
	public Double width;

	protected Viz(String type) {
		this.type = type;
	}

}
