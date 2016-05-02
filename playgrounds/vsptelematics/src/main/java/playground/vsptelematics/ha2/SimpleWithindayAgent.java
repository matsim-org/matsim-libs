/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleWithindayAgent
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
package playground.vsptelematics.ha2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 *
 */
public class SimpleWithindayAgent implements MobsimDriverAgent {

	
	private PersonDriverAgentImpl delegate;

	protected SimpleWithindayAgent(Person p, Netsim simulation) {
//		super(p.getSelectedPlan(), simulation);
		// (not sure if this will still work; this used to extend from EperimentalWithindayAgent. kai, feb'14)

		this.delegate = new PersonDriverAgentImpl(p.getSelectedPlan(), simulation) ;
		throw new RuntimeException("I replaced inheritance by delegation; pls chk carefully if this is still working "
				+ "as expected. kai, nov'14") ;
	}

	@Override
	public Id<Link> chooseNextLinkId(){
		Id<Link> currentLinkId  = this.getCurrentLinkId();
		Id<Link> nextLink = null;
		if (currentLinkId.equals(Id.create("1", Link.class))){
			nextLink = Id.create("2", Link.class);
		}
		else if (currentLinkId.equals(Id.create("2", Link.class))){
			nextLink = Id.create("4", Link.class);
		}
		else if (currentLinkId.equals(Id.create("4", Link.class))){
			nextLink = Id.create("6", Link.class);
		}
		return nextLink;
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public final void endActivityAndComputeNextState(double now) {
		delegate.endActivityAndComputeNextState(now);
	}

	public final void endLegAndComputeNextState(double now) {
		delegate.endLegAndComputeNextState(now);
	}

	public final void setStateToAbort(double now) {
		delegate.setStateToAbort(now);
	}

	public final void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		delegate.notifyMoveOverNode(newLinkId);
	}

	public String toString() {
		return delegate.toString();
	}

	public final boolean isWantingToArriveOnCurrentLink() {
		return delegate.isWantingToArriveOnCurrentLink();
	}

	public final double getActivityEndTime() {
		return delegate.getActivityEndTime();
	}

	public final Double getExpectedTravelTime() {
		return delegate.getExpectedTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {
        return delegate.getExpectedTravelDistance();
    }

    public final String getMode() {
		return delegate.getMode();
	}

	public final void setVehicle(MobsimVehicle veh) {
		delegate.setVehicle(veh);
	}

	public final MobsimVehicle getVehicle() {
		return delegate.getVehicle();
	}

	public final Id<Link> getCurrentLinkId() {
		return delegate.getCurrentLinkId();
	}

	public final Id<Vehicle> getPlannedVehicleId() {
		return delegate.getPlannedVehicleId();
	}

	public final Id<Link> getDestinationLinkId() {
		return delegate.getDestinationLinkId();
	}

	public final Person getPerson() {
		return delegate.getPerson();
	}

	public final Id<Person> getId() {
		return delegate.getId();
	}

	public final State getState() {
		return delegate.getState();
	}

	public final PlanElement getCurrentPlanElement() {
		return delegate.getCurrentPlanElement();
	}

	public final PlanElement getNextPlanElement() {
		return delegate.getNextPlanElement();
	}

	public final Plan getCurrentPlan() {
		return delegate.getCurrentPlan();
	}

	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.delegate.getCurrentFacility();
	}

	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.delegate.getDestinationFacility();
	}

	public final PlanElement getPreviousPlanElement() {
		return this.delegate.getPreviousPlanElement();
	}

}
