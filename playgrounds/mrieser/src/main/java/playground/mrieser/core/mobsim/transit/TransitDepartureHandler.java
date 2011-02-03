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

package playground.mrieser.core.mobsim.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import playground.mrieser.core.mobsim.api.DepartureHandler;
import playground.mrieser.core.mobsim.api.PlanAgent;

/**
 * @author mrieser
 */
public class TransitDepartureHandler implements DepartureHandler {

	private final TransitStopAgentTracker agentTracker;

	public TransitDepartureHandler(final TransitStopAgentTracker agentTracker) {
		this.agentTracker = agentTracker;
	}

	@Override
	public void handleDeparture(final PlanAgent agent) {
		Leg leg = (Leg) agent.getCurrentPlanElement();
		ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		Id accessStopId = route.getAccessStopId();
		PassengerAgent passenger = new PassengerAgentImpl(agent.getPlan().getPerson().getId(), route, agent.getWeight());
		this.agentTracker.addAgentToStop(passenger, accessStopId);
	}

}
