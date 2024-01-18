package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces a hexagon plot on a map.
 */
public final class Hexagons extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String file;

	/**
	 * The projection of the network.
	 */
	@JsonProperty(required = true)
	public String projection;

	public double[] center;

	public Double zoom;

	public Double radius;
	public Double maxHeight;

	/**
	 * List of all shown aggregations
	 */
	private Map<String, List<Aggregations.FromToObject>> aggregations = new HashMap<>();

	public Hexagons() {
		super("hexagons");
	}

	// TODO DOCs
	public Hexagons addAggregation(String aggregationTitle, String fromTitle, String fromX, String fromY, String toTitle, String toX, String toY) {

		this.aggregations.put(aggregationTitle, List.of(new Aggregations.FromToObject(fromTitle, fromX, fromY), new Aggregations.FromToObject(toTitle, toX, toY)));

		return this;
	}

	// TODO docs
	public Hexagons addAggregation(String aggregationTitle, String fromTitle, String fromX, String fromY) {
		this.aggregations.put(aggregationTitle, List.of(new Aggregations.FromToObject(fromTitle, fromX, fromY)));

		return this;
	}


	/**
	 * Defines an aggregation element.
	 */
	private static final class Aggregations {

		private FromToObject fromAggregation;

		private FromToObject toAggregation;

		private Aggregations(String fromTitle, String fromX, String fromY, String toTitle, String toX, String toY) {
			this.fromAggregation = new FromToObject(fromTitle, fromX, fromY);
			this.toAggregation = new FromToObject(toTitle, toX, toY);
		}

		private static final class FromToObject {

			@JsonProperty(required = true)
			private String title;

			@JsonProperty(required = true)
			private String x;

			@JsonProperty(required = true)
			private String y;

			private FromToObject(String title, String x, String y) {
				this.title = title;
				this.x = x;
				this.y = y;
			}
		}
	}
}
