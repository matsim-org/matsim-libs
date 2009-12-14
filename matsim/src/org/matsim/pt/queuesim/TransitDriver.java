/* *********************************************************************** *
 * project: org.matsim.*
 * BusDriver.java
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

package org.matsim.pt.queuesim;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.ptproject.qsim.Simulation;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;


public class TransitDriver extends AbstractTransitDriver {

	private static PersonImpl createDummyPerson(final TransitLine line,
			final TransitRoute route, final Departure departure) {
		PersonImpl dummyPerson = new PersonImpl(new IdImpl("ptDrvr_" + line.getId() + "_" + route.getId() + "_" + departure.getId().toString()));
		return dummyPerson;
	}
	
	final NetworkRouteWRefs carRoute;
	final TransitLine transitLine;
	final TransitRoute transitRoute;
	final double departureTime;
	private final LegImpl currentLeg = new LegImpl(TransportMode.car);

	public TransitDriver(final TransitLine line, final TransitRoute route, final Departure departure, final TransitStopAgentTracker agentTracker, final QueueSimulation sim) {
		super(createDummyPerson(line, route, departure), sim, agentTracker);
		this.departureTime = departure.getDepartureTime();
		this.transitLine = line;
		this.transitRoute = route;
		this.carRoute = route.getRoute();
		this.currentLeg.setRoute(getWrappedCarRoute()); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
		init();
	}

	@Override
	public void legEnds(final double now) {
		this.getSimulation().handleAgentArrival(now, this);
		Simulation.decLiving();
	}

	@Override
	public NetworkRouteWRefs getCarRoute() {
		return carRoute;
	}

	@Override
	public TransitLine getTransitLine() {
		return transitLine;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	@Override
	public double getDepartureTime() {
		return departureTime;
	}
	
	public LegImpl getCurrentLeg() {
		return this.currentLeg;
	}

	public Link getDestinationLink() {
		return this.currentLeg.getRoute().getEndLink();
	}

}
