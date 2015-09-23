/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public abstract class PassivePlannerTransitAgent extends PassivePlannerDriverAgent implements MobsimDriverPassengerAgent  {

	//Constants
	private final static Logger log = Logger.getLogger(PassivePlannerTransitAgent.class);
	
	//Constructors
	public PassivePlannerTransitAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		super(basePerson, simulation, passivePlannerManager);
	}

	//Methods
	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
			TransitVehicle transitVehicle) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) ((Leg)getCurrentPlanElement()).getRoute();
		if (line.getId().equals(route.getLineId()))
			return containsId(stopsToCome, route.getEgressStopId());
		else
			return false;
	}
	private boolean containsId(List<TransitRouteStop> stopsToCome,
			Id<TransitStopFacility> egressStopId) {
		for (TransitRouteStop stop : stopsToCome)
			if (egressStopId.equals(stop.getStopFacility().getId()))
				return true;
		return false;
	}
	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) ((Leg)getCurrentPlanElement()).getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}
	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		Leg leg = (Leg)getCurrentPlanElement();
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ leg.getRoute().getRouteDescription());
			return null;
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			Id<TransitStopFacility> accessStopId = route.getAccessStopId();
			return accessStopId;
		}
	}
	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) ((Leg)getCurrentPlanElement()).getRoute();
		return route.getEgressStopId();
	}
	
}
