package org.matsim.application.prepare.counts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds data for one bast counting station.
 */
class BAStCountStation{

	private final String name;
	private final String id;
	private final String dir1;
	private final String directionField;

	private Link matchedLink;
	private boolean hasMatchedLink = true;

	private final Coord coord;

	private final Map<String, Double> mivTrafficVolume1 = new HashMap<>();
	private final Map<String, Double> freightTrafficVolume1 = new HashMap<>();

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

	public Map<String, Double> getMivTrafficVolume() {
		return mivTrafficVolume1;
	}

	public Map<String, Double> getFreightTrafficVolume() {
		return freightTrafficVolume1;
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
		return hasMatchedLink;
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
}
