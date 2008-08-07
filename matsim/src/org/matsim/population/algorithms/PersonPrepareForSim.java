/* *********************************************************************** *
 * project: org.matsim.*
 * PersonPrepareForSim.java
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
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;

/**
 * Performs several checks that persons are ready for a mobility simulation.
 * It is intended to run only once after the initial plans are read from file,
 * as we expect that no "damage" happens to the plans during the iterations.
 * <br/>
 * Currently, this only checks that in all plans the act's have a link assigned
 * and that all plans have valid routes, calculating missing links and
 * routes if required. Additionally, it will output a warning to the
 * log if a person has no plans at all.
 *
 * @author mrieser
 */
public class PersonPrepareForSim extends AbstractPersonAlgorithm {

	private final PlanAlgorithm router;
	private final XY2Links xy2links;

	private static final Logger log = Logger.getLogger(PersonPrepareForSim.class);

	public PersonPrepareForSim(final PlanAlgorithm router, final NetworkLayer network) {
		super();
		this.router = router;
		this.xy2links = new XY2Links(network);
	}

	@Override
	public void run(final Person person) {
		// first make sure we have a selected plan
		Plan selectedPlan = person.getSelectedPlan();
		if (selectedPlan == null) {
			// the only way no plan can be selected should be when the person has no plans at all
			log.warn("Person " + person.getId() + " has no plans!");
			return;
		}
		
		// make sure all the plans have valid act-locations and valid routes
		for (Plan plan : person.getPlans()) {
			boolean needsXY2Links = false;
			boolean needsReRoute = false;
			ArrayList<Object> actslegs = plan.getActsLegs();
			for (int i = 0; i < actslegs.size(); i++) {
				if (i % 2 == 0) {
					Act act = (Act)actslegs.get(i);
					if (act.getLink() == null) {
						needsXY2Links = true;
						needsReRoute = true;
						break;
					}
				} else {
					Leg leg = (Leg)actslegs.get(i);
					if (leg.getRoute() == null) {
						needsReRoute = true;
					}
				}
			}
			if (needsXY2Links) {
				this.xy2links.run(plan);
			}
			if (needsReRoute) {
				this.router.run(plan);
			}
		}

	}

}
