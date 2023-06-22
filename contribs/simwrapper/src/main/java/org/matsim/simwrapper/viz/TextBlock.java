package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The TextBlock Plug-In displays a markdown file on simwrapper.
 */
public class TextBlock extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String file;

	public TextBlock() {
		super("text");
	}
}
