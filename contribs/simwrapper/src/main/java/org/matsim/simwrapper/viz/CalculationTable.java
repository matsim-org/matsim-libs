package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Creates a Calculation Table for simwrapper.
 */
public class CalculationTable extends Viz {

	/**
	 * Path to the config file.
	 */
	@JsonProperty(required = true)
	public String configFile;

	public CalculationTable() {
		super("table");
	}
}
