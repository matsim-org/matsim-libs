package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * The Tile plug-in creates an overview of important key figures.
 */
public class GridMap extends Viz {

	/**
	 * The unit of the values.
	 */
	@JsonProperty(required = false)
	public String unit;

	/**
	 * The center of the map.
	 */
	@JsonProperty(required = false)
	public Double zoom;

	/**
	 * The center of the map.
	 */
	@JsonProperty(required = false)
	public double[] center;

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String file;

	/**
	 * The projection containing the projection e.g. EPSG:25832.
	 */
	@JsonProperty(required = true)
	public String projection;

	/**
	 * The cellSize containing the cellSize in meters.
	 */
	@JsonProperty(required = false)
	public Integer cellSize;

	/**
	 * The opacity defines the opacity of the grids (between 0 and 1);
	 */
	@JsonProperty(required = false)
	public Double opacity;

	/**
	 * The maxHeight defines the maximum height of the "towers".
	 */
	@JsonProperty(required = false)
	public Integer maxHeight;

	/**
	 * The valueColumn defines the column with the values. The default value is `value`.
	 */
	@JsonProperty(required = false)
	public String valueColumn;

	private Map<String, Object> colorRamp;

	public GridMap() {
		super("gridmap");
	}

	/**
	 * Set the color ramp name.
	 */
	public GridMap setColorRamp(String ramp) {
		colorRamp = Map.of("ramp", ramp);
		return this;
	}

	/**
	 * Sets the full color ramps settings.
	 */
	public GridMap setColorRamp(double[] breakpoints, String[] colors) {
		colorRamp = Map.of("breakpoints", breakpoints, "fixedColors", colors);
		return this;
	}

	public GridMap setColorRamp(String ramp, int steps, boolean reverse) {
		colorRamp = Map.of("ramp", ramp, "reverse", reverse, "steps", steps);
		return this;
	}

}
