package playground.pieter.singapore.utils.postgresql.travelcomponents;

import org.matsim.api.core.v01.Coord;

public 	 class Wait extends TravelComponent {
	public Journey journey;
	private Coord coord;
	private boolean accessWait = false;

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
}