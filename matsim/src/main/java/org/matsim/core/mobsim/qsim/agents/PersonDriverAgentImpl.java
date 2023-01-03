/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAgent.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * @author dgrether, nagel
 * <p></p>
 * I think this class is reasonable in terms of what is public and/or final and what not.
 */
public class PersonDriverAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, HasModifiablePlan {
	// yy cannot make this final since it is overridden at 65 locations
	// (but since all methods are final, it seems that all of these could be solved by delegation).
	// kai, nov'14

	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(PersonDriverAgentImpl.class);
	
	private BasicPlanAgentImpl basicAgentDelegate ;
	private PlanBasedDriverAgentImpl driverAgentDelegate ;

	public PersonDriverAgentImpl(final Plan plan1, final Netsim simulation, final TimeInterpretation timeInterpretation) {
		basicAgentDelegate = new BasicPlanAgentImpl(plan1, simulation.getScenario(), simulation.getEventsManager(), 
				simulation.getSimTimer(), timeInterpretation ) ;
		driverAgentDelegate = new PlanBasedDriverAgentImpl(basicAgentDelegate) ;
		
		// deliberately does NOT keep a back pointer to the whole Netsim; this should also be removed in the constructor call.
	}

	@Override
	public final void endLegAndComputeNextState(double now) {
		basicAgentDelegate.endLegAndComputeNextState(now);
	}

	@Override
	public final void setStateToAbort(double now) {
		basicAgentDelegate.setStateToAbort(now);
	}

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public final void endActivityAndComputeNextState(double now) {
		basicAgentDelegate.endActivityAndComputeNextState(now);
	}

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		return basicAgentDelegate.getPlannedVehicleId();
	}

	@Override
	public final String getMode() {
		return basicAgentDelegate.getMode();
	}

	@Override
	public final OptionalTime getExpectedTravelTime() {
		return basicAgentDelegate.getExpectedTravelTime();
	}

    @Override
    public final Double getExpectedTravelDistance() {
        return basicAgentDelegate.getExpectedTravelDistance();
    }

    @Override
	public String toString() {
		return basicAgentDelegate.toString();
	}

	@Override
	public final PlanElement getCurrentPlanElement() {
		return basicAgentDelegate.getCurrentPlanElement();
	}

	@Override
	public final PlanElement getNextPlanElement() {
		return basicAgentDelegate.getNextPlanElement();
	}

	@Override
	public final Plan getCurrentPlan() {
		return basicAgentDelegate.getCurrentPlan();
	}

	@Override
	public final Id<Person> getId() {
		return basicAgentDelegate.getId();
	}

	@Override
	public final Person getPerson() {
		return basicAgentDelegate.getPerson();
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return basicAgentDelegate.getVehicle();
	}

	@Override
	public final void setVehicle(MobsimVehicle vehicle) {
		basicAgentDelegate.setVehicle(vehicle);
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		return basicAgentDelegate.getCurrentLinkId();
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return basicAgentDelegate.getDestinationLinkId();
	}

	@Override
	public final double getActivityEndTime() {
		return basicAgentDelegate.getActivityEndTime();
	}

	@Override
	public final State getState() {
		return basicAgentDelegate.getState();
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		driverAgentDelegate.notifyMoveOverNode(newLinkId);
	}

	@Override
	public final Id<Link> chooseNextLinkId() {
		return driverAgentDelegate.chooseNextLinkId();
	}

	@Override
	public final boolean isWantingToArriveOnCurrentLink() {
		return driverAgentDelegate.isWantingToArriveOnCurrentLink();
	}
	
	final Leg getCurrentLeg() {
		return basicAgentDelegate.getCurrentLeg() ;
	}
	@Override
	public final int getCurrentLinkIndex() {
		return basicAgentDelegate.getCurrentLinkIndex() ;
	}
	final int getCurrentPlanElementIndex() {
		return basicAgentDelegate.getCurrentPlanElementIndex() ;
	}
	@Override
	public final Plan getModifiablePlan() {
		return basicAgentDelegate.getModifiablePlan() ;
	}
//	final void calculateAndSetDepartureTime( Activity act ) {
//		basicAgentDelegate.calculateAndSetDepartureTime(act);
//	}
	@Override public final void resetCaches() {
		basicAgentDelegate.resetCaches();
		driverAgentDelegate.resetCaches(); 
	}

	@Override
	public Facility getCurrentFacility() {
		return this.basicAgentDelegate.getCurrentFacility();
	}

	@Override
	public Facility getDestinationFacility() {
		return this.basicAgentDelegate.getDestinationFacility();
	}

	@Override
	public final PlanElement getPreviousPlanElement() {
		return this.basicAgentDelegate.getPreviousPlanElement();
	}

}
