/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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
package org.matsim.contrib.bicycle.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.utils.eventsfilecomparison.ComparisonResult.FILES_ARE_EQUAL;

/**
 * @author dziemke
 */
public class BicycleTest {
	private static final Logger LOG = LogManager.getLogger(BicycleTest.class);

	private static final String bicycleMode = "bicycle";

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testNormal() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals(FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( eventsFilenameReference, eventsFilenameNew),
				"Different event files.");

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		for (Id<Person> personId : scenarioReference.getPopulation().getPersons().keySet()) {
			double scoreReference = scenarioReference.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			double scoreCurrent = scenarioCurrent.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			Assertions.assertEquals(scoreReference, scoreCurrent, MatsimTestUtils.EPSILON, "Scores of persons " + personId + " are different");
		}
		assertTrue(PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()), "Populations are different");
	}

	@Test
	void testCobblestone() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Links 4-8 and 13-17 have cobblestones
		config.network().setInputFile("network_cobblestone.xml");

		config.plans().setInputFile("population_1200.xml");

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );
		{
			Scenario scenarioReference = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			Scenario scenarioCurrent = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			new PopulationReader( scenarioReference ).readFile( utils.getInputDirectory() + "output_plans.xml.gz" );
			new PopulationReader( scenarioCurrent ).readFile( utils.getOutputDirectory() + "output_plans.xml.gz" );
			assertTrue( PopulationUtils.equalPopulation( scenarioReference.getPopulation(), scenarioCurrent.getPopulation() ), "Populations are different" );
		}
		{
			LOG.info( "Checking MATSim events file ..." );
			final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
			final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
			assertEquals( FILES_ARE_EQUAL,
					new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( eventsFilenameReference, eventsFilenameNew ),
					"Different event files.");
		}
	}

	@Test
	void testPedestrian() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Links 4-8 and 13-17 are pedestrian zones
		config.network().setInputFile("network_pedestrian.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals(FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( eventsFilenameReference, eventsFilenameNew),
				"Different event files.");

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue(PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()), "Populations are different");
	}

	@Test
	void testLane() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Links 2-4/8-10 and 11-13/17-19 have cycle lanes (cycleway=lane)
		config.network().setInputFile("network_lane.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals(FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( eventsFilenameReference, eventsFilenameNew),
				"Different event files.");

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");

		assertEquals(PopulationComparison.Result.equal, PopulationComparison.compare(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()), "Populations are different");
	}

	@Test
	void testGradient() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Nodes 5-8 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		config.network().setInputFile("network_gradient.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals(FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( eventsFilenameReference, eventsFilenameNew),
				"Different event files.");

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue(PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()), "Populations are different");
	}

	@Test
	void testGradientLane() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		// and links 4-5 and 13-14 have cycle lanes
		config.network().setInputFile("network_gradient_lane.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals(FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison(eventsFilenameReference, eventsFilenameNew),
				"Different event files.");

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue(PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()), "Populations are different");
	}

	@Test
	void testNormal10It() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);

		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		// 10 iterations
		config.controller().setLastIteration(10);
		config.controller().setWriteEventsInterval(10);
		config.controller().setWritePlansInterval(10);
		config.controller().setCreateGraphs(false);

		new RunBicycleExample().run(config );

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals(FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison(eventsFilenameReference, eventsFilenameNew),
				"Different event files.");

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue(PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()), "Populations are different");
	}

	@Test
	void testLinkBasedScoring() {
//		{
//			Config config = createConfig( 0 );
//			BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) config.getModules().get( "bicycle" );
//			bicycleConfigGroup.setBicycleScoringType( BicycleScoringType.legBased );
//			new RunBicycleExample().run( config );
//		}
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		{
			Config config2 = createConfig( 0 );
			BicycleConfigGroup bicycleConfigGroup2 = (BicycleConfigGroup) config2.getModules().get( "bicycle" );
//			bicycleConfigGroup2.setBicycleScoringType( BicycleScoringType.linkBased );
			new RunBicycleExample().run( config2 );
		}
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");

//		LOG.info("Checking MATSim events file ...");
//		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
//		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
//		assertEquals("Different event files.", FILES_ARE_EQUAL,
//				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison(eventsFilenameReference, eventsFilenameNew));

		for (Id<Person> personId : scenarioReference.getPopulation().getPersons().keySet()) {
			double scoreReference = scenarioReference.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			double scoreCurrent = scenarioCurrent.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			Assertions.assertEquals(scoreReference, scoreCurrent, MatsimTestUtils.EPSILON, "Scores of persons " + personId + " are different");
		}
//		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}

	@Test
	void testLinkVsLegMotorizedScoring() {
		// --- withOUT additional car traffic:
//		{
//			Config config2 = createConfig( 0 );
//			BicycleConfigGroup bicycleConfigGroup2 = ConfigUtils.addOrGetModule( config2, BicycleConfigGroup.class );
////			bicycleConfigGroup2.setBicycleScoringType( BicycleScoringType.linkBased );
//			bicycleConfigGroup2.setMotorizedInteraction( false );
//			new RunBicycleExample().run( config2 );
//		}
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		// ---
		// --- WITH additional car traffic:
		{
			Config config = createConfig( 0 );
			BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
//			bicycleConfigGroup.setBicycleScoringType( BicycleScoringType.legBased );
			bicycleConfigGroup.setMotorizedInteraction( true );

			// the following comes from inlining RunBicycleExample, which we need since we need to modify scenario data:
			config.global().setNumberOfThreads(1 );
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists );

			config.routing().setRoutingRandomness(3. );

			final String bicycle = bicycleConfigGroup.getBicycleMode();

			Scenario scenario = ScenarioUtils.loadScenario( config );

			for( Link link : scenario.getNetwork().getLinks().values() ){
				link.setAllowedModes( CollectionUtils.stringArrayToSet( new String[]{ bicycleMode, TransportMode.car}) );
			}

			// add car traffic:
			{
				PopulationFactory pf = scenario.getPopulation().getFactory();
				List<Person> newPersons = new ArrayList<>();
				for( Person oldPerson : scenario.getPopulation().getPersons().values() ){
					Person newPerson = pf.createPerson( Id.createPersonId( oldPerson.getId() + "_car" ) );
					Plan newPlan = pf.createPlan();
					PopulationUtils.copyFromTo( oldPerson.getSelectedPlan(), newPlan );
					for( Leg leg : TripStructureUtils.getLegs( newPlan ) ){
						leg.setMode( TransportMode.car );
					}
					newPerson.addPlan( newPlan );
					newPersons.add( newPerson );
				}
				for( Person newPerson : newPersons ){
					scenario.getPopulation().addPerson( newPerson );
				}
			}

			// go again back to RunBicycleExample material:

			// set config such that the mode vehicles come from vehicles data:
			scenario.getConfig().qsim().setVehiclesSource( VehiclesSource.modeVehicleTypesFromVehiclesData );

			// now put hte mode vehicles into the vehicles data:
			final VehiclesFactory vf = VehicleUtils.getFactory();
			scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) ) );
			scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create( bicycle, VehicleType.class ) )
								 .setNetworkMode( bicycle ).setMaximumVelocity(4.16666666 ).setPcuEquivalents(0.25 ) );

			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new BicycleModule() );

			controler.run();
		}
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		// ---
		// ---

