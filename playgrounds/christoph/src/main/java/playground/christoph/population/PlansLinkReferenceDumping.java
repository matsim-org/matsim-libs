/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

/*
 * Dumps LinkIds out of all plans.
 */
public class PlansLinkReferenceDumping {

//	private static String networkFile = "../../matsim/mysimulations/multimodal/input/network.xml.gz";
//	private static String facilitiesFile = "../../matsim/mysimulations/multimodal/input/facilities_KTIYear2.xml.gz";
//	private static String populationFile = "../../matsim/mysimulations/multimodal/input/plans.xml.gz";
//	private static String populationOutFile = "../../matsim/mysimulations/multimodal/input/out_plans.xml.gz";

	private static String networkFile = "/data/matsim/cdobler/Dissertation/InitialRoutes/input_Zurich_TeleAtlas/network.xml.gz";
	private static String facilitiesFile = "/data/matsim/cdobler/Dissertation/InitialRoutes/input_Zurich_IVTCH/facilities.xml.gz";
	private static String populationFile = "/data/matsim/cdobler/Dissertation/InitialRoutes/input_Zurich_TeleAtlas/plans_kti_10pct.xml.gz";
	private static String populationOutFile = "/data/matsim/cdobler/Dissertation/InitialRoutes/input_Zurich_IVTCH/plans_tki_10pct.xml.gz";
	
	
	private static final String separator = System.getProperty("file.separator");

	public static void main(String[] args) {
		networkFile = networkFile.replace("/", separator);
		facilitiesFile = facilitiesFile.replace("/", separator);
		populationFile = populationFile.replace("/", separator);

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.facilities().setInputFile(facilitiesFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		
		Network network = scenario.getNetwork();
		ActivityFacilities facilities = scenario.getActivityFacilities();
		Population population = scenario.getPopulation();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
//				plan.setScore(null);
				
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						ActivityImpl activity = (ActivityImpl) planElement;
						activity.setLinkId(null);
						
						// if available, use faciliy's link
						Id facilityId = activity.getFacilityId();
						if (facilityId != null) {
							activity.setLinkId(facilities.getFacilities().get(facilityId).getLinkId());
						}
							
					} else if (planElement instanceof Leg) {
						LegImpl leg = (LegImpl) planElement;
						leg.setRoute(null);
					}
				}
			}
		}

		// use a V4 Writer which supports Desires
		new PopulationWriter(population, network).writeFileV4(populationOutFile);
		System.out.println("Done");
	}
}
