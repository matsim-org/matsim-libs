/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.mobsim.qsim.agents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * The functionality of this class is no longer necessary, due to changes assumptions
 * for this project.
 * 
 * @author droeder
 *
 */
@Deprecated
class WagonSimAgent implements MobsimDriverAgent, MobsimPassengerAgent, PTPassengerAgent, HasPerson, PlanAgent{

	private Netsim sim;
	private TransitAgent delegate;

	static WagonSimAgent createInstance(Person p, Netsim sim){
		return new WagonSimAgent(p, p.getSelectedPlan(), sim);
	}
	
	/**
	 * Uses {@link TransitAgent} as delegate for all methods except getEnterTransitRoute(...).
	 * This agents enters a vehicle when a) it is the correct TransitRoute (train) and b)
	 * ((timeToEnter + now) < plannedDeparture). However, when this is true for a lot of agents
	 * ALL will enter, even when the sum of access-times exceeds the allowed/planned departure-
	 * time of the vehicle.
	 *
	 * @param person
	 * @param plan
	 * @param simulation
	 */
	private WagonSimAgent(final Person person, final Plan plan, final Netsim simulation) {
		/*
		 * When two agents are at the same stop and both want to enter and the remaining time is 
		 * long enough for only one to enter, still both will enter... That is we are not
		 * completely at the right side here, we have to change the {@link TransitQSimEngine} and the
		 * {@link TransitStopHandler}  as well
		 */

		// extending TransitAgent is not possible as the constructor is package-protected => delegate
		this.delegate = TransitAgent.createTransitAgent(person, simulation);
		this.sim = simulation;
	}

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(WagonSimAgent.class);

	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
			TransitVehicle transitVehicle) {
		if(this.delegate.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle)){
			// enter only when it is the correct route. Now check if the current time allows to enter...
			double now = sim.getSimTimer().getTimeOfDay();
			// the time an agents needs to enter (don't know why this is deprecated, but I can't see any other way to to get the accesstime)
			double access = transitVehicle.getVehicle().getType().getAccessTime();
			Leg l = (Leg) delegate.getCurrentPlanElement();
			// as far as I understood every TransitLine will consist of one TransitRoute with exactly one departure.
			// That is, we can just use the first element from the map.
			double plannedDeparture = transitRoute.getDepartures().values().iterator().next().getDepartureTime();
			Id accesStopId = ((ExperimentalTransitRoute) l.getRoute()).getAccessStopId();
			for(TransitRouteStop s: transitRoute.getStops()){
				if(s.getStopFacility().getId().equals(accesStopId)){
					plannedDeparture += s.getDepartureOffset();
					// assume there are no ''loop-routes''
					break;
				}
			}
			if((now + access) < plannedDeparture){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		return this.delegate.getExitAtStop(stop);
	}

	@Override
	public Id getDesiredAccessStopId() {
		return this.delegate.getDesiredAccessStopId();
	}

	@Override
	public Id getDesiredDestinationStopId() {
		return this.delegate.getDesiredDestinationStopId();
	}

	@Override
	public double getWeight() {
		return this.delegate.getWeight();
	}

	@Override
	public State getState() {
		return this.delegate.getState();
	}

	@Override
	public double getActivityEndTime() {
		return this.delegate.getActivityEndTime();
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		this.delegate.endActivityAndComputeNextState(now);
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		this.delegate.endLegAndComputeNextState(now);
	}

	@Override
	public void setStateToAbort(double now) {
		this.delegate.setStateToAbort(now);
	}

	@Override
	public Double getExpectedTravelTime() {
		return this.delegate.getExpectedTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {
        return delegate.getExpectedTravelDistance();
    }

    @Override
	public String getMode() {
		return this.delegate.getMode();
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		this.delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public Id getCurrentLinkId() {
		return this.delegate.getCurrentLinkId();
	}

	@Override
	public Id getDestinationLinkId() {
		return this.delegate.getDestinationLinkId();
	}

	@Override
	public Id getId() {
		return this.delegate.getId();
	}

	@Override
	public Id chooseNextLinkId() {
		return this.delegate.chooseNextLinkId();
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		this.delegate.notifyMoveOverNode(newLinkId);
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.delegate.setVehicle(veh);
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.delegate.getVehicle();
	}

	@Override
	public Id getPlannedVehicleId() {
		return this.delegate.getPlannedVehicleId();
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return this.delegate.getCurrentPlanElement();
	}

	@Override
	public PlanElement getNextPlanElement() {
		return this.delegate.getNextPlanElement();
	}

	@Override
	public Plan getCurrentPlan() {
		return this.delegate.getCurrentPlan();
	}

	@Override
	public Person getPerson() {
		return this.delegate.getPerson();
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return this.delegate.isWantingToArriveOnCurrentLink() ;
	}

	public final PlanElement getPreviousPlanElement() {
		return this.delegate.getPreviousPlanElement();
	}

	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.delegate.getCurrentFacility();
	}

	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.delegate.getDestinationFacility();
	}

}

