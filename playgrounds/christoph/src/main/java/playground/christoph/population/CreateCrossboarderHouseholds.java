/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCrossboarderHouseholds.java
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

package playground.christoph.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateCrossboarderHouseholds {

	private String populationFile = "../../matsim/mysimulations/crossboarder/plansCBV2_with_facilities.xml.gz";
	private String networkFile = "../../matsim/mysimulations/crossboarder/network.xml.gz";
	private String facilitiesFile = "../../matsim/mysimulations/crossboarder/facilities.xml.gz";
	private String householdsFile = "../../matsim/mysimulations/crossboarder/households.xml.gz";
	private String objectAttributesFile = "../../matsim/mysimulations/crossboarder/householdObjectAttributes.xml.gz";
		
	public static void main(String[] args) throws Exception {
		new CreateCrossboarderHouseholds();
	}
	
	/*
	 * We create one household for each person using the person's Id.
	 * Additionally, we set the HHTP Code (Household type in Swiss Census) for every
	 * household to 1000, which is a one person only household.
	 */
	public CreateCrossboarderHouseholds() throws Exception {
		
		Config config = ConfigUtils.createConfig();
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.scenario().setUseKnowledge(true);
		config.scenario().setUseHouseholds(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		ActivityFacilities facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		Households households = ((ScenarioImpl) scenario).getHouseholds();
		ObjectAttributes householdAttributes = new ObjectAttributes();
		
		Counter counter = new Counter ("Created Households ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			
			Id householdId = person.getId();
			Id homeFacilityId = firstActivity.getFacilityId();
			Coord homeFacilityCoord = facilities.getFacilities().get(homeFacilityId).getCoord();
			
			Household household = households.getFactory().createHousehold(householdId);
			households.getHouseholds().put(householdId, household);
			
			household.getMemberIds().add(person.getId());
			householdAttributes.putAttribute(householdId.toString(), "homeFacilityId", homeFacilityId.toString());
			householdAttributes.putAttribute(householdId.toString(), "x", homeFacilityCoord.getX());
			householdAttributes.putAttribute(householdId.toString(), "y", homeFacilityCoord.getY());
			householdAttributes.putAttribute(householdId.toString(), "HHTP", 1000);
			counter.incCounter();
		}
		counter.printCounter();
		
		new HouseholdsWriterV10(((ScenarioImpl) scenario).getHouseholds()).writeFile(householdsFile);
		new ObjectAttributesXmlWriter(householdAttributes).writeFile(objectAttributesFile);
	}
}