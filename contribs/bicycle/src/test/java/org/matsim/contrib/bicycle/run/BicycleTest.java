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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleConfigGroup.BicycleScoringType;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculator;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result.FILES_ARE_EQUAL;

/**
 * @author dziemke
 */
public class BicycleTest {
	private static final Logger LOG = Logger.getLogger(BicycleTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testNormal() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		for (Id<Person> personId : scenarioReference.getPopulation().getPersons().keySet()) {
			double scoreReference = scenarioReference.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			double scoreCurrent = scenarioCurrent.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			Assert.assertEquals("Scores of persons " + personId + " are different", scoreReference, scoreCurrent, MatsimTestUtils.EPSILON);
		}
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testCobblestone() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Links 4-8 and 13-17 have cobblestones
		config.network().setInputFile("network_cobblestone.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);
		{
			Scenario scenarioReference = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			Scenario scenarioCurrent = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			new PopulationReader( scenarioReference ).readFile( utils.getInputDirectory() + "output_plans.xml.gz" );
			new PopulationReader( scenarioCurrent ).readFile( utils.getOutputDirectory() + "output_plans.xml.gz" );
			assertTrue( "Populations are different", PopulationUtils.equalPopulation( scenarioReference.getPopulation(), scenarioCurrent.getPopulation() ) );
		}
		{
			LOG.info( "Checking MATSim events file ..." );
			final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
			final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
			assertEquals( "Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare( eventsFilenameReference, eventsFilenameNew ));
		}
	}
		
	@Test
	public void testPedestrian() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Links 4-8 and 13-17 are pedestrian zones
		config.network().setInputFile("network_pedestrian.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testLane() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Links 2-4/8-10 and 11-13/17-19 have cycle lanes (cycleway=lane)
		config.network().setInputFile("network_lane.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testGradient() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		config.network().setInputFile("network_gradient.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testGradientLane() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		// and links 4-5 and 13-14 have cycle lanes
		config.network().setInputFile("network_gradient_lane.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testNormal10It() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		// 10 iterations
		config.controler().setLastIteration(10);
		config.controler().setWriteEventsInterval(10);
		config.controler().setWritePlansInterval(10);
		config.controler().setCreateGraphs(false);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testMotorizedInteraction() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(10);
		config.controler().setLastIteration(10);
		config.controler().setWriteEventsInterval(10);
		config.controler().setWritePlansInterval(10);
		config.controler().setCreateGraphs(false);
		
		// Activate link-based scoring
		BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) config.getModules().get("bicycle");
		bicycleConfigGroup.setBicycleScoringType(BicycleScoringType.linkBased);
		
		// Interaction with motor vehicles
		new RunBicycleExample().run(config, true);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testInfrastructureSpeedFactor() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());

		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);
		config.qsim().setStartTime(6. * 3600.);
		config.qsim().setEndTime(10. * 3600.);
		
		List<String> mainModeList = new ArrayList<>();
		mainModeList.add("bicycle");
		mainModeList.add(TransportMode.car);
		config.qsim().setMainModes(mainModeList);
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(1.0);
			config.strategy().addStrategySettings(strategySettings);
		}
		
		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
		
		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		ModeParams bicycle = new ModeParams("bicycle");
		bicycle.setConstant(0.);
		bicycle.setMarginalUtilityOfDistance(-0.0004); // util/m
		bicycle.setMarginalUtilityOfTraveling(-6.0); // util/h
		bicycle.setMonetaryDistanceRate(0.);
		config.planCalcScore().addModeParams(bicycle);
		
		config.plansCalcRoute().setNetworkModes(mainModeList);
				
		// link 2 has infrastructure speed factor = 1.0, all other links 0.01
		config.network().setInputFile("network_infrastructure-speed-factor.xml");
		config.plans().setInputFile("population_4.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		config.global().setNumberOfThreads(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plansCalcRoute().setRoutingRandomness(3.);
				
		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		scenario.getVehicles().addVehicleType(car);

		VehicleType bicycleVehType = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
		bicycleVehType.setMaximumVelocity(25.0/3.6);
		bicycleVehType.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bicycleVehType);

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		Controler controler = new Controler(scenario);
		BicycleModule bicycleModule = new BicycleModule(scenario);
		controler.addOverridingModule(bicycleModule);
		
		LinkDemandEventHandler linkHandler = new LinkDemandEventHandler();

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(linkHandler);
			}
		});
		
		controler.addOverridingQSimModule(new AbstractQSimModule() {

            @Override
            protected void configureQSim() {
                bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
                    @Inject
                    private EventsManager events;

                    @Override
                    public QNetworkFactory get() {
                        final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
                        factory.setLinkSpeedCalculator(new BicycleLinkSpeedCalculator(scenario));
                        return factory;
                    }
                });
            }
        });
		//TODO fix
		controler.run();
		
		Assert.assertEquals("All bicycle users should use the longest but fastest route where the bicycle infrastructur speed factor is set to 1.0", 3, linkHandler.getLinkId2demand().get(Id.createLinkId("2")), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Only the car user should use the shortest route", 1, linkHandler.getLinkId2demand().get(Id.createLinkId("6")), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong travel time (car user)", Math.ceil( 10000 / (13.88) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(0), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong travel time (bicycle user)", 1.0 + Math.ceil( 13000 / (25.0 * 1.0/3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("2")).get(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time (bicycle user)", 1.0 + Math.ceil( 13000 / (25.0 * 1.0/3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("2")).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time (bicycle user)", 1.0 + Math.ceil( 13000 / (25.0 * 1.0/3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("2")).get(2), MatsimTestUtils.EPSILON);

	}
	
	@Test
	public void testInfrastructureSpeedFactorDistanceMoreRelevantThanTravelTime() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());

		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);
		config.qsim().setStartTime(6. * 3600.);
		config.qsim().setEndTime(14. * 3600.);
		
		List<String> mainModeList = new ArrayList<>();
		mainModeList.add("bicycle");
		mainModeList.add(TransportMode.car);
		config.qsim().setMainModes(mainModeList);
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(1.0);
			config.strategy().addStrategySettings(strategySettings);
		}
		
		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
		
		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		ModeParams bicycle = new ModeParams("bicycle");
		bicycle.setConstant(0.);
		bicycle.setMarginalUtilityOfDistance(-999999); // util/m
		bicycle.setMarginalUtilityOfTraveling(-6.0); // util/h
		bicycle.setMonetaryDistanceRate(0.);
		config.planCalcScore().addModeParams(bicycle);
		
		config.plansCalcRoute().setNetworkModes(mainModeList);
				
		// link 2 has infrastructure speed factor = 1.0, all other links 0.01
		config.network().setInputFile("network_infrastructure-speed-factor.xml");
		config.plans().setInputFile("population_4.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		config.global().setNumberOfThreads(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plansCalcRoute().setRoutingRandomness(3.);
				
		Scenario scenario = ScenarioUtils.loadScenario(config);

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		scenario.getVehicles().addVehicleType(car);

		VehicleType bicycleVehType = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
		bicycleVehType.setMaximumVelocity(25.0/3.6);
		bicycleVehType.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bicycleVehType);

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		Controler controler = new Controler(scenario);
		BicycleModule bicycleModule = new BicycleModule(scenario);
		controler.addOverridingModule(bicycleModule);
		
		LinkDemandEventHandler linkHandler = new LinkDemandEventHandler();

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(linkHandler);
			}
		});
		
