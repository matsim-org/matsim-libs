/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcTravelDistance.java
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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Route;

public class PlansCalcTravelDistance extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansCalcTravelDistance.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcTravelDistance() {
		super();
		log.info("This algo does not care about the mode! It calculates the distance including the start link and excluding the target link.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			try {
				handlePlan(plan);
			} catch (Exception e) {
				log.warn("Skipping plan id="+planId + " of person id=" + person.getId() + " because of: " + e.getMessage());
			}
		}
	}

	public void run(final Plan plan) {
		try {
			handlePlan(plan);
		} catch (Exception e) {
			log.warn("Skipping plan id=unknown of person id=unknown because of: " + e.getMessage());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	public void handlePlan(final Plan plan) throws Exception {
		ArrayList<?> actslegs = plan.getActsLegs();
		Act fromAct = (Act)actslegs.get(0);

		// loop over all <act>s
		for (int j = 2; j < actslegs.size(); j=j+2) {
			Act toAct = (Act)actslegs.get(j);
			Leg leg = (Leg)actslegs.get(j-1);

			Link startlink = fromAct.getLink();
			if (startlink == null) throw new Exception("start link missing");
			Node startnode = startlink.getFromNode();

			ArrayList<Node> nodes = new ArrayList<Node>();
			nodes.add(startnode);

			Route route = leg.getRoute();
			if (route == null) throw new Exception("route missing");
			nodes.addAll(route.getRoute());

			double dist = calcDistance(nodes);

			route.setDist(dist);

			fromAct = toAct;
		}
	}

	private double calcDistance(final ArrayList<Node> nodes) {
		double dist = 0.0;
		for (int i=0; i<nodes.size()-1; i++) {
			Node from = nodes.get(i);
			Node to = nodes.get(i+1);

			for (Link currlink : from.getOutLinks().values()) {
				if (currlink.getToNode().equals(to)) {
					dist += currlink.getLength();
					break;
				}
			}
		}
		return dist;
	}
}
