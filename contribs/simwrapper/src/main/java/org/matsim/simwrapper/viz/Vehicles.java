package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Creates a vehicle (DRT) animation visualization for simwrapper.
 */
public class Vehicles extends Viz {

	/**
	 * Sets the path of the processed trips file in JSON format
	 */
	@JsonProperty(required = true)
	public String drtTrips;

	/**
	 * Sets the projection of the map. E.g. EPSG:31468
	 */
	public String projection;

	/**
	 * Sets the long/lat center coordinates.
	 */
	public double[] center;

	/**
	 * Sets the map zoom level.
	 */
	public Double zoom;

	/**
	 * Set to true for this map to have independent center/zoom/motion
	 */
	public Boolean mapIsIndependent;

	/**
	 * Set to true to animate vehicles on the left side of road centerlines
	 */
	public Boolean leftside;

	public Vehicles() {
		super("vehicles");
	}
}
