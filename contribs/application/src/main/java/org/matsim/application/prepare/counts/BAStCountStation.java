package org.matsim.application.prepare.counts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds data for one bast counting station.
 */
class BAStCountStation {

	private final String name;
	private final String id;
	private final String dir1; //from column 'Hi_Ri1'
	private final String dir2; //from column 'Hi_Ri2'

	private Link matchedLink;
	private Link oppLink;
	private String matchedDir;
	private String oppDir;

	private boolean hasMatchedLink = true;
	private boolean hasOppLink = true;

	private final Coord coord;

	private final Map<String, Double> mivTrafficVolume1 = new HashMap<>();
	private final Map<String, Double> mivTrafficVolume2 = new HashMap<>();

	private final Map<String, Double> freightTrafficVolume1 = new HashMap<>();
	private final Map<String, Double> freightTrafficVolume2 = new HashMap<>();

	BAStCountStation(String id, String name, String dir1, String dir2, Coord coord) {

		this.coord = coord;
		this.dir1 = dir1; // is one of: "N", "O", "S", "W"
		this.dir2 = dir2;
		this.id = id;
		this.name = name;

		this.matchedLink = null;
		this.matchedDir = null;
	}

	public String getId() {
		return id;
	}

	public String getDir1() {
		return dir1;
	}

	public String getDir2() {
		return dir2;
	}

	public Map<String, Double> getMivTrafficVolume1() {
		return mivTrafficVolume1;
	}

	public Map<String, Double> getMivTrafficVolume2() {
		return mivTrafficVolume2;
	}

	public Map<String, Double> getFreightTrafficVolume1() {
		return freightTrafficVolume1;
	}

	public Map<String, Double> getFreightTrafficVolume2() {
		return freightTrafficVolume2;
	}

	public Coord getCoord() {
		return coord;
	}

	public String getName() {
		return name;
	}

	public Link getMatchedLink() {
		return matchedLink;
	}

	public String getMatchedDir() {
		return matchedDir;
	}

	public Link getOppLink() {
		return oppLink;
	}

	public String getOppDir() {
		return oppDir;
	}

	public boolean hasMatchedLink(){
		return hasMatchedLink;
	}
	public boolean hasOppLink() {
		return hasOppLink;
	}

	public void setMatchedLink(Link matchedLink) {
		this.matchedLink = matchedLink;

		matchDirection(matchedLink, this.dir1);
	}

	public void setOppLink(Link oppLink) {
		this.oppLink = oppLink;
	}

	public void setHasNoMatchedLink(){
		this.hasMatchedLink = false;
	}

	public void setHasNoOppLink() {
		this.hasOppLink = false;
	}

	private void matchDirection(Link link, String bastDirection) {
		String direction = getLinkDirection(link);

		this.matchedDir = direction.contains(bastDirection) ? "R1": "R2";
		this.oppDir = matchedDir.equals("R1") ? "R2": "R1";
	}

	public void overwriteDirections(String newMatchedDir, String newOppDir){

		if(Objects.equals(newMatchedDir, newOppDir))
			throw new RuntimeException("Can't match the same direction for both links!");

		if(newMatchedDir != null)
			this.matchedDir = newMatchedDir.contains(this.dir1) ? "R1": "R2";

		if(newOppDir != null)
			this.oppDir = newOppDir.contains(this.dir1) ? "R1": "R2";
	}

	public String getLinkDirection(Link link) {

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
