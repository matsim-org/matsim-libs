package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

abstract class Chart extends Viz {

	/**
	 * The filepath containing the data
	 */
	@JsonProperty(required = true)
	public String dataset;

	/**
	 * The column containing x-values.
	 */
	@JsonProperty(required = true)
	public String x;

	/**
	 * Array of strings. List the column names of the columns
	 * which have the values to be graphed. Each element will
	 * be its own line/color. Example: ['distance', 'duration']
	 */
	@JsonProperty(required = true)
	public List<String> columns;

	/**
	 * If set to true, only the last row of the datafile will be
	 * used to build the pie chart. For example, this is useful
	 * for MATSim outputs which list every iteration's output,
	 * if you are only in the final iteration.
	 */
	public boolean useLastRow;

	/**
	 * Array of strings. Legend titles for each line. The column
	 * names will be used if this is omitted.
	 */
	public List<String> legendName;

	/**
	 * Label for the x-axes.
	 */
	public String xAxisName;

	/**
	 * Label for the y-axes.
	 */
	public String yAxisName;


	protected Chart(String type) {
		super(type);
	}
}
