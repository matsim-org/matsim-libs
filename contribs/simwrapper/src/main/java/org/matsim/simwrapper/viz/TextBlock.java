package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TODO: java doc
 */
public class TextBlock extends Viz {

	/**
	 * TODO: java doc
	 */
	@JsonProperty(required = true)
	public String file;

	public TextBlock() {
		super("text");
	}
}