//		LOG.info("Checking MATSim events file ...");
//		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
//		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
//		assertEquals("Different event files.", FILES_ARE_EQUAL,
//				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison(eventsFilenameReference, eventsFilenameNew));

		for (Id<Person> personId : scenarioReference.getPopulation().getPersons().keySet()) {
			double scoreReference = scenarioReference.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			double scoreCurrent = scenarioCurrent.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			Assertions.assertEquals(scoreReference, scoreCurrent, MatsimTestUtils.EPSILON, "Scores of person=" + personId + " are different");
		}
//		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}

//	@Test public void testMotorizedInteraction() {
////		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
//		Config config = createConfig( 10 );
//
//		// Activate link-based scoring
//		BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) config.getModules().get("bicycle");
////		bicycleConfigGroup.setBicycleScoringType(BicycleScoringType.linkBased);
//
//		// Interaction with motor vehicles
//		new RunBicycleExample().run(config );
//
//		LOG.info("Checking MATSim events file ...");
//		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
//		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
//		assertEquals("Different event files.", FILES_ARE_EQUAL,
//				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison(eventsFilenameReference, eventsFilenameNew));
//
//		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
//		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
//		for (Id<Person> personId : scenarioReference.getPopulation().getPersons().keySet()) {
//			double scoreReference = scenarioReference.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
//			double scoreCurrent = scenarioCurrent.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
//			Assert.assertEquals("Scores of persons " + personId + " are different", scoreReference, scoreCurrent, MatsimTestUtils.EPSILON);
//		}
//		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
//	}

	@Test
	void testInfrastructureSpeedFactor() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		config.addModule(new BicycleConfigGroup());

		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);
		config.qsim().setStartTime(6. * 3600.);
		config.qsim().setEndTime(10. * 3600.);

		List<String> mainModeList = new ArrayList<>();
		mainModeList.add( bicycleMode );
		mainModeList.add(TransportMode.car);
		config.qsim().setMainModes(mainModeList);

		config.replanning().setMaxAgentPlanMemorySize(5);
		{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(1.0);
			config.replanning().addStrategySettings(strategySettings);
		}

		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.scoring().addActivityParams(homeActivity);

		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(8*60*60);
		config.scoring().addActivityParams(workActivity);

		ModeParams bicycle = new ModeParams( bicycleMode );
		bicycle.setConstant(0.);
		bicycle.setMarginalUtilityOfDistance(-0.0004); // util/m
		bicycle.setMarginalUtilityOfTraveling(-6.0); // util/h
		bicycle.setMonetaryDistanceRate(0.);
		config.scoring().addModeParams(bicycle);

		config.routing().setNetworkModes(mainModeList);

		// link 2 has infrastructure speed factor = 1.0, all other links 0.01
		config.network().setInputFile("network_infrastructure-speed-factor.xml");
		config.plans().setInputFile("population_4.xml");

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		config.global().setNumberOfThreads(1);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.routing().setRoutingRandomness(3.);

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config);
		VehiclesFactory vf = scenario.getVehicles().getFactory();

		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) ) );

		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create( bicycleMode, VehicleType.class ) )
							 .setNetworkMode( bicycleMode ).setMaximumVelocity(25.0/3.6 ).setPcuEquivalents(0.25 ) );

		scenario.getConfig().qsim().setVehiclesSource( VehiclesSource.modeVehicleTypesFromVehiclesData );

		// ---

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule() );

		LinkDemandEventHandler linkHandler = new LinkDemandEventHandler();

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(linkHandler);
			}
		});

		controler.run();

		Assertions.assertEquals(3, linkHandler.getLinkId2demand().get(Id.createLinkId("2")), MatsimTestUtils.EPSILON, "All bicycle users should use the longest but fastest route where the bicycle infrastructur speed factor is set to 1.0");
		Assertions.assertEquals(1, linkHandler.getLinkId2demand().get(Id.createLinkId("6")), MatsimTestUtils.EPSILON, "Only the car user should use the shortest route");

		Assertions.assertEquals(1.0 + Math.ceil( 13000 / (25.0 /3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("2")).get(0), MatsimTestUtils.EPSILON, "Wrong travel time (bicycle user)");
		Assertions.assertEquals(1.0 + Math.ceil( 13000 / (25.0 /3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("2")).get(1), MatsimTestUtils.EPSILON, "Wrong travel time (bicycle user)");
		Assertions.assertEquals(1.0 + Math.ceil( 13000 / (25.0 /3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("2")).get(2), MatsimTestUtils.EPSILON, "Wrong travel time (bicycle user)");

		Assertions.assertEquals(Math.ceil( 10000 / (13.88) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(0), MatsimTestUtils.EPSILON, "Wrong travel time (car user)");

	}

	@Test
	void testInfrastructureSpeedFactorDistanceMoreRelevantThanTravelTime() {
		Config config = ConfigUtils.createConfig(utils.getClassInputDirectory() );
		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );

		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);
		config.qsim().setStartTime(6. * 3600.);
		config.qsim().setEndTime(14. * 3600.);

		List<String> mainModeList = new ArrayList<>();
		mainModeList.add("bicycle");
		mainModeList.add(TransportMode.car);
		config.qsim().setMainModes(mainModeList);

		config.replanning().setMaxAgentPlanMemorySize(5);
		{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(1.0);
			config.replanning().addStrategySettings(strategySettings);
		}

		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.scoring().addActivityParams(homeActivity);

		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(8*60*60);
		config.scoring().addActivityParams(workActivity);

		ModeParams bicycle = new ModeParams("bicycle");
		bicycle.setConstant(0.);
		bicycle.setMarginalUtilityOfDistance(-999999); // util/m
		bicycle.setMarginalUtilityOfTraveling(-6.0); // util/h
		bicycle.setMonetaryDistanceRate(0.);
		config.scoring().addModeParams(bicycle);

		config.routing().setNetworkModes(mainModeList);

		// link 2 has infrastructure speed factor = 1.0, all other links 0.01
		config.network().setInputFile("network_infrastructure-speed-factor.xml");
		config.plans().setInputFile("population_4.xml");
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		config.global().setNumberOfThreads(1);

		config.routing().setRoutingRandomness(3.);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		var vf  = scenario.getVehicles().getFactory();

		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class ) ) );

		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create("bicycle", VehicleType.class ) )
				  .setMaximumVelocity(25.0/3.6 ).setPcuEquivalents(0.25 ).setNetworkMode( bicycleConfigGroup.getBicycleMode() ) );

		scenario.getConfig().qsim().setVehiclesSource( VehiclesSource.modeVehicleTypesFromVehiclesData );

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BicycleModule() );

		LinkDemandEventHandler linkHandler = new LinkDemandEventHandler();

		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				this.addEventHandlerBinding().toInstance(linkHandler);
			}
		});

		controler.run();

		Assertions.assertEquals(4, linkHandler.getLinkId2demand().get(Id.createLinkId("6")), MatsimTestUtils.EPSILON, "All bicycle users should use the shortest route even though the bicycle infrastructur speed factor is set to 0.1");
		Assertions.assertEquals(Math.ceil(10000 / 13.88 ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(0), MatsimTestUtils.EPSILON, "Wrong travel time (car user)");
		Assertions.assertEquals(Math.ceil( 10000 / (25. * 0.1 / 3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(1), MatsimTestUtils.EPSILON, "Wrong travel time (bicycle user)");
		Assertions.assertEquals(Math.ceil( 10000 / (25. * 0.1 / 3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(2), MatsimTestUtils.EPSILON, "Wrong travel time (bicycle user)");
		Assertions.assertEquals(Math.ceil( 10000 / (25. * 0.1 / 3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(3), MatsimTestUtils.EPSILON, "Wrong travel time (bicycle user)");
	}

	private Config createConfig( int lastIteration ){
		//		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		Config config = ConfigUtils.createConfig( utils.getClassInputDirectory() );
		config.addModule( new BicycleConfigGroup() );
		RunBicycleExample.fillConfigWithBicycleStandardValues( config );

		// Normal network
		config.network().setInputFile( "network_normal.xml" );
		config.plans().setInputFile( "population_1200.xml" );
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setLastIteration( lastIteration );
		config.controller().setLastIteration( lastIteration );
		config.controller().setWriteEventsInterval( 10 );
		config.controller().setWritePlansInterval( 10 );
		config.controller().setCreateGraphs( false );
		return config;
	}


}

class LinkDemandEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

	private final Map<Id<Link>,Integer> linkId2demand = new HashMap<>();
	private final Map<Id<Link>,List<Double>> linkId2travelTimes = new HashMap<>();

	private final Map<Id<Vehicle>, Double> vehicleId2lastEnterTime = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.linkId2demand.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		if (this.linkId2demand.get(event.getLinkId()) != null) {
			int agents = this.linkId2demand.get(event.getLinkId());
			this.linkId2demand.put(event.getLinkId(), agents + 1);

		} else {
			this.linkId2demand.put(event.getLinkId(), 1);
		}

		if (vehicleId2lastEnterTime.get(event.getVehicleId()) != null) {
			double tt = event.getTime() - vehicleId2lastEnterTime.get(event.getVehicleId());
			linkId2travelTimes.get(event.getLinkId()).add(tt);
		}
	}

	Map<Id<Link>, Integer> getLinkId2demand() {
		return linkId2demand;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		vehicleId2lastEnterTime.put(event.getVehicleId(), event.getTime());

		if (!linkId2travelTimes.containsKey(event.getLinkId())) {
			linkId2travelTimes.put(event.getLinkId(), new ArrayList<>());
		}
	}

	Map<Id<Link>, List<Double>> getLinkId2travelTimes() {
		return linkId2travelTimes;
	}

}
