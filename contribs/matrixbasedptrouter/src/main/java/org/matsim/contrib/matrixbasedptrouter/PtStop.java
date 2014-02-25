package org.matsim.contrib.matrixbasedptrouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

class PtStop {
	
	private final Id id;
	private final Coord coord;

	public PtStop(Id id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	public Id getId() {
		return id;
	}
	public Coord getCoord() {
		return coord;
	}
}