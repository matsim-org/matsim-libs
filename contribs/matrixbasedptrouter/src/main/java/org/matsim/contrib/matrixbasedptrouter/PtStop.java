package org.matsim.contrib.matrixbasedptrouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

final class PtStop {
	
	private final Id<PtStop> id;
	private final Coord coord;

	public PtStop(Id<PtStop> id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}
	public Id<PtStop> getId() {
		return id;
	}
	public Coord getCoord() {
		return coord;
	}
}