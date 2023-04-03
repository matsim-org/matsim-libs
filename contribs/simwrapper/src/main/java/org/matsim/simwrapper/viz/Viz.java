package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single visualization element.
 */
public abstract class Viz {

	protected final String type;

	@JsonProperty(required = true)
	public String title;
	public String description;
	public Double height;
	public Double width;

	protected Viz(String type) {
		this.type = type;
	}


	public final Viz prop(String name, String value) {
		return this;
	}

}
