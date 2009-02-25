/* *********************************************************************** *
 * project: org.matsim.*
 * ReroutingStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.mobsim;

import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.withinday.routeprovider.RouteProvider;

/**
 * @author illenberger
 * 
 */
public class ReroutingStrategy implements IntradayStrategy {

	private RouteProvider router;

	private PlanAgent agent;
	
	public ReroutingStrategy(PlanAgent agent, RouteProvider router) {
		this.agent = agent;
	}

	public Plan replan(double time) {
		if (allowReroute(time)) {
			/*
			 * TODO: Introduce standard clone() methods!
			 */
			Plan copy = new org.matsim.population.PlanImpl(agent.getPerson());
			copy.copyPlan(agent.getPerson().getSelectedPlan());

			CarRoute newRoute = getRoute(agent.getLink(), agent
					.getDestinationLink(time), time);
			/*
			 * TODO: Do some route validation, e.g. check if departure and
			 * destination links are consistent.
			 */
			adaptRoute(newRoute, (Leg) copy.getActsLegs().get(
					agent.getCurrentPlanIndex()), agent.getCurrentRouteIndex(), time);

			return copy;
		} else
			return null;
	}

	protected boolean allowReroute(double time) {
		if (agent.getCurrentPlanIndex() % 2 != 0) {
			if (agent.getNextLink(time) == null) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	protected CarRoute getRoute(Link origin, Link destination, double time) {
		return router.requestRoute(origin, destination, time);
	}
	
	protected void adaptRoute(CarRoute route, Leg leg, int index, double time) {
		/*
		 * TODO: Need link-based route implementation here.
		 * TODO: Move this to re-routing strategy?
		 */
	}
}
