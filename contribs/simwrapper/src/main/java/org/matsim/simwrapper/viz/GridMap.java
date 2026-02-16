package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * The Tile plug-in creates an overview of important key figures.
 */
public class GridMap extends VizMap<GridMap> {

	/**
	 * Defines the time selector types
	 */
	public enum TimeSelector {
		discrete,
		slider
	}

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
	 * Set to true for this map to have independent center/zoom/motion
	 */
	public Boolean mapIsIndependent;

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

	/**
	 * The secondValueColumn defines the column to compare with valueColumn for difference plot.
	 */
	public String secondValueColumn;

	/**
	 * If true, the map will show the difference plot between valueColumn and secondValueColumn.
	 */
	public boolean diff;

	private Map<String, Object> colorRamp;

	public GridMap() {
		super("gridmap");
	}

	/**
	 * Set the color ramp name.
	 */
	public GridMap setColorRamp(String ramp) {
		colorRamp = new HashMap<>(Map.of("ramp", ramp));
		return this;
	}

	/**
	 * Defines which type of time selector to use.
	 * Possible values are `discrete` and `slider`.
	 */
	@JsonProperty(required = false)
	public TimeSelector timeSelector;

	/**
	 * Sets the full color ramps settings.
	 */
	public GridMap setColorRamp(double[] breakpoints, String[] colors) {
		colorRamp = new HashMap<>(Map.of("breakpoints", breakpoints, "fixedColors", colors));
		return this;
	}

	public GridMap setColorRamp(String ramp, int steps, boolean reverse) {
		colorRamp = new HashMap<>(Map.of("ramp", ramp, "reverse", reverse, "steps", steps));
		return this;
	}

	public GridMap setColorRampBounds(boolean boundsEnabled, double lowerBound, double upperBound) {
		if (colorRamp == null || colorRamp.isEmpty()) {
			throw new IllegalStateException("Color ramp must be set before setting bounds.");
		}

		colorRamp.put("boundsEnabled", boundsEnabled);
		colorRamp.put("lowerBound", lowerBound);
		colorRamp.put("upperBound", upperBound);
		return this;
	}

}
