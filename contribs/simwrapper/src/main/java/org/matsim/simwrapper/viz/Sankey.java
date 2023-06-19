package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The plug-in creates a sankey/alluvial diagram.
 */
public class Sankey extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String csv;

	public Sankey() {
		super("sankey");
	}
}
