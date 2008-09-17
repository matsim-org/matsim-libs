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
import java.util.Set;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

/**
 * Checks if the fastest route between two activities ("beeline route") crosses a specific area.
 *
 * @author glaemmel
 */
public class RouteAndBeelineTransitionCheck implements PlanAlgorithm {

	private NetworkLayer network = null;
	private Set<Id> aOI = new HashSet<Id>();
	private PlansCalcRoute router = null;

	// the result of RouteAndBeelineTransitionCheck
	// count[0] --> # beeline does not intersect AOI, plan leg does not intersect AOI
	// count[1] --> # beeline does not intersect AOI, plan leg intersect AOI
	// count[2] --> # beeline intersect AOI, plan leg does not intersect AOI
	// count[3] --> # beeline intersect AOI, plan leg intersect AOI
	public int[] count;

	public RouteAndBeelineTransitionCheck(final NetworkLayer net, final Set<Id> areaOfInterest) {
		this.network = net;
		this.aOI = areaOfInterest;
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.router = new PlansCalcRoute(this.network, timeCostCalc, timeCostCalc);
		this.count = new int[4];
	}

	public void run(final Plan plan) {
		Plan beeline = getBeeline(plan);

		LegIterator itPlanLegs = plan.getIteratorLeg();
		LegIterator itBeelineLegs = beeline.getIteratorLeg();

		while (itPlanLegs.hasNext()) {
			Leg planLeg = (Leg) itPlanLegs.next();
			Leg beelineLeg = (Leg) itBeelineLegs.next();
			int type = 2 * intersectAOI(beelineLeg) + intersectAOI(planLeg);
			this.count[type]++;
		}
	}

	private int intersectAOI(final Leg leg) {
		for (Link link : leg.getRoute().getLinkRoute()) {
			if (this.aOI.contains(link.getId()))
				return 1;
		}
		return 0;
	}

	private Plan getBeeline(final Plan plan) {
		Plan beeline = new Plan(plan.getPerson());
		ActIterator it = plan.getIteratorAct();
		beeline.addAct(it.next());
		while (it.hasNext()) {
			Leg leg = new Leg(BasicLeg.Mode.car);
			leg.setNum(1);
			leg.setDepTime(0.0);
			leg.setTravTime(0.0);
			leg.setArrTime(0.0);
			beeline.addLeg(leg);
			beeline.addAct(it.next());
		}
		this.router.run(beeline);
		return beeline;
	}

}
