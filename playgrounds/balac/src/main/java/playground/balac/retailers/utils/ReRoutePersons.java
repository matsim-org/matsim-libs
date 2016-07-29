/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.balac.retailers.utils;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

public class ReRoutePersons {

	private final static Logger log = Logger.getLogger(ReRoutePersons.class);


	public void run (
			Map<Id<ActivityFacility>,ActivityFacilityImpl> movedFacilities,
			Network network,
			Map<Id<Person>, Person> persons,
			PersonAlgorithm pcrl,
			ActivityFacilities facilities){
		log.info("movedFacilities= " + movedFacilities);
		int counterPlans = 0;
		int counterPersons = 0;
		for (Person p : persons.values()) {
			boolean routeIt = false;
			for (Plan plan:p.getPlans()) {

				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (movedFacilities.containsKey(act.getFacilityId())) {
							act.setLinkId(((ActivityFacilityImpl) facilities.getFacilities().get((act.getFacilityId()))).getLinkId());
							routeIt = true;
						}
					}
				}
			}
			if (routeIt) {
				//log.info("reRouting person: " + p.getId());
				pcrl.run(p);
				counterPersons = counterPersons+1;
			}
		}
		log.info("The program modified " +  counterPlans + " plans");
		log.info("The program re-routed " +  counterPersons + " persons who were shopping in moved facilities");
	}
}