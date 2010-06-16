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

package playground.mrieser.core.sim.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.mrieser.core.sim.api.DepartureHandler;
import playground.mrieser.core.sim.api.NewSimEngine;
import playground.mrieser.core.sim.api.PlanAgent;
import playground.mrieser.core.sim.api.PlanElementHandler;

/**
 * @author mrieser
 */
public class LegHandler implements PlanElementHandler {

	private final NewSimEngine simEngine;
	private final ConcurrentHashMap<TransportMode, DepartureHandler> departureHandlers = new ConcurrentHashMap<TransportMode, DepartureHandler>();

	public LegHandler(final NewSimEngine simEngine) {
		this.simEngine = simEngine;
	}

	@Override
	public void handleStart(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		EventsManager em = this.simEngine.getEventsManager();

		// find departure link id
		// TODO [MR] improve this, or remove this information from event
		Activity prevActivity = null;
		for (PlanElement pe : agent.getPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				prevActivity = (Activity) pe;
			}
			if (pe == leg) {
				break;
			}
		}

		// generate event
		em.processEvent(em.getFactory().createAgentDepartureEvent(this.simEngine.getCurrentTime(), agent.getPlan().getPerson().getId(), prevActivity.getLinkId(), leg.getMode()));

		// process departure
		DepartureHandler depHandler = getDepartureHandler(leg.getMode());
		if (depHandler == null) {
			throw new NullPointerException("No DepartureHandler registered for mode " + leg.getMode());
		}
		depHandler.handleDeparture(agent);
	}

	@Override
	public void handleEnd(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		EventsManager em = this.simEngine.getEventsManager();

		// find departure link id
		// TODO [MR] improve this, or remove this information from event
		boolean breakAtNextActivity = false;
		Activity nextActivity = null;
		for (PlanElement pe : agent.getPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				nextActivity = (Activity) pe;
				if (breakAtNextActivity) {
					break;
				}
			}
			if (pe == leg) {
				breakAtNextActivity = true;
			}
		}

		// generate event
		em.processEvent(em.getFactory().createAgentArrivalEvent(this.simEngine.getCurrentTime(), agent.getPlan().getPerson().getId(), nextActivity.getLinkId(), leg.getMode()));
	}

	public DepartureHandler setDepartureHandler(final TransportMode mode, final DepartureHandler handler) {
		return this.departureHandlers.put(mode, handler);
	}

	public DepartureHandler removeDepartureHandler(final TransportMode mode) {
		return this.departureHandlers.remove(mode);
	}

	/*package*/ DepartureHandler getDepartureHandler(final TransportMode mode) {
		return this.departureHandlers.get(mode);
	}

}
