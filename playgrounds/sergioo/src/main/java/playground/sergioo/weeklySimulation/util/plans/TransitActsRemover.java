/* *********************************************************************** *
 * project: org.matsim.*
 * TransitInteractionRemover.java
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

package playground.sergioo.weeklySimulation.util.plans;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

import playground.sergioo.passivePlanning2012.core.population.BasePlanImpl;

/**
 * Removes all transit activities (like "pt -interaction") as well as the legs
 * following those activities. In addition, all legs with mode "transit_walk"
 * are set to mode "pt" to be routed again with the transit. 
 * 
 * @see PtConstants#TRANSIT_ACTIVITY_TYPE
 * 
 * @author mrieser
 */
public class TransitActsRemover implements PlanAlgorithm {

	@Override
	public void run(final Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		for (int i = 0, n = planElements.size(); i < n; i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {
					double travelTime = ((Leg) plan.getPlanElements().get(i-1)).getTravelTime();
					Route route = ((Leg) plan.getPlanElements().get(i-1)).getRoute();
					if(plan instanceof BasePlanImpl)
						((BasePlanImpl) plan).removeActivity(i);
					else
						((PlanImpl) plan).removeActivity(i);
					((Leg) plan.getPlanElements().get(i-1)).setTravelTime(travelTime);
					((Leg) plan.getPlanElements().get(i-1)).setRoute(route);
					n -= 2;
					i--; // i will be incremented again in next loop-iteration, so we'll check the next act
				}
			} else if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (TransportMode.transit_walk.equals(leg.getMode())) {
					leg.setMode(TransportMode.pt);
					double distance = 0;
					double travelTime = 0;
					Id<Link> endLinkId = null;
					DISTANCE_CALC:
					for(int j = i; ;j++) {
						PlanElement pe2 = planElements.get(j);
						if(!(pe2 instanceof Activity && !PtConstants.TRANSIT_ACTIVITY_TYPE.equals(((Activity)pe2).getType()))) {
							if(pe2 instanceof Leg) {
								travelTime += ((Leg)pe2).getTravelTime();
								if(((Leg)pe2).getRoute()!=null) {
									distance += ((Leg)pe2).getRoute().getDistance();
									endLinkId = ((Leg) pe2).getRoute().getEndLinkId();
								}
							}
						}
						else
							break DISTANCE_CALC;
					}
					leg.setTravelTime(travelTime);
					Route route = new GenericRouteImpl(leg.getRoute()==null?null:leg.getRoute().getStartLinkId(), endLinkId);
					route.setTravelTime(travelTime);
					route.setDistance(distance);
					leg.setRoute(route);
				}
			}
		}
	}

}
