/* *********************************************************************** *
 * project: org.matsim.*
 * AssignInitialFacilities.java
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

package playground.anhorni.locationchoice.preprocess.plans.modifications;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.locationchoice.utils.QuadTreeRing;


public class AssignInitialFacilities {

	private final static Logger log = Logger.getLogger(AssignInitialFacilities.class);
	private QuadTreeRing<ActivityFacilityImpl> actTree = null;

	public AssignInitialFacilities(final QuadTreeRing<ActivityFacilityImpl> actTree) {
		this.actTree = actTree;
	}


	public void run(Population plans, String type, String mode) {

		log.info("Assigning inital " + type + "facilities ...");
		Counter counter = new Counter("Person :");

		for (Person person : plans.getPersons().values()) {
			counter.incCounter();

			if (person.getPlans().size() != 1) {
				Gbl.errorMsg("pid = " + person.getId() + " : There must be exactly one plan.");
			}
			PlanImpl plan = (PlanImpl)person.getSelectedPlan();
			if (type.startsWith("shop")) {
				AssignInitalShopFacilities initialShopFacilitiesAssigner = new AssignInitalShopFacilities(this.actTree);
				initialShopFacilitiesAssigner.run(plan, type);
			}
			else if (type.startsWith("leisure")) {
				AssignInitialLeisureFacilities initalLeisureFacilitiesAssigner =
					new AssignInitialLeisureFacilities(this.actTree);
				initalLeisureFacilitiesAssigner.run(plan, mode);
			}
			else {
				log.error("This should never happen!");
			}
		}
	}
}

