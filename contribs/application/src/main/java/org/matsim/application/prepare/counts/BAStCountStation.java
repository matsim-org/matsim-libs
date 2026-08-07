package org.matsim.application.prepare.counts;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds data for one bast counting station.
 */
class BAStCountStation{

	static final int HOURS_PER_DAY = 24;

	private final String name;
	private final String id;
	private final String dir1;
	private final String directionField;

	private Link matchedLink;
	private boolean hasMatchedLink = true;

	private final Coord coord;

	/**
	 * Observed hourly volumes per aggregation name. One aggregation holds the single days its hours are formed of, so
	 * that mean as well as median can be taken over them.
	 */
	private final Map<String, HourlyValues> volumes = new HashMap<>();

	BAStCountStation(String id, String name, String directionField, String actualDirection, Coord coord){
		this.coord = coord;
		// actualDirection is one of: "N", "O", "S", "W"
		this.dir1 = actualDirection;
		this.directionField = directionField;
		this.id = id;
		this.name = name;

		this.matchedLink = null;
	}

	public String getId() {
		return id;
	}

	/**
	 * Observed volumes of one aggregation, created on first use.
	 */
	public HourlyValues getVolumes(String aggregation) {
		return volumes.computeIfAbsent(aggregation, k -> HourlyValues.create());
	}

	/**
	 * Observed volumes of one aggregation, or null if the aggregation covered no day of this station.
	 */
	public HourlyValues peekVolumes(String aggregation) {
		return volumes.get(aggregation);
	}

	public Coord getCoord() {
		return coord;
	}

	public String getName() {
		return name;
	}

	public String getDirectionField(){
		return directionField;
	}

	public Link getMatchedLink() {
		return matchedLink;
	}

	public boolean hasMatchedLink(){
		return hasMatchedLink && matchedLink != null;
	}

	public void setMatchedLink(Link matchedLink) {
		this.matchedLink = matchedLink;
	}

	public void setHasNoMatchedLink(){
		this.hasMatchedLink = false;
	}

	public String getDirection() {
		return dir1;
	}

	public static String getLinkDirection(Link link) {

		Coord fromCoord = link.getFromNode().getCoord();
		Coord toCoord = link.getToNode().getCoord();

		String direction = toCoord.getY() > fromCoord.getY() ? "N" : "S";

		if (toCoord.getX() > fromCoord.getX()) {

			direction += "O";
		} else {
			direction += "W";
		}

		return direction;
	}

	/**
	 * Car and freight volumes of one station in one aggregation, one list of observed days per hour of day.
	 */
	record HourlyValues(DoubleArrayList[] car, DoubleArrayList[] freight) {

		static HourlyValues create() {

			DoubleArrayList[] car = new DoubleArrayList[HOURS_PER_DAY];
			DoubleArrayList[] freight = new DoubleArrayList[HOURS_PER_DAY];

			for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
				car[hour] = new DoubleArrayList();
				freight[hour] = new DoubleArrayList();
			}

			return new HourlyValues(car, freight);
		}

		void add(int hour, double car, double freight) {
			this.car[hour].add(car);
			this.freight[hour].add(freight);
		}

		/**
		 * Number of days the thinnest hour of the day rests on. This is 0 as long as one hour is not covered at all,
		 * so it doubles as the completeness check.
		 */
		int fewestDays() {

			int fewest = Integer.MAX_VALUE;
			for (int hour = 0; hour < HOURS_PER_DAY; hour++)
				fewest = Math.min(fewest, car[hour].size());

			return fewest;
		}
	}
}
