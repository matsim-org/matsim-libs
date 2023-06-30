package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The abstract class Plot combines the attributes dataset, x, y, legendName, xAxisName and yAxisName used by Bubble and Scatter Plot.
 */
public abstract class Plot extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String dataset;

	/**
	 * The column containing x-values.
	 */
	public String x;

	/**
	 * The column containing y-values.
	 */
	public String y;

	/**
	 * Array of strings. Legend titles for each line.
	 * The column names will be used if this is omitted.
	 */
	public String legendName;

	/**
	 * Label for the x-axes.
	 */
	public String xAxisName;

	/**
	 * Label for the y-axes.
	 */
	public String yAxisName;

	protected Plot(String type) {
		super(type);
	}
}
