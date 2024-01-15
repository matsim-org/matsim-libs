package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Displays an xml file.
 */
public class XML extends Viz {

	/**
	 * Path to the xml that should be shown.
	 */
	@JsonProperty(required = true)
	public String file;

	/**
	 * Descriptive how many levels should be unfolded at the beginning.
	 */
	public Integer unfoldLevel;

	public XML() {
		super("xml");
	}
}
