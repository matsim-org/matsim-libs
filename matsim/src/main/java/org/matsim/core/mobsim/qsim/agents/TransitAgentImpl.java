/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.agents;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public final class TransitAgentImpl implements PTPassengerAgent {

	private BasicPlanAgentImpl basicAgentDelegate;

	public TransitAgentImpl( BasicPlanAgentImpl basicAgent ) {
		this.basicAgentDelegate = basicAgent ;
	}

	private static final Logger log = Logger.getLogger(TransitAgentImpl.class);

	@Override
	public final  boolean getExitAtStop(final TransitStopFacility stop) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) basicAgentDelegate.getCurrentLeg().getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}

	@Override
	public final boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) basicAgentDelegate.getCurrentLeg().getRoute();
		if (line.getId().equals(route.getLineId())) {
			return containsId(stopsToCome, route.getEgressStopId());
		} else {
			return false;
		}
	}

	@SuppressWarnings("static-method")
	private final boolean containsId(List<TransitRouteStop> stopsToCome,
			Id<TransitStopFacility> egressStopId) {
		for (TransitRouteStop stop : stopsToCome) {
			if (egressStopId.equals(stop.getStopFacility().getId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final double getWeight() {
		return 1.0;
	}

	@Override
	public final Id<TransitStopFacility> getDesiredAccessStopId() {
		Leg leg = basicAgentDelegate.getCurrentLeg();
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ (leg.getRoute() instanceof GenericRoute ? ((GenericRoute) leg.getRoute()).getRouteDescription() : ""));
			return null;
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			Id<TransitStopFacility> accessStopId = route.getAccessStopId();
			return accessStopId;
		}
	}

	@Override
	public final Id<TransitStopFacility> getDesiredDestinationStopId() {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) basicAgentDelegate.getCurrentLeg().getRoute();
		return route.getEgressStopId();
	}
	@Override
	public Id<Link> getCurrentLinkId() {
		return basicAgentDelegate.getCurrentLinkId() ;
	}
	@Override
	public Id<Link> getDestinationLinkId() {
		return basicAgentDelegate.getDestinationLinkId() ;
	}
	@Override
	public void setVehicle(MobsimVehicle veh) {
		basicAgentDelegate.setVehicle( veh );
	}
	@Override
	public MobsimVehicle getVehicle() {
		return basicAgentDelegate.getVehicle() ;
	}
	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return basicAgentDelegate.getPlannedVehicleId() ;
	}
	@Override
	public Id<Person> getId() {
		return basicAgentDelegate.getId() ;
	}

	@Override
	public String getMode() {
		return basicAgentDelegate.getMode() ;
	}

}
