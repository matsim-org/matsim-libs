package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RRouteStop;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData.RTransfer;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / Simunto
 */
public class Transfer {
	RTransfer rTransfer = null;
	RRouteStop fromStop = null;
	RRouteStop toStop = null;

	void reset(RTransfer transfer, RRouteStop rFromStop, RRouteStop rToStop) {
		this.rTransfer = transfer;
		this.fromStop = rFromStop;
		this.toStop = rToStop;
	}

	public TransitStopFacility getFromStop() {
		return this.fromStop.routeStop.getStopFacility();
	}

	public TransitStopFacility getToStop() {
		return this.toStop.routeStop.getStopFacility();
	}

	public double getTransferTime() {
		return this.rTransfer.transferTime;
	}

	public double getTransferDistance() {
		return this.rTransfer.transferDistance;
	}

	public TransitLine getFromTransitLine() {
		return this.fromStop.line;
	}

	public TransitRoute getFromTransitRoute() {
		return this.fromStop.route;
	}

	public TransitLine getToTransitLine() {
		return this.toStop.line;
	}

	public TransitRoute getToTransitRoute() {
		return this.toStop.route;
	}
}
