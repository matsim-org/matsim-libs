package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Creates a heatmap for simwrapper.
 */
public class Heatmap extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String dataset;

	/**
	 * Column containing y-value data.
	 */
	@JsonProperty(required = true)
	public String y;

	/**
	 * Array[] containing names of columns with data to
	 * be categorized on the x-axis.
	 */
	@JsonProperty(required = true)
	public List<String> columns;

	/**
	 * Descriptive titles for the x-axis.
	 */
	public String xAxisTitle;

	/**
	 * Descriptive titles for the y-axis.
	 */
	public String yAxisTitle;

	/**
	 * Transpose the heatmap matrix, thus flipping the x
	 * and y axes. Can be useful if your data is stored
	 * one way, but you want it displayed the other.
	 */
	public Boolean flipAxes;

	/**
	 * Show labels on the heatmap.
	 */
	public Boolean showLabels;

	public Heatmap() {
		super("heatmap");
	}
}
