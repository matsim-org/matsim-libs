package org.matsim.simwrapper.viz;

/**
 * A single visualization element.
 */
public abstract class Viz {

	protected final String type;

	public String title;
	public String description;
	public Double height;
	public Double width;
	public String dataset;

	protected Viz(String type) {
		this.type = type;
	}


	public final Viz prop(String name, String value) {
		return this;
	}

}
