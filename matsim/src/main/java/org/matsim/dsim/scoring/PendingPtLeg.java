package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PendingPtLeg {

	private final Id<TransitStopFacility> startFacility;

	private Id<TransitStopFacility> endFacility;

	public PendingPtLeg(Id<TransitStopFacility> startFacility) {
		this.startFacility = startFacility;
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
}
