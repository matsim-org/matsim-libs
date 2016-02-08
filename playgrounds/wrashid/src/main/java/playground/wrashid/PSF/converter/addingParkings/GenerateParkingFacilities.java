/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

/*
 * For each activity, define a parking and facility (and home...)
 */
public class GenerateParkingFacilities {

	/**
	 * Generate an output facilities file, which contains parking facilities at the same place, as the activities happen.
	 * @param inputPlansFile
	 * @param networkFile
	 * @param outputFacilitiesFile
	 */
	
	public static void generateParkingFacilties(Scenario scenario) {

		// generate facilities

		ActivityFacilities facilities = scenario.getActivityFacilities();

		for (Person person : scenario.getPopulation().getPersons().values()) {

			for (int i = 0; i < person.getSelectedPlan().getPlanElements()
					.size(); i++) {
				if (person.getSelectedPlan().getPlanElements().get(i) instanceof Activity) {
					Activity act = (Activity) person.getSelectedPlan()
							.getPlanElements().get(i);
					Id<ActivityFacility> facilityId=Id.create( "facility_" + act.getLinkId().toString(), ActivityFacility.class);
					
					// add facility only, if it does not already exist
					if (!facilities.getFacilities().containsKey(facilityId)){
						ActivityFacilityImpl facility = (ActivityFacilityImpl) facilities.getFactory().createActivityFacility(facilityId, act.getCoord());
						facilities.addActivityFacility(facility);
						facility.createAndAddActivityOption(act.getType());
						facility.createAndAddActivityOption("parkingArrival");
						facility.createAndAddActivityOption("parkingDeparture");
					}
					
					//facilities.getFacilities().put(facilityId, facility);
				}
			}
			
		}

		// write facilities out to file
//		GeneralLib.writeActivityFacilities(facilities, outputFacilitiesFile);
	}
}
