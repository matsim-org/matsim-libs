/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.stream.Stream;

/**
 * Created by amit on 05.02.18.
 */

public class ActivityFacilitiesSourceTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	public static Stream<Arguments> arguments() {
		// it is not clear to me why/how this works.  The documentation of JUnitParamsRunner says that such
		// implicit behavior ("dirty tricks") should no longer be necessary when using it.  kai, jul'18
		return Stream.of(
				Arguments.of(FacilitiesConfigGroup.FacilitiesSource.none, true), // true/false doen't matter here
				Arguments.of(FacilitiesConfigGroup.FacilitiesSource.fromFile, true),// true/false doen't matter here
				Arguments.of(FacilitiesConfigGroup.FacilitiesSource.setInScenario, true),
				Arguments.of(FacilitiesConfigGroup.FacilitiesSource.setInScenario, false),
			 	Arguments.of(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile, true), // true/false doen't matter here
			 	Arguments.of(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile, true) // true/false doen't matter here
		);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void test(FacilitiesConfigGroup.FacilitiesSource facilitiesSource, boolean facilitiesWithCoordOnly){
		String outDir = utils.getOutputDirectory() ;
		String testOutDir = outDir + "/" + facilitiesSource.toString() + "_facilitiesWithCoordOnly_" + String.valueOf(facilitiesWithCoordOnly) + "/";
		new File(testOutDir).mkdirs();

		Scenario scenario = prepareScenario(facilitiesSource, facilitiesWithCoordOnly);
		scenario.getConfig().controller().setOutputDirectory(testOutDir);
		scenario.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		// (overwriteExistingFiles is needed here for the parameterized test since otherwise all output directories except for
		// the last test will be deleted and thus not available for debugging. kai, sep'19)
		new Controler(scenario).run();

		// checks
		ActivityFacilities activityFacilities = getFacilities(scenario.getConfig().controller().getOutputDirectory());
		switch (facilitiesSource) {
			case none:
				break;
			case fromFile:
				for (ActivityFacility af : activityFacilities.getFacilities().values()){
					Assertions.assertNotNull(af.getLinkId());
				}
				break;
			case setInScenario:
				Assertions.assertEquals(2, activityFacilities.getFacilities().size(), MatsimTestUtils.EPSILON, "wrong number of facilities");
				if (facilitiesWithCoordOnly) {
					for (ActivityFacility af : activityFacilities.getFacilities().values()){
						Assertions.assertNotNull(af.getLinkId());
					}
				} else {
					for (ActivityFacility af : activityFacilities.getFacilities().values()){
						Assertions.assertNull(af.getCoord());
					}
				}
				break;
			case onePerActivityLinkInPlansFile:
				Assertions.assertEquals(4, getFacilities(scenario.getConfig().controller().getOutputDirectory()).getFacilities().size(), MatsimTestUtils.EPSILON, "wrong number of facilities");
				for (ActivityFacility af : activityFacilities.getFacilities().values()){
					Assertions.assertNotNull(af.getLinkId());
				}
				break;
			case onePerActivityLocationInPlansFile:
				Assertions.assertEquals(2, getFacilities(scenario.getConfig().controller().getOutputDirectory()).getFacilities().size(), MatsimTestUtils.EPSILON, "wrong number of facilities");
				for (ActivityFacility af : activityFacilities.getFacilities().values()){
					Assertions.assertNotNull(af.getCoord());
					Assertions.assertNotNull(af.getLinkId());
				}
				break;
		}
	}

	private ActivityFacilities getFacilities(String outputDir){
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		new FacilitiesReaderMatsimV1(null, null, scenario.getActivityFacilities()).readFile(outputDir+"/output_facilities.xml.gz");
		return scenario.getActivityFacilities();
	}


