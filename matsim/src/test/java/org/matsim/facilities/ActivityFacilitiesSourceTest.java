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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

/**
 * Created by amit on 05.02.18.
 */

@RunWith(Parameterized.class)
public class ActivityFacilitiesSourceTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	private final String mode = TransportMode.car;
//    private static final String outDir = "test/output/"+ActivityFacilitiesSourceTest.class.getCanonicalName().replace('.','/')+"/";

	private final FacilitiesConfigGroup.FacilitiesSource facilitiesSource;
	private final boolean facilitiesWithCoordOnly ;

	public ActivityFacilitiesSourceTest(FacilitiesConfigGroup.FacilitiesSource facilitiesSource, boolean facilitiesWithCoordOnly) {
		this.facilitiesSource = facilitiesSource;
		this.facilitiesWithCoordOnly = facilitiesWithCoordOnly;
	}

	@Parameterized.Parameters(name = "{index}: FacilitiesSource - {0}; facilitiesWithCoordOnly - {1}")
	public static Collection<Object[]> parametersForFacilitiesSourceTest() {
		// it is not clear to me why/how this works.  The documentation of JUnitParamsRunner says that such
		// implicit behavior ("dirty tricks") should no longer be necessary when using it.  kai, jul'18
		return Arrays.asList(
				new Object[]{FacilitiesConfigGroup.FacilitiesSource.none, true} // true/false doen't matter here
				,new Object[]{FacilitiesConfigGroup.FacilitiesSource.fromFile, true}// true/false doen't matter here
				,new Object[]{FacilitiesConfigGroup.FacilitiesSource.setInScenario, true}
				,new Object[]{FacilitiesConfigGroup.FacilitiesSource.setInScenario, false}
				,new Object[]{FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile, true} // true/false doen't matter here
				,new Object[]{FacilitiesConfigGroup.FacilitiesSource.onePerActivityLocationInPlansFile, true} // true/false doen't matter here
		);
	}

	@Test
	public void test(){
		String outDir = utils.getOutputDirectory() ;
		String testOutDir = outDir + "/" + facilitiesSource.toString() + "_facilitiesWithCoordOnly_" + String
				.valueOf(facilitiesWithCoordOnly) + "/";
		new File(testOutDir).mkdirs();

		Scenario scenario = prepareScenario();
		scenario.getConfig().controler().setOutputDirectory(testOutDir);
		scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		new Controler(scenario).run();

		// checks
		ActivityFacilities activityFacilities = getFacilities(scenario.getConfig().controler().getOutputDirectory());
		switch (this.facilitiesSource) {
			case none:
				break;
			case fromFile:
				for (ActivityFacility af : activityFacilities.getFacilities().values()){
					Assert.assertNull(af.getLinkId());
				}
				break;
			case setInScenario:
				Assert.assertEquals("wrong number of facilities", 2, activityFacilities.getFacilities().size(), MatsimTestUtils.EPSILON);
				if (facilitiesWithCoordOnly) {
					for (ActivityFacility af : activityFacilities.getFacilities().values()){
						Assert.assertNull(af.getLinkId());
					}
				} else {
					for (ActivityFacility af : activityFacilities.getFacilities().values()){
						Assert.assertNull(af.getCoord());
					}
				}
				break;
			case onePerActivityLinkInPlansFile:
				Assert.assertEquals("wrong number of facilities", 4, getFacilities(scenario.getConfig().controler().getOutputDirectory()).getFacilities().size(), MatsimTestUtils.EPSILON);
				for (ActivityFacility af : activityFacilities.getFacilities().values()){
					Assert.assertNotNull(af.getLinkId());
				}
				break;
			case onePerActivityLocationInPlansFile:
				// TODO fix this
//				Assert.assertEquals("wrong number of facilities", 4, getFacilities(scenario.getConfig().controler().getOutputDirectory()).getFacilities().size(), MatsimTestUtils.EPSILON);
//				for (ActivityFacility af : activityFacilities.getFacilities().values()){
//					Assert.assertNotNull(af.getCoord());
//				}
				break;
		}
	}

	private ActivityFacilities getFacilities(String outputDir){
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		new FacilitiesReaderMatsimV1(scenario).readFile(outputDir+"/output_facilities.xml.gz");
		return scenario.getActivityFacilities();
	}

	
	// create basic scenario
	private Scenario prepareScenario() {
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.plans().setInputFile(null);
		config.controler().setLastIteration(0);
		
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
		
		{ //  activities from coords only
			Person person = populationFactory.createPerson(Id.createPersonId("0"));
			Plan plan = populationFactory.createPlan();
			Activity home = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			home.setEndTime(7 * 3600.0);
			if (assignFacilityIdToActivity(facilitiesSource)) {
				home.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(home);
			plan.addLeg(populationFactory.createLeg(mode));
			Activity work = populationFactory.createActivityFromCoord("w", new Coord(10000.0, 0.0));
			work.setEndTime(16 * 3600.0);
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg(mode));
			Activity lastAct = populationFactory.createActivityFromCoord("h", new Coord(-25000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				lastAct.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(lastAct);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		{ // activity from link only
			Person person = populationFactory.createPerson(Id.createPersonId("1"));
			Plan plan = populationFactory.createPlan();
			Activity home = populationFactory.createActivityFromLinkId("h", Id.createLinkId("1"));
			home.setEndTime(8 * 3600.0);
			if (assignFacilityIdToActivity(facilitiesSource)) {
				home.setFacilityId(Id.create("1", ActivityFacility.class));
			}
			plan.addActivity(home);
			plan.addLeg(populationFactory.createLeg(mode));
			Activity work = populationFactory.createActivityFromLinkId("w", Id.createLinkId("20"));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			work.setEndTime(17.5 * 3600.0);
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg(mode));
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
			plan.addLeg(populationFactory.createLeg(mode));
			Activity work = populationFactory.createActivityFromLinkId("w", Id.createLinkId("20"));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			work.setEndTime(18 * 3600.0);
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg(mode));
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
			plan.addLeg(populationFactory.createLeg(mode));
			Activity work = populationFactory.createActivityFromCoord("w", new Coord(10000.0, 0.0));
			if (assignFacilityIdToActivity(facilitiesSource)) {
				work.setFacilityId(Id.create("2", ActivityFacility.class));
			}
			work.setEndTime(18.5 * 3600.0);
			work.setLinkId(Id.createLinkId("20"));
			plan.addActivity(work);
			plan.addLeg(populationFactory.createLeg(mode));
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
