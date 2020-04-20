/* *********************************************************************** *
 * project: org.matsim.*
 * MockAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.fakes;


import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;


/**
 * @author mrieser
 */
public class FakeAgent implements MobsimDriverAgent, PTPassengerAgent {

	private final TransitStopFacility exitStop;
	private final Leg dummyLeg;
	private final Person dummyPerson = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
	// as long as all instance variables are final, the "resetCaches" method can remain empty.  kai, oct'10

	/**
	 * Creates a new fake Agent. If enterStop or exitStop are <code>null</code>,
	 * the leg will have no route.
	 *
	 * @param enterStop may be <code>null</code>
	 * @param exitStop may be <code>null</code>
	 */
	public FakeAgent(final TransitStopFacility enterStop, final TransitStopFacility exitStop) {
		this.exitStop = exitStop;
		this.dummyLeg = PopulationUtils.createLeg(TransportMode.pt);
		if ((enterStop != null) && (exitStop != null)) {
			Route route = new DefaultTransitPassengerRoute(enterStop, null, null, exitStop);
			route.setStartLinkId(enterStop.getLinkId());
			route.setEndLinkId(exitStop.getLinkId());
			route.setRouteDescription("{" + 
					"\"accessFacilityId\":\"" + enterStop.getId().toString() + "\"," +
					"\"egressFacilityId\":\"" + exitStop.getId().toString() + "\"," + 
					"\"transitLineId\":\"T1\"" +
			"}");
			this.dummyLeg.setRoute(route);
		}
	}

	@Override
	public void endActivityAndComputeNextState(final double now) {
	}
	
	@Override
	public void setStateToAbort(final double now){
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return null;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return null;
	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		// since the class does not tell what it is supposed to do I do not know what is a reasonable answer here.  kai, jun'11
		throw new UnsupportedOperationException("Not implemented");
	}

    @Override
    public Double getExpectedTravelDistance() {
        return null;
    }

    @Override
	public String getMode() {
		return this.dummyLeg.getMode();
	}
	
	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return ((NetworkRoute)this.dummyLeg.getRoute()).getVehicleId(); // not sure if this is very clever.  kai, jun'11
	}
	
	@Override
	public double getActivityEndTime() {
		return 0;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return null;
	}

	@Override
	public void endLegAndComputeNextState(final double now) {
	}

	@Override
	public void notifyMoveOverNode(Id<Link> nextLinkId) {
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		return stop == this.exitStop;
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute, List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		return true;
	}

	@Override
	public void setVehicle(final MobsimVehicle veh) {
	}

	@Override
	public QVehicle getVehicle() {
		return null;
	}

	@Override
	public Id<Person> getId() {
		return this.dummyPerson.getId();
	}

	@Override
	public double getWeight() {
		return 1.0;
	}

	@Override
	public State getState() {
		return MobsimAgent.State.ABORT;
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		return null;
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		return null;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility getCurrentFacility() {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility getDestinationFacility() {
		throw new RuntimeException("not implemented") ;
	}
	
}
