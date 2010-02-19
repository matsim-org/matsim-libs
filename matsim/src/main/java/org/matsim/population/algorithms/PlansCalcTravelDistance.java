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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * Calculates the sum of the distance of all routes in a plan,
 * including the start link, but excluding the target link.
 * The algorithm does not care about the leg-mode!
 */
public class PlansCalcTravelDistance extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansCalcTravelDistance.class);
	private final Network network;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcTravelDistance(Network network) {
		super();
		this.network = network;
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

	public void handlePlan(final Plan plan) throws Exception {
		List<? extends PlanElement> actslegs = plan.getPlanElements();

		for (PlanElement pe : actslegs) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;

				ArrayList<Id> linkIds = new ArrayList<Id>();

				NetworkRoute route = (NetworkRoute) leg.getRoute();
				if (route == null) throw new Exception("route missing");

				linkIds.add(route.getStartLinkId());
				linkIds.addAll(route.getLinkIds());
				double dist = calcDistance(linkIds);

				route.setDistance(dist);
			}
		}
	}

	private double calcDistance(final List<Id> linkIds) {
		double dist = 0.0;
		for (Id linkId : linkIds) {
			Link link = this.network.getLinks().get(linkId);
			dist += link.getLength();
		}
		return dist;
	}
}
