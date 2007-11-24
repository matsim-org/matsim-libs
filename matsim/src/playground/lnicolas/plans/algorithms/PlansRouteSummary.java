/* *********************************************************************** *
 * project: org.matsim.*
 * PlansRouteSummary.java
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

package playground.lnicolas.plans.algorithms;

import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PlansRouteSummary extends PersonAlgorithm implements PlanAlgorithmI {

	private long routesCount = 0;
	private double avgRouteDist = 0;
	private Route routeMaxDist = null;
	private double avgRouteTravTime = 0;
	private Route routeMaxTravTime = null;
	
	public PlansRouteSummary() {
		super();
	}

	@Override
	public void run(Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			try {
				handlePlan(plan);
			} catch (Exception e) {
				Gbl.warningMsg(this.getClass(),
						"run(Person)", "Skipping plan id="+planId + " of person id="
						+ person.getId() + " because of: " + e.getMessage());
			}
		}
	}

	public void run(Plan plan) {
		try {
			handlePlan(plan);
		} catch (Exception e) {
			Gbl.warningMsg(this.getClass(), "run(Person)",
					"Skipping plan id=unknown of person id=unknown because of: "
					+ e.getMessage());
		}
	}

	public void handlePlan(Plan plan) throws Exception {
		ArrayList actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);

		// loop over all <act>s
		for (int j = 2; j < actslegs.size(); j=j+2) {
			Act toAct = (Act)actslegs.get(j);
			Leg leg = (Leg)actslegs.get(j-1);
			
			Link startlink = fromAct.getLink();
			if (startlink == null) throw new Exception("start link missing");
			
			Route route = leg.getRoute();
			if (route == null) throw new Exception("route missing");
			
			double dist = route.getDist();
			double travTime = route.getTravTime();
			
			this.avgRouteDist = ((this.avgRouteDist * this.routesCount
					+ dist) / (this.routesCount + 1));
			this.avgRouteTravTime = ((this.avgRouteTravTime * this.routesCount
					+ travTime) / (this.routesCount + 1));
			if (this.routeMaxDist == null
					|| dist > this.routeMaxDist.getDist()) {
				this.routeMaxDist = route;
			}
			if (this.routeMaxTravTime == null
					|| travTime > this.routeMaxTravTime.getTravTime()) {
				this.routeMaxTravTime = route;
			}
			this.routesCount++;
			
			fromAct = toAct;
		}
	}
	
	/**
	 * Prints the route details to the console.
	 */
	public void printSummary() {
		System.out.println("Number of routes: " + this.routesCount);
		System.out.println("Avg distance per route: " + this.avgRouteDist);
		System.out.println("Avg travel time per route: " + this.avgRouteTravTime);
		System.out.println("");
	}
}
