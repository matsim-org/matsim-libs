package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * The Tile plug-in creates an overview of important key figures.
 */
public class XYTime extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String file;

	/**
	 * The radius of the points.
	 */
	@JsonProperty(required = false)
	public Double radius;

	/**
	 * The name of the color ramp.
	 */
	@JsonProperty(required = false)
	public String colorRamp;

	/**
	 * The number of buckets of the color ramp.
	 */
	@JsonProperty(required = false)
	public Integer buckets;

	/**
	 * The exponent of the color ramp.
	 */
	@JsonProperty(required = false)
	public Integer exponent;

	/**
	 * The minimum value of the color ramp.
	 */
	@JsonProperty(required = false)
	public Integer clipMax;

	/**
	 * Breakpoints can either be a list of values or a map with colors and values.
	 */
	@JsonProperty(required = false)
	private Object breakpoints;

	public XYTime() {
		super("xytime");
	}

	/**
	 * Sets breakpoints as a map when colors are provided.
	 * <p>
	 * The number of colors must be one less than the number of values.
	 */
	public XYTime setBreakpoints(String[] colors, double... values) {
		this.breakpoints = Map.of(
			"colors", colors,
			"values", values
		);
		return this;
	}

	/**
	 * Set breakpoints as a simple list.
	 */
	public XYTime setBreakpoints(double... values) {
		this.breakpoints = List.of(values);
		return this;
	}

}
