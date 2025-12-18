package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PendingPtLeg {

	private Id<TransitStopFacility> startFacility;
	private Id<TransitStopFacility> endFacility;
	private final Id<TransitLine> line;
	private final Id<TransitRoute> route;

	public PendingPtLeg(Id<TransitRoute> route, Id<TransitLine> line) {
		this.route = route;
		this.line = line;
	}

	void startFacility(Id<TransitStopFacility> startFacility) {
		this.startFacility = startFacility;
	}

	boolean hasStartFacility() {
		return startFacility != null;
	}

	void endFacility(Id<TransitStopFacility> endFacility) {
		this.endFacility = endFacility;
	}

	Id<TransitStopFacility> startFacility() {
		return startFacility;
	}

	Id<TransitStopFacility> endFacility() {
		return endFacility;
	}

	Id<TransitLine> line() {
		return line;
	}

	Id<TransitRoute> route() {
		return route;
	}
}