/*		controler.addOverridingQSimModule(new AbstractQSimModule() {

            @Override
            protected void configureQSim() {
                bind(QNetworkFactory.class).toProvider(new Provider<QNetworkFactory>() {
                    @Inject
                    private EventsManager events;

                    @Override
                    public QNetworkFactory get() {
                        final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
                        factory.setLinkSpeedCalculator(new BicycleLinkSpeedCalculator(scenario));
                        return factory;
                    }
                });
            }
        });
		*/
//TODO fix
		controler.run();
		
		Assert.assertEquals("All bicycle users should use the shortest route even though the bicycle infrastructur speed factor is set to 0.1", 4, linkHandler.getLinkId2demand().get(Id.createLinkId("6")), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time (car user)", Math.ceil(10000 / 13.88 ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time (bicycle user)", Math.ceil( 10000 / (25. * 0.1 / 3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time (bicycle user)", Math.ceil( 10000 / (25. * 0.1 / 3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong travel time (bicycle user)", Math.ceil( 10000 / (25. * 0.1 / 3.6) ), linkHandler.getLinkId2travelTimes().get(Id.createLinkId("6")).get(3), MatsimTestUtils.EPSILON);
	}
}

class LinkDemandEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {
	
	private Map<Id<Link>,Integer> linkId2demand = new HashMap<>();
	private Map<Id<Link>,List<Double>> linkId2travelTimes = new HashMap<>();
	
	private Map<Id<Vehicle>, Double> vehicleId2lastEnterTime = new HashMap<>();

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

	public Map<Id<Link>, Integer> getLinkId2demand() {
		return linkId2demand;
	}

	public void setLinkId2demand(Map<Id<Link>, Integer> linkId2demand) {
		this.linkId2demand = linkId2demand;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		vehicleId2lastEnterTime.put(event.getVehicleId(), event.getTime());
		
		if (linkId2travelTimes.get(event.getLinkId()) == null) {
			linkId2travelTimes.put(event.getLinkId(), new ArrayList<>());
		}
	}

	public Map<Id<Link>, List<Double>> getLinkId2travelTimes() {
		return linkId2travelTimes;
	}
	
}
