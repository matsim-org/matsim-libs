package org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public 	 class Trip extends TravelComponent {
	Journey journey;
	private String mode;
	private Id line;
	private Id route;
	private Coord orig;
	private Coord dest;
	private Id boardingStop;
	private Id alightingStop;
	private double distance;

	public String toString() {
		return String
				.format("\tTRIP: mode: %s start: %6.0f end: %6.0f distance: %6.0f \n",
						getMode(), getStartTime(), getEndTime(), getDistance());
	}

	public Id getLine() {
		return line;
	}

	public void setLine(Id line) {
		this.line = line;
	}

	public Id getRoute() {
		return route;
	}

	public void setRoute(Id route) {
		this.route = route;
	}

	public Id getBoardingStop() {
		return boardingStop;
	}

	public void setBoardingStop(Id boardingStop) {
		this.boardingStop = boardingStop;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode.trim();
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public Id getAlightingStop() {
		return alightingStop;
	}

	public void setAlightingStop(Id alightingStop) {
		this.alightingStop = alightingStop;
	}

	public Coord getDest() {
		return dest;
	}

	public void setDest(Coord dest) {
		this.dest = dest;
	}

	public Coord getOrig() {
		return orig;
	}

	public void setOrig(Coord orig) {
		this.orig = orig;
	}

	public void incrementDistance(double linkLength) {
		this.distance += linkLength;
		
	}

	public void incrementTime(double linkTime) {
		this.setEndTime(this.getEndTime()+linkTime);
		
	}
}
