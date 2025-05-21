package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

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


	@JsonProperty(required = false)
	public int radius;

	private Map<String, Object> colorRamp;

	/**
	 * Set the color ramp name.
	 */
	public XYTime setColorRamp(String ramp) {
		colorRamp = Map.of("ramp", ramp);
		return this;
	}

	/**
	 * Sets the full color ramps settings.
	 */
	public XYTime setColorRamp(double[] breakpoints, String[] colors) {
		colorRamp = Map.of("breakpoints", breakpoints, "fixedColors", colors);
		return this;
	}

	public XYTime setColorRamp(String ramp, int steps, boolean reverse) {
		colorRamp = Map.of("ramp", ramp, "reverse", reverse, "steps", steps);
		return this;
	}

	public XYTime() {
		super("xytime");
	}
}
