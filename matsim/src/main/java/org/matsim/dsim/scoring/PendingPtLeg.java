package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;

class PendingPtLeg {

	private final List<PtPart> chainedLegParts = new ArrayList<>();

	PendingPtLeg() {
	}

	void startPart(Id<TransitLine> line, Id<TransitRoute> route) {
		chainedLegParts.add(new PtPart(line, route));
	}

	void startFacility(Id<TransitStopFacility> startFacility) {
		this.chainedLegParts.getLast().setStartFacility(startFacility);
	}

	boolean hasStartFacility() {
		return this.chainedLegParts.getLast().getStartFacility() != null;
	}

	void endFacility(Id<TransitStopFacility> endFacility) {
		this.chainedLegParts.getLast().setEndFacility(endFacility);
	}

	List<PtPart> getPtParts() {
		return chainedLegParts;
	}

	static class PtPart {

		private Id<TransitStopFacility> startFacility;
		private Id<TransitStopFacility> endFacility;
		private final Id<TransitLine> line;
		private final Id<TransitRoute> route;

		PtPart(Id<TransitLine> line, Id<TransitRoute> route) {
			this.line = line;
			this.route = route;
		}

		public void setStartFacility(Id<TransitStopFacility> startFacility) {
			this.startFacility = startFacility;
		}

		public void setEndFacility(Id<TransitStopFacility> endFacility) {
			this.endFacility = endFacility;
		}

		public Id<TransitStopFacility> getStartFacility() {
			return startFacility;
		}

		public Id<TransitStopFacility> getEndFacility() {
			return endFacility;
		}

		public Id<TransitLine> getLine() {
			return line;
		}

		public Id<TransitRoute> getRoute() {
			return route;
		}
	}
}
