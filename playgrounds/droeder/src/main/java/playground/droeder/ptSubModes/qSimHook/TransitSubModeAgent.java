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
package playground.droeder.ptSubModes.qSimHook;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 * except of getEnterTransitRoute(...) c&p from {@link TransitAgent} and {@link PTransitAgent} 
 *
 */
class TransitSubModeAgent extends PersonDriverAgentImpl implements MobsimDriverPassengerAgent{
	private static final Logger log = Logger.getLogger(TransitSubModeAgent.class);

	private boolean fixedSubMode;

	private TransitSchedule transitSchedule;
	
	protected static TransitSubModeAgent createAgent(Person p, Netsim sim, boolean fixedMode){
		return new TransitSubModeAgent(p, sim, fixedMode);
	}


	private TransitSubModeAgent(Person p, Netsim sim, boolean fixedMode) {
		super(p.getSelectedPlan(), sim);
		this.fixedSubMode = fixedMode;
		this.transitSchedule = sim.getScenario().getTransitSchedule();
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		return route.getEgressStopId().equals(stop.getId());
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		Leg leg = getCurrentLeg();
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		
		if(containsId(stopsToCome, route.getEgressStopId()) && enterThisMode(leg, transitRoute) && lessThanEqualTime(line, leg, transitRoute)){
			return true;
		}
		return false;
	}
	
	/**
	 * adapted from {@linkPTransitAgent}
	 * 
	 * @param line 
	 * @param leg
	 * @param transitRoute
	 * @return
	 */
	private boolean lessThanEqualTime(TransitLine line, Leg leg, TransitRoute transitRoute) {
		
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) getCurrentLeg().getRoute();
		
		TransitLine transitLinePlanned = this.transitSchedule.getTransitLines().get(route.getLineId());
		if(transitLinePlanned == null){
			// line doesn't exist anymore
			return true;
		}
		
		TransitRoute transitRoutePlanned = transitLinePlanned.getRoutes().get(route.getRouteId());
		if (transitRoutePlanned == null) {
			// This route doesn't exist anymore. In terms of time enter, other conditions checked somewhere else
			return true;
		}
		
		TransitRoute transitRouteOffered = this.transitSchedule.getTransitLines().get(line.getId()).getRoutes().get(transitRoute.getId());

		double travelTimePlanned = getArrivalOffsetFromRoute(transitRoutePlanned, route.getEgressStopId()) - getDepartureOffsetFromRoute(transitRoutePlanned, route.getAccessStopId());
		double travelTimeOffered = getArrivalOffsetFromRoute(transitRouteOffered, route.getEgressStopId()) - getDepartureOffsetFromRoute(transitRouteOffered, route.getAccessStopId());
		
		if (travelTimeOffered <= travelTimePlanned) {
			// this is a faster or at least an equal solution, enter
			return true;
		}
		return false;
	}
	
	
	// ################ c&p from PTransitAgent ########################################
	private double getArrivalOffsetFromRoute(TransitRoute transitRoute, Id egressStopId) {
		for (TransitRouteStop routeStop : transitRoute.getStops()) {
			if (egressStopId.equals(routeStop.getStopFacility().getId())) {
				return routeStop.getArrivalOffset();
			}
		}

		log.error("Stop " + egressStopId + " not found in route " + transitRoute.getId());
		// returning what???
		return -1.0;
	}
	
	private double getDepartureOffsetFromRoute(TransitRoute transitRoute, Id accessStopId) {
		for (TransitRouteStop routeStop : transitRoute.getStops()) {
			if (accessStopId.equals(routeStop.getStopFacility().getId())) {
				return routeStop.getDepartureOffset();
			}
		}

		log.error("Stop " + accessStopId + " not found in route " + transitRoute.getId());
		// returning what???
		return -1.0;
	}
	// ################# end c&p #######################################################

	private boolean containsId(List<TransitRouteStop> stopsToCome,
			Id egressStopId) {
		for (TransitRouteStop stop : stopsToCome) {
			if (egressStopId.equals(stop.getStopFacility().getId())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param leg
	 * @param transitRoute
	 * @return
	 */
	private boolean enterThisMode(Leg leg, TransitRoute transitRoute) {
		if(transitRoute.getTransportMode().equals(leg.getMode())){
			// agent should definitely enter
			return true;
		}else{
			if(!this.fixedSubMode){
				// subMode is not fixed, so enter anyway
				return true;
			}
		}
		return false;
	}

	private Leg getCurrentLeg() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		return (Leg) currentPlanElement;
	}


	@Override
	public double getWeight() {
		return 1.0;
	}

	@Override
	public Id getDesiredAccessStopId() {
		Leg leg = getCurrentLeg();
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ leg.getRoute().getRouteDescription());
			return null;
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			Id accessStopId = route.getAccessStopId();
			return accessStopId;
		}
	}

	@Override
	public Id getDesiredDestinationStopId() {
		Leg leg = getCurrentLeg();
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
			log.info("route: "
					+ leg.getRoute().getClass().getCanonicalName()
					+ " "
					+ leg.getRoute().getRouteDescription());
			return null;
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			return route.getEgressStopId();
		}
	}
}

