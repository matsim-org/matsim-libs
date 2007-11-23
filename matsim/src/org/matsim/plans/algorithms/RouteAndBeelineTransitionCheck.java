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

package org.matsim.plans.algorithms;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.identifiers.IdI;


/**
 * @author glaemmel
 *
 */
//////////////////////////////////////////////////////////////////////
// RouteAndBeelineTransitionCheck checks if a route/beeline crosses a specific area
//////////////////////////////////////////////////////////////////////
public class RouteAndBeelineTransitionCheck implements PlanAlgorithmI {


	private NetworkLayer network = null;
	private Set<IdI> aOI = new HashSet<IdI>();
	private PlansCalcRoute router = null;

	//the result of RouteAndBeelineTransitionCheck
	// count[0] --> # beeline does not intersect AOI, plan leg does not intersect AOI
	// count[1] --> # beeline does not intersect AOI, plan leg intersect AOI
	// count[2] --> # beeline intersect AOI, plan leg does not intersect AOI
	// count[3] --> # beeline intersect AOI, plan leg intersect AOI
	public int[] count;


	public RouteAndBeelineTransitionCheck(NetworkLayer net, Set<IdI> areaOfInterest){
		this.network = net;
		this.aOI = areaOfInterest;
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.router = new PlansCalcRoute(this.network, timeCostCalc, timeCostCalc, false);
		this.count = new int[4];
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.plans.algorithms.PlanAlgorithmI#run(org.matsim.demandmodeling.plans.Plan)
	 */
	public void run(Plan plan) {
		Plan beeline = getBeeline(plan);

		Iterator itPlanLegs = plan.getIteratorLeg();
		Iterator itBeelineLegs = beeline.getIteratorLeg();

		while (itPlanLegs.hasNext()){
			Leg planLeg = (Leg) itPlanLegs.next();
			Leg beelineLeg = (Leg) itBeelineLegs.next();
			int type = 2 * intersectAOI(beelineLeg) + intersectAOI(planLeg);
			this.count[type]++;

		}

	}


	private int intersectAOI(Leg leg) {
		for (Link link : leg.getRoute().getLinkRoute()){
			if (this.aOI.contains(link.getId())) return 1;
		}
		return 0;
	}

	private Plan getBeeline(Plan plan){
		Plan beeline = new Plan(plan.getPerson());
		Iterator it = plan.getIteratorAct();
		beeline.addAct((Act) it.next());
		while (it.hasNext()){
			beeline.addLeg(new Leg(1,"car",0.0,0.0,0.0));
			beeline.addAct((Act) it.next());
		}
		this.router.run(beeline);
		return beeline;
	}

}
