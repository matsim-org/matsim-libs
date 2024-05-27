package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Tile plug-in creates an overview of important key figures.
 */
public class XYTime extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String file;

	public XYTime() {
		super("xytime");
	}
}
