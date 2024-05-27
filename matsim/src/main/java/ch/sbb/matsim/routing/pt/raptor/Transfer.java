/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
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
