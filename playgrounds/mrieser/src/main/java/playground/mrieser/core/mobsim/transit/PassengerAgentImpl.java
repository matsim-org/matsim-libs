/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.transit;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/*package*/ class PassengerAgentImpl implements PTPassengerAgent {

	private final Id id;
	private final ExperimentalTransitRoute route;
	private final double weight;

	public PassengerAgentImpl(final Id id, final ExperimentalTransitRoute route, final double weight) {
		this.id = id;
		this.route = route;
		this.weight = weight;
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute,
			final List<TransitRouteStop> stopsToCome) {
		if (line.getId().equals(route.getLineId())) {
			return containsId(stopsToCome, route.getEgressStopId());
		}
		return false;
	}

	private boolean containsId(List<TransitRouteStop> stopsToCome, Id egressStopId) {
		for (TransitRouteStop stop : stopsToCome) {
			if (egressStopId.equals(stop.getStopFacility().getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		return route.getEgressStopId().equals(stop.getId());
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public double getWeight() {
		return this.weight;
	}

	@Override
	public Id getDesiredAccessStopId() {
		// Probably not used in your simulation because you ask this before you pass the route in. //mz
		return route.getAccessStopId();
	}
	
	@Override
	public Id getDesiredDestinationStopId() {
		return route.getEgressStopId();
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
	}

	@Override
	public MobsimVehicle getVehicle() {
		return null;
	}

	@Override
	public Id getPlannedVehicleId() {
		return null;
	}

	@Override
	public Id getCurrentLinkId() {
		return null;
	}

	@Override
	public Id getDestinationLinkId() {
		return null;
	}

}
