/* *********************************************************************** *
 * project: org.matsim.*
 * RouteAndBeelineTransitionCheck.java
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

package org.matsim.population.algorithms;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;

/**
 * Checks if the fastest route between two activities ("beeline route") crosses a specific area.
 *
 * @author glaemmel
 */
public class RouteAndBeelineTransitionCheck implements PlanAlgorithm {

	private Network network = null;
	private Set<Id> aOI = new HashSet<Id>();
	private PlansCalcRoute router = null;

	// the result of RouteAndBeelineTransitionCheck
	// count[0] --> # beeline does not intersect AOI, plan leg does not intersect AOI
	// count[1] --> # beeline does not intersect AOI, plan leg intersect AOI
	// count[2] --> # beeline intersect AOI, plan leg does not intersect AOI
	// count[3] --> # beeline intersect AOI, plan leg intersect AOI
	public int[] count;

	public RouteAndBeelineTransitionCheck(final Network net, final Set<Id> areaOfInterest) {
		this.network = net;
		this.aOI = areaOfInterest;
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.router = new PlansCalcRoute(this.network, timeCostCalc, timeCostCalc);
		this.count = new int[4];
	}

	public void run(final Plan plan) {
		Plan beeline = getBeeline(plan);

		Iterator<PlanElement> itPlan = plan.getPlanElements().iterator();
		Iterator<PlanElement> itBeeline = beeline.getPlanElements().iterator();

		Leg planLeg = getNextLeg(itPlan);
		while (planLeg != null) {
			Leg beelineLeg = getNextLeg(itBeeline);
			int type = 2 * intersectAOI(beelineLeg) + intersectAOI(planLeg);
			this.count[type]++;
			planLeg = getNextLeg(itPlan);
		}
	}
	
	private Leg getNextLeg(Iterator<PlanElement> iterator) {
		while (iterator.hasNext()) {
			PlanElement pe = iterator.next();
			if (pe instanceof Leg) {
				return (Leg) pe;
			}
		}
		return null;
	}

	private int intersectAOI(final Leg leg) {
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		for (Link link : route.getLinks()) {
			if (this.aOI.contains(link.getId()))
				return 1;
		}
		return 0;
	}

	private Plan getBeeline(final Plan plan) {
		Plan beeline = new PlanImpl(plan.getPerson());
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = new org.matsim.core.population.LegImpl(TransportMode.car);
				leg.setDepartureTime(0.0);
				leg.setTravelTime(0.0);
				leg.setArrivalTime(0.0);
				beeline.addLeg(leg);
			} else {
				beeline.getPlanElements().add(pe);
			}
		}
		this.router.run(beeline);
		return beeline;
	}

}
