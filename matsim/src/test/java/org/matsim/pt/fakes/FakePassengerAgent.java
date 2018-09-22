/* *********************************************************************** *
 * project: org.matsim.*
 * MockPassengerAgent.java
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

package org.matsim.pt.fakes;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;


/**
 * A very simple implementation of the interface {@link PTPassengerAgent} for
 * use in tests. Enters every available line and exits at the specified stop.
 *
 * @author mrieser
 */
public class FakePassengerAgent implements PTPassengerAgent {

	private final TransitStopFacility exitStop;

	/**
	 * @param exitStop can be <code>null</code>
	 */
	public FakePassengerAgent(final TransitStopFacility exitStop) {
		this.exitStop = exitStop;
	}

	@Override
	public Id<Person> getId() {
		return null;
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		return stop == this.exitStop;
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		return true;
	}

	@Override
	public double getWeight() {
		return 1.0;
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
	public void setVehicle(MobsimVehicle veh) {
	}

	@Override
	public MobsimVehicle getVehicle() {
		return null;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return null;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return null;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return null;
	}

	@Override
	public String getMode() {
		throw new RuntimeException("not implemented") ;
	}

}
