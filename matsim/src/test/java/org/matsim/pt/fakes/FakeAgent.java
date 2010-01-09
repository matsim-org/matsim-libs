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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.pt.queuesim.PassengerAgent;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitStopFacility;


/**
 * @author mrieser
 */
public class FakeAgent implements DriverAgent, PassengerAgent {

	private final TransitStopFacility exitStop;
	private final LegImpl dummyLeg;
	private final PersonImpl dummyPerson = new PersonImpl(new IdImpl(1));

	/**
	 * Creates a new fake Agent. If enterStop or exitStop are <code>null</code>,
	 * the leg will have no route.
	 *
	 * @param enterStop may be <code>null</code>
	 * @param exitStop may be <code>null</code>
	 */
	public FakeAgent(final TransitStopFacility enterStop, final TransitStopFacility exitStop) {
		this.exitStop = exitStop;
		this.dummyLeg = new LegImpl(TransportMode.pt);
		if ((enterStop != null) && (exitStop != null)) {
			GenericRoute route = new ExperimentalTransitRoute(enterStop, null, null, exitStop);
			route.setRouteDescription(enterStop.getLink(), "PT1 " + enterStop.getId().toString() + " T1 " + exitStop.getId().toString(), exitStop.getLink());
			this.dummyLeg.setRoute(route);
		}
	}

	@Override
	public void activityEnds(final double now) {
	}

	@Override
	public LinkImpl chooseNextLink() {
		return null;
	}

	@Override
	public LegImpl getCurrentLeg() {
		return this.dummyLeg;
	}

	@Override
	public double getDepartureTime() {
		return 0;
	}

	@Override
	public Id getDestinationLinkId() {
		return null;
	}

	@Override
	public PersonImpl getPerson() {
		return this.dummyPerson;
	}

	@Override
	public void legEnds(final double now) {
	}

	@Override
	public void moveOverNode() {
	}

	@Override
	public void teleportToLink(final Id linkId) {
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		return stop == this.exitStop;
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute route) {
		return true;
	}

}
