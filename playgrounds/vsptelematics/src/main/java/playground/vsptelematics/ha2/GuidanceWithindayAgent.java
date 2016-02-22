/* *********************************************************************** *
 * project: org.matsim.*
 * GuidanceWithindayAgent
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
//public class GuidanceWithindayAgent extends PersonDriverAgentImpl {
public class GuidanceWithindayAgent implements MobsimDriverAgent {

	private Id<Link> id1 = Id.create("1", Link.class);
	private Id<Link> id2 = Id.create("2", Link.class);
	private Id<Link> id3 = Id.create("3", Link.class);
	private Id<Link> id4 = Id.create("4", Link.class);
	private Id<Link> id5 = Id.create("5", Link.class);
	private Id<Link> id6 = Id.create("6", Link.class);
	private Guidance guidance;
	private Netsim simulation;
	
	private MobsimDriverAgent delegate ;
	
	protected GuidanceWithindayAgent(Person p, Netsim simulation, Guidance guidance) {
//		super(p.getSelectedPlan(), simulation);
		// (not sure if this will work; this class used to extend from ExperimentalWithindayAgent. kai, feb'14)
		

		this.simulation = simulation;
		this.guidance = guidance;

		this.delegate = new PersonDriverAgentImpl(p.getSelectedPlan(), simulation) ;
		throw new RuntimeException("I replaced inheritance by delegation; pls chk carefully if this is still working "
				+ "as expected. kai, nov'14") ;
	}

	@Override
	public Id<Link> chooseNextLinkId(){
		double time = this.simulation.getSimTimer().getTimeOfDay();
		Id<Link> currentLinkId  = this.getCurrentLinkId();
		Id<Link> nextLink = null;
		if (currentLinkId.equals(id1)){
			nextLink = this.guidance.getNextLink(time);
		}
		else if (currentLinkId.equals(id2)){
			nextLink = id4;
		}
		else if (currentLinkId.equals(id3)){
			nextLink = id5;
		}
		else if (currentLinkId.equals(id4) || currentLinkId.equals(id5)){
			nextLink = id6;
		}
		return nextLink;
	}

	public Id<Person> getId() {
		return delegate.getId();
	}

	public void setVehicle(MobsimVehicle veh) {
		delegate.setVehicle(veh);
	}

	public Id<Link> getCurrentLinkId() {
		return delegate.getCurrentLinkId();
	}

	public MobsimVehicle getVehicle() {
		return delegate.getVehicle();
	}

	public void notifyMoveOverNode(Id<Link> newLinkId) {
		delegate.notifyMoveOverNode(newLinkId);
	}

	public Id<Link> getDestinationLinkId() {
		return delegate.getDestinationLinkId();
	}

	public Id<Vehicle> getPlannedVehicleId() {
		return delegate.getPlannedVehicleId();
	}

	public State getState() {
		return delegate.getState();
	}

	public boolean isWantingToArriveOnCurrentLink() {
		return delegate.isWantingToArriveOnCurrentLink();
	}

	public double getActivityEndTime() {
		return delegate.getActivityEndTime();
	}

	public void endActivityAndComputeNextState(double now) {
		delegate.endActivityAndComputeNextState(now);
	}

	public void endLegAndComputeNextState(double now) {
		delegate.endLegAndComputeNextState(now);
	}

	public void setStateToAbort(double now) {
		delegate.setStateToAbort(now);
	}

	public Double getExpectedTravelTime() {
		return delegate.getExpectedTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {
        return delegate.getExpectedTravelDistance();
    }

    public String getMode() {
		return delegate.getMode();
	}

	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.delegate.getCurrentFacility();
	}

	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.delegate.getDestinationFacility();
	}
	
}