	// create basic scenario
	private Scenario prepareScenario(FacilitiesConfigGroup.FacilitiesSource facilitiesSource, boolean facilitiesWithCoordOnly) {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.plans().setInputFile(null);
		config.controller().setLastIteration(0);

		switch (facilitiesSource) {
			case fromFile:
				break;
			case none:
			case setInScenario:
				config.facilities().setInputFile(null); // remove facility file if exists
			case onePerActivityLocationInPlansFile:
				config.facilities().setInputFile(null); // remove facility file if exists
//				config.facilities().setAssigningLinksToFacilitiesIfMissing(false);
				break;
			case onePerActivityLinkInPlansFile:
				config.facilities().setInputFile(null); // remove facility file if exists
//				config.facilities().setAssigningLinksToFacilitiesIfMissing(false);
				break;
		}

		config.facilities().setFacilitiesSource(facilitiesSource);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (facilitiesSource.equals(FacilitiesConfigGroup.FacilitiesSource.setInScenario)) {
			ActivityFacilities facilities = scenario.getActivityFacilities();
			ActivityFacilitiesFactory factory = facilities.getFactory();
			if (facilitiesWithCoordOnly) {
				facilities.addActivityFacility(factory.createActivityFacility(Id.create("1", ActivityFacility.class),
						new Coord(-25000.0, 0.),
						null));
				facilities.addActivityFacility(factory.createActivityFacility(Id.create("2", ActivityFacility.class),
						new Coord(10000.0, 0.0),
						null));
			} else {
				facilities.addActivityFacility(factory.createActivityFacility(Id.create("1", ActivityFacility.class),
						null,
						Id.createLinkId("1")));
				facilities.addActivityFacility(factory.createActivityFacility(Id.create("2", ActivityFacility.class),
						null,
						Id.createLinkId("20")));
			}
		}

		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		String mode = TransportMode.car;
		{ //  activities from coords only
			Person person = populationFactory.createPerson(Id.createPersonId("0"));
			Plan plan = populationFactory.createPlan();
			Activity home = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			home.setEndTime(7 * 3600.0);
			if (assignFacilityIdToActivity(facilitiesSource)) {
				home.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(home);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity work = populationFactory.createActivityFromCoord("w", new Coord(10000.0, 0.0));
			work.setEndTime(16 * 3600.0);
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity lastAct = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				lastAct.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(lastAct);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}

		// activity from link only
		if (! facilitiesSource.equals(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile)){
			Person person = populationFactory.createPerson(Id.createPersonId("1"));
			Plan plan = populationFactory.createPlan();
			Activity home = populationFactory.createActivityFromLinkId("h", Id.createLinkId("1"));
			home.setEndTime(8 * 3600.0);
			if (assignFacilityIdToActivity(facilitiesSource)) {
				home.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(home);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity work = populationFactory.createActivityFromLinkId("w", Id.createLinkId("20"));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			work.setEndTime(17.5 * 3600.0);
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity lastAct = populationFactory.createActivityFromLinkId("h", Id.createLinkId("1"));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				lastAct.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(lastAct);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		{ //  one activity from coord another with link
			Person person = populationFactory.createPerson(Id.createPersonId("2"));
			Plan plan = populationFactory.createPlan();
			Activity home = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				home.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			home.setEndTime(7.8 * 3600.0);
			plan.addActivity(home);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity work = populationFactory.createActivityFromLinkId("w", Id.createLinkId("20"));

			if (facilitiesSource.equals(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile)){
				work.setCoord(new Coord(10000.0, 0.0));
			}

			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			work.setEndTime(18 * 3600.0);
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity lastAct = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				lastAct.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(lastAct);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		{ // an activity from coord and link both
			Person person = populationFactory.createPerson(Id.createPersonId("3"));
			Plan plan = populationFactory.createPlan();
			Activity home = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				home.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			home.setEndTime(9 * 3600.0);
			home.setLinkId(Id.createLinkId("1"));
			plan.addActivity(home);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity work = populationFactory.createActivityFromCoord("w", new Coord(10000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			work.setEndTime(18.5 * 3600.0);
			work.setLinkId(Id.createLinkId("20"));
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg( mode ) );
			Activity lastAct = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				lastAct.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(lastAct);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		return scenario;
	}

	private boolean assignFacilityIdToActivity(FacilitiesConfigGroup.FacilitiesSource facilitiesSource) {
		return facilitiesSource.equals(FacilitiesConfigGroup.FacilitiesSource.setInScenario) || facilitiesSource.equals(
				FacilitiesConfigGroup.FacilitiesSource.fromFile);
	}
}
