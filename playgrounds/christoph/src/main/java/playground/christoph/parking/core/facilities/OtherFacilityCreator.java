/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingFacilityCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.core.facilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;

/**
 * Creates facilities for each link where agents perform activities according to
 * their activity types.
 * 
 * @author cdobler
 */
public class OtherFacilityCreator {
	
	public static void createParkings(Scenario scenario) {
		
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (activity.getFacilityId() == null) {
						Id linkId = activity.getLinkId();
						Link link = scenario.getNetwork().getLinks().get(linkId);
						
						ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(linkId);
						if (facility == null) {
							facility = factory.createActivityFacility(linkId, link.getCoord());
							((ActivityFacilityImpl) facility).setLinkId(link.getId());
							scenario.getActivityFacilities().addActivityFacility(facility);
						}
						
						if (facility.getActivityOptions().get(activity.getType()) == null) {
							ActivityOption activityOption = factory.createActivityOption(activity.getType());
							facility.addActivityOption(activityOption);
						}
						
						((ActivityImpl) activity).setFacilityId(linkId);
					}
				}
			}
		}
	}
}