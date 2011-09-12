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
package playground.johannes.mz2005.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;

/**
 * @author illenberger
 *
 */
public class FacilityFromActivity {

	public static void createActivities(Population population, ActivityFacilitiesImpl facilities) {
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					
					if(act.getCoord() == null) 
						throw new NullPointerException("Activity has no coordinate.");
					
					Id id = new IdImpl(String.format("tmp.%1$s.%2$s", person.getId().toString(), i));
					ActivityFacilityImpl facility = facilities.createFacility(id, act.getCoord());
					((ActivityImpl)act).setFacilityId(facility.getId());
				}
			}
		}
	}
}
