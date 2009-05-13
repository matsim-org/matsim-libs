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

package playground.marcel.pt.mocks;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.GenericRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

import playground.marcel.pt.integration.ExperimentalTransitRoute;
import playground.marcel.pt.interfaces.PassengerAgent;
import playground.marcel.pt.transitSchedule.TransitLine;

public class MockAgent implements DriverAgent, PassengerAgent {

	private final ActivityFacility exitStop;
	private final Leg dummyLeg;
	private final Person dummyPerson = new PersonImpl(new IdImpl(1));
	
	public MockAgent(final ActivityFacility enterStop, final ActivityFacility exitStop) {
		this.exitStop = exitStop;
		this.dummyLeg = new LegImpl(TransportMode.pt);
		GenericRoute route = new ExperimentalTransitRoute(enterStop.getLink(), exitStop.getLink());
		route.setRouteDescription(enterStop.getLink(), "PT1 " + enterStop.getId().toString() + " T1 " + exitStop.getId().toString(), exitStop.getLink());
		this.dummyLeg.setRoute(route);
	}
	
	public void activityEnds(final double now) {
	}

	public Link chooseNextLink() {
		return null;
	}

	public Leg getCurrentLeg() {
		return this.dummyLeg;
	}

	public double getDepartureTime() {
		return 0;
	}

	public Link getDestinationLink() {
		return null;
	}

	public Person getPerson() {
		return this.dummyPerson;
	}

	public void legEnds(final double now) {
	}

	public void moveOverNode() {
	}

	public void teleportToLink(Link link) {
	}

	public boolean arriveAtStop(final ActivityFacility stop) {
		return stop == exitStop;
	}

	public boolean ptLineAvailable(final TransitLine line) {
		return true;
	}

}
