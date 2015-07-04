package org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public 	 class Wait extends TravelComponent {
	public Journey journey;
	private Coord coord;
	private boolean accessWait = false;
	private Id stopId;

	public String toString() {
		return String.format(
				"\tWAIT: start: %6.0f end: %6.0f dur: %6.0f \n", getStartTime(),
				getEndTime(), getEndTime() - getStartTime());
	}

	public boolean isAccessWait() {
		return accessWait;
	}

	public void setAccessWait(boolean accessWait) {
		this.accessWait = accessWait;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public Id getStopId() {
		return stopId;
	}

	public void setStopId(Id stopId) {
		this.stopId = stopId;
	}
}