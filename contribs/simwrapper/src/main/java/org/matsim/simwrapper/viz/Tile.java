package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tile extends Viz{

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String dataset;

	public Tile() {
		super("tile");
	}
}
