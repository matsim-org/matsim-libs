/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityFromActivity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.mz2005.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author illenberger
 *
 */
public class FacilityFromActivity {

	public static void createActivities(Population population, ActivityFacilities facilities) {
		for(Person person : population.getPersons().values()) {
			int k = 0;
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					
					if(act.getCoord() == null) 
						throw new NullPointerException("Activity has no coordinate.");
					
					Id<ActivityFacility> id = Id.create(String.format("tmp.%1$s.%2$s.%3$s", person.getId().toString(), k, i), ActivityFacility.class);
					ActivityFacility facility = facilities.getFactory().createActivityFacility(id, act.getCoord());
					facilities.addActivityFacility(facility);
					((ActivityImpl)act).setFacilityId(facility.getId());
				}
				k++;
			}
		}
	}
}
