/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.boescpa.ivtBaseline.preparation.*;
import playground.boescpa.lib.tools.fileCreation.F2LConfigGroup;
import playground.boescpa.lib.tools.fileCreation.F2LCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author boescpa
 */
public class TestRunBaseline {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {

		final String pathToOnlyStreetNetwork = utils.getClassInputDirectory() + "onlystreetnetwork.xml";
		final String pathToNetwork = "test/scenarios/pt-tutorial/multimodalnetwork.xml";
		final String pathToInitialPopulation = utils.getClassInputDirectory() + "population.xml";
		final String pathToPopulation = utils.getOutputDirectory() + "population.xml";
		final String pathToPrefs = utils.getOutputDirectory() + "prefs.xml";
		final String pathToFacilities = utils.getOutputDirectory() + "facilities.xml";
		final String pathToF2L = utils.getOutputDirectory() + "f2l.f2l";
		final String pathToConfig = utils.getOutputDirectory() + "config.xml";
		final String pathToSchedule = "test/scenarios/pt-tutorial/transitschedule.xml";
		final String pathToVehicles = "test/scenarios/pt-tutorial/transitVehicles.xml";

		Scenario tempScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(tempScenario.getNetwork()).readFile(pathToOnlyStreetNetwork);
		new MatsimPopulationReader(tempScenario).readFile(pathToInitialPopulation);
		createPrefs(tempScenario, pathToPrefs);
		createFacilities(tempScenario, pathToFacilities);
		F2LCreator.createF2L(tempScenario, pathToF2L);
		new PopulationWriter(tempScenario.getPopulation()).write(pathToPopulation);

		// create config
		String[] argsConfig = {pathToConfig, "100"};
		IVTConfigCreator.main(argsConfig);
		Config config = ConfigUtils.loadConfig(pathToConfig, new F2LConfigGroup());
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory() + "output/");
			// Reduce iterations to one write out interval + 1
		config.setParam("controler", "lastIteration", "11");
			// Set files
		config.setParam("facilities", "inputFacilitiesFile", pathToFacilities);
		config.setParam("f2l", "inputF2LFile", pathToF2L);
		config.setParam("households", "inputFile", "null");
		config.setParam("households", "inputHouseholdAttributesFile", "null");
		config.setParam("network", "inputNetworkFile", pathToNetwork);
		config.setParam("plans", "inputPersonAttributesFile", pathToPrefs);
		config.setParam("plans", "inputPlansFile", pathToPopulation);
		config.setParam("transit", "transitScheduleFile", pathToSchedule);
		config.setParam("transit", "vehiclesFile", pathToVehicles);
			// Set threads to 1
		config.setParam("global", "numberOfThreads", "1");
		config.setParam("parallelEventHandling", "numberOfThreads", "1");
		config.setParam("qsim", "numberOfThreads", "1");
		new ConfigWriter(config).write(pathToConfig);

		String[] argsSim = {pathToConfig};
		RunIVTBaseline.main(argsSim);
	}

	@Test
	public void testScenario() {

	}

	public static void createPrefs(Scenario tempScenario, String pathToPrefsFile) {
		ObjectAttributes prefs = PrefsCreator.createPrefsBasedOnPlans(tempScenario.getPopulation());
		ObjectAttributesXmlWriter attributesXmlWriterWriter = new ObjectAttributesXmlWriter(prefs);
		attributesXmlWriterWriter.writeFile(pathToPrefsFile);
	}

	public static void createFacilities(Scenario tempScenario, String pathToFacilitiesFile) {
		int facilityId = 0;
		Map<Coord, Id<ActivityFacility>> facilities = new HashMap<>();
		ActivityFacilities activityFacilities = tempScenario.getActivityFacilities();
		for (Person person : tempScenario.getPopulation().getPersons().values()) {
			if (person.getSelectedPlan() != null) {
				List<PlanElement> plan = person.getSelectedPlan().getPlanElements();
				for (PlanElement planElement : plan) {
					if (planElement instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) planElement;
						if (!facilities.containsKey(act.getCoord())) {
							ActivityFacility activityFacility =
									activityFacilities.getFactory().createActivityFacility(Id.create(facilityId++, ActivityFacility.class), act.getCoord());
							activityFacilities.addActivityFacility(activityFacility);
							facilities.put(act.getCoord(), activityFacility.getId());
						}
						ActivityFacility activityFacility = activityFacilities.getFacilities().get(facilities.get(act.getCoord()));
						if (!activityFacility.getActivityOptions().containsKey(act.getType())) {
							ActivityOption activityOption =
									activityFacilities.getFactory().createActivityOption(act.getType());
							if (!act.getType().equals("home")) {
								activityOption.addOpeningTime(new OpeningTimeImpl(21600, act.getEndTime()));
							}
							activityFacility.addActivityOption(activityOption);
						}
						act.setFacilityId(activityFacility.getId());
					}
				}
			}
		}
		new FacilitiesWriter(tempScenario.getActivityFacilities()).write(pathToFacilitiesFile);
	}

}
