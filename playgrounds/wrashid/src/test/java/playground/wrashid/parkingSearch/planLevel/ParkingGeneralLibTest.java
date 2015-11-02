/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;

public class ParkingGeneralLibTest extends MatsimTestCase {

	public void testGetAllParkingFacilityIds() {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(super.loadConfig(null));

		BaseNonControlerScenario.loadNetwork(sc);

		LinkedList<Id<ActivityFacility>> parkingFacilityIds = ParkingGeneralLib.getAllParkingFacilityIds(sc.getPopulation().getPersons()
				.get(Id.create(1, Person.class)).getSelectedPlan());

		assertEquals(2, parkingFacilityIds.size());
		assertEquals("36", parkingFacilityIds.get(0).toString());
		assertEquals("1", parkingFacilityIds.get(1).toString());
	}
	
	public void testGetParkingRelatedWalkingDistanceOfWholeDay(){
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(super.loadConfig(null));

		BaseNonControlerScenario.loadNetwork(sc);
		
		double parkingRelatedWalkingDistance=ParkingGeneralLib.getParkingRelatedWalkingDistanceOfWholeDayAveragePerLeg(sc.getPopulation().getPersons()
				.get(Id.create(1, Person.class)).getSelectedPlan(),sc.getActivityFacilities());
		
		assertEquals(0.0, parkingRelatedWalkingDistance);
		
	}
}
