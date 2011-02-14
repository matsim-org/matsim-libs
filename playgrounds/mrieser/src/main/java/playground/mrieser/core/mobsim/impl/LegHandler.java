/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.api.MobsimKeepAlive;
import playground.mrieser.core.mobsim.api.NewMobsimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;

/**
 * @author mrieser
 */
public class LegHandler implements PlanElementHandler, MobsimKeepAlive {

	private final NewMobsimEngine simEngine;
	private final ConcurrentHashMap<String, DepartureHandler> departureHandlers = new ConcurrentHashMap<String, DepartureHandler>();
	private AtomicInteger onRoute = new AtomicInteger(0);

	public LegHandler(final NewMobsimEngine simEngine) {
		this.simEngine = simEngine;
		this.simEngine.addKeepAlive(this);
	}

	@Override
	public void handleStart(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		EventsManager em = this.simEngine.getEventsManager();

		Id linkId = leg.getRoute().getStartLinkId();

		// generate event
		em.processEvent(em.getFactory().createAgentDepartureEvent(this.simEngine.getCurrentTime(), agent.getPlan().getPerson().getId(), linkId, leg.getMode()));

		// process departure
		DepartureHandler depHandler = getDepartureHandler(leg.getMode());
		if (depHandler == null) {
			throw new NullPointerException("No DepartureHandler registered for mode " + leg.getMode());
		}
		depHandler.handleDeparture(agent);
		this.onRoute.incrementAndGet();
	}

	@Override
	public void handleEnd(final PlanAgent agent) {
		this.onRoute.decrementAndGet();
		Leg leg = (Leg) agent.getCurrentPlanElement();
		EventsManager em = this.simEngine.getEventsManager();

		Id linkId = leg.getRoute().getEndLinkId();

		// generate event
		em.processEvent(em.getFactory().createAgentArrivalEvent(this.simEngine.getCurrentTime(), agent.getPlan().getPerson().getId(), linkId, leg.getMode()));
	}

	public DepartureHandler setDepartureHandler(final String mode, final DepartureHandler handler) {
		return this.departureHandlers.put(mode, handler);
	}

	public DepartureHandler removeDepartureHandler(final String mode) {
		return this.departureHandlers.remove(mode);
	}

	/*package*/ DepartureHandler getDepartureHandler(final String mode) {
		return this.departureHandlers.get(mode);
	}

	@Override
	public boolean keepAlive() {
		return this.onRoute.intValue() > 0;
	}

}
