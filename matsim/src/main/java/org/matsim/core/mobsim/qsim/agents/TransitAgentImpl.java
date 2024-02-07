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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.config.TransitConfigGroup.BoardingAcceptance;
import org.matsim.pt.routes.TransitPassengerRoute;
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
	private final BoardingAcceptance boardingAcceptance ;

	public TransitAgentImpl( BasicPlanAgentImpl basicAgent ) {
		this( basicAgent, BoardingAcceptance.checkLineAndStop ) ;
	}

	public TransitAgentImpl(BasicPlanAgentImpl basicAgent, BoardingAcceptance boardingAcceptance) {
		this.basicAgentDelegate = basicAgent ;
		this.boardingAcceptance = boardingAcceptance;
	}

	private static final Logger log = LogManager.getLogger(TransitAgentImpl.class);

	@Override
	public final  boolean getExitAtStop(final TransitStopFacility stop) {
		TransitPassengerRoute route = (TransitPassengerRoute) basicAgentDelegate.getCurrentLeg().getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}

	@Override
	public final boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		TransitPassengerRoute route = (TransitPassengerRoute) basicAgentDelegate.getCurrentLeg().getRoute();
		switch ( boardingAcceptance ) {
			case checkLineAndStop:
				return line.getId().equals(route.getLineId()) && containsId(stopsToCome, route.getEgressStopId());
			case checkStopOnly:
				return containsId(stopsToCome, route.getEgressStopId());
			default:
				throw new RuntimeException("not implemented");
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
		if (!(leg.getRoute() instanceof TransitPassengerRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ leg.getRoute().getRouteDescription());
			return null;
		} else {
			TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
			Id<TransitStopFacility> accessStopId = route.getAccessStopId();
			return accessStopId;
		}
	}

	@Override
	public final Id<TransitStopFacility> getDesiredDestinationStopId() {
		TransitPassengerRoute route = (TransitPassengerRoute) basicAgentDelegate.getCurrentLeg().getRoute();
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
