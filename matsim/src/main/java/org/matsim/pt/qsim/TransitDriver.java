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

package org.matsim.pt.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.ptproject.qsim.interfaces.Mobsim;


public class TransitDriver extends AbstractTransitDriver {

	final NetworkRoute carRoute;

	final TransitLine transitLine;

	final TransitRoute transitRoute;

	final Departure departure;

	final double departureTime;

	private final Leg currentLeg;
	
	// as long as all instance variables are final, it should be ok to leave "resetCaches" empty.  kai, oct'10

	public TransitDriver(final TransitLine line, final TransitRoute route, final Departure departure, final TransitStopAgentTracker agentTracker, final Mobsim sim) {
		super(sim, agentTracker);
		PersonImpl driver = new PersonImpl(new IdImpl("ptDrvr_" + line.getId() + "_" + route.getId() + "_" + departure.getId().toString()));
		this.carRoute = route.getRoute();
		Plan plan = new PlanImpl();
		Leg leg = new LegImpl(TransportMode.car);
		leg.setRoute(getWrappedCarRoute(getCarRoute()));
		Activity startActivity = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, leg.getRoute().getStartLinkId());
		Activity endActiity = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, leg.getRoute().getEndLinkId());
		plan.addActivity(startActivity);
		plan.addLeg(leg);
		plan.addActivity(endActiity);
		this.currentLeg = leg;
		this.departureTime = departure.getDepartureTime();
		this.transitLine = line;
		this.transitRoute = route;
		this.departure = departure;
		setDriver(driver);
		init();
	}

	@Override
	public void endLegAndAssumeControl(final double now) {
//		this.getSimulation().handleAgentArrival(now, this);
		this.getSimulation().getEventsManager().processEvent(
				this.getSimulation().getEventsManager().getFactory().createAgentArrivalEvent(
						now, this.getPerson().getId(), this.getDestinationLinkId(), this.getCurrentLeg().getMode()));

		this.getSimulation().getAgentCounter().decLiving();
	}

	@Override
	public NetworkRoute getCarRoute() {
		return this.carRoute;
	}

	@Override
	public TransitLine getTransitLine() {
		return this.transitLine;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return this.transitRoute;
	}

	@Override
	public double getDepartureTimeForLeg() {
		return this.departureTime;
	}

	@Override
	public Leg getCurrentLeg() {
		return this.currentLeg;
	}

	@Override
	public Activity getCurrentActivity() {
		// As far as I can see, there is never a current Activity. cdobler, nov'10
		return null;
	}
	
	@Override
	public PlanElement getCurrentPlanElement() {
		return this.currentLeg ; // always a leg (?)
	}

	@Override
	public Id getDestinationLinkId() {
		return this.currentLeg.getRoute().getEndLinkId();
	}

	@Override
	public Departure getDeparture() {
		return this.departure;
	}

}
