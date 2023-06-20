package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Aggregate O/D flows shows aggregated flows between areas defined by a shapefile.
 */
public class AggregateOD extends Viz {
	/**
	 * The path to shp data
	 */
	@JsonProperty(required = true)
	public String shpFile;

	/**
	 * The DBF data must contain a column with the ID of the zones/regions.
	 * This ID will be used to identify the O/D flows in the CSV file
	 */
	@JsonProperty(required = true)
	public String dbfFile;

	/**
	 * The path to csv data
	 */
	@JsonProperty(required = true)
	public String csvFile;

	/**
	 * Coordinate projection, such as "EPSG:31468" or "GK4"
	 */
	@JsonProperty(required = true)
	public String projection;

	/**
	 * Factor to scale all values -- to handle 1% or 10% scenarios, for example
	 */
	@JsonProperty(required = true)
	public Double scaleFactor;

	/**
	 * Data column in shapefile which contains the ID for regions/zones(default "id")
	 */
	public String idColumn;

	/**
	 * Starting width scaling of lines
	 */
	@JsonProperty(required = true)
	public Double lineWidth;


	public AggregateOD() {
		super("aggregate");
	}
}
