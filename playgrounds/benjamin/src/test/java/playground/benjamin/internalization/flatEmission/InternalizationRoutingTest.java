/* *********************************************************************** *
 * project: org.matsim.*
 * RunInternalizationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.internalization.flatEmission;

import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

/**
 * @author benjamin
 *
 */
public class InternalizationRoutingTest extends MatsimTestCase{
	private static final Logger logger = Logger.getLogger(InternalizationRoutingTest.class);

	static boolean isUsingDetailedEmissionCalculation = false;

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private Vehicles emissionVehicles;
	private EmissionModule emissionModule;
	private EmissionCostModule emissionCostModule;

	public void testDistanceRouting() {
		this.config = super.loadConfig(null);
		this.scenario = ScenarioUtils.createScenario(this.config);

		createNetwork();
		createActiveAgents();
		createVehicles();

		this.controler = new Controler(this.scenario);
		specifyControler();

		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setPerforming_utils_hr(0.0);
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.0);
		pcs.setMarginalUtilityOfMoney(1.0);
		double monetaryDistanceRateCar = -0.001;
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

		//link 11 distance
		int expectedRoad = 11;
		final InternalizationRoutingTestHandler handler = new InternalizationRoutingTestHandler(expectedRoad) ;

		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(handler);
			}
		};

		this.controler.addControlerListener(startupListener);
		this.controler.run();
		logger.info("Person is driving on route " + handler.actualRoadSelected + "; expected route: " + expectedRoad);
		assertTrue("Person was expected to be routed through link "+ expectedRoad +", but was " + handler.getActualRoadSelected(), handler.expectedRoadSelected() == true);
	}
	
	public void testTimeRouting() {
		this.config = super.loadConfig(null);
		this.scenario = ScenarioUtils.createScenario(this.config);

		createNetwork();
		createActiveAgents();
		createVehicles();

		this.controler = new Controler(this.scenario);
		specifyControler();

		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setPerforming_utils_hr(0.0);
		final double traveling = -6.0;
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		pcs.setMarginalUtilityOfMoney(1.0);
		double monetaryDistanceRateCar = -0.0;
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

		//link 9 time
		int expectedRoad = 9;
		final InternalizationRoutingTestHandler handler = new InternalizationRoutingTestHandler(expectedRoad) ;

		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(handler);
			}
		};

		this.controler.addControlerListener(startupListener);
		this.controler.run();
		logger.info("Person is driving on route " + handler.actualRoadSelected + "; expected route: " + expectedRoad);
		assertTrue("Person was expected to be routed through link "+ expectedRoad +", but was " + handler.getActualRoadSelected(), handler.expectedRoadSelected() == true);
	}

	public void testTimeDistanceRouting() {
		this.config = super.loadConfig(null);
		this.scenario = ScenarioUtils.createScenario(this.config);

		createNetwork();
		createActiveAgents();
		createVehicles();

		this.controler = new Controler(this.scenario);
		specifyControler();

		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setPerforming_utils_hr(0.0);
		final double traveling = -6.0;
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		pcs.setMarginalUtilityOfMoney(1.0);
		double monetaryDistanceRateCar = -0.001;
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

		//link 13 time AND distance
		int expectedRoad = 13;
		final InternalizationRoutingTestHandler handler = new InternalizationRoutingTestHandler(expectedRoad) ;

		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(handler);
			}
		};

		this.controler.addControlerListener(startupListener);
		this.controler.run();
		logger.info("Person is driving on route " + handler.actualRoadSelected + "; expected route: " + expectedRoad);
		assertTrue("Person was expected to be routed through link "+ expectedRoad +", but was " + handler.getActualRoadSelected(), handler.expectedRoadSelected() == true);
	}

	public void testTimeDistanceEmissionRouting() {
		this.config = super.loadConfig(null);
		this.scenario = ScenarioUtils.createScenario(this.config);

		createNetwork();
		createActiveAgents();
		createVehicles();

		this.controler = new Controler(this.scenario);
		specifyControler();

		emissionModule = new EmissionModule(scenario, this.emissionVehicles);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		emissionCostModule = new EmissionCostModule(100.0);

		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setPerforming_utils_hr(0.0);
		final double traveling = -6.0;
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		pcs.setMarginalUtilityOfMoney(1.0);
		double monetaryDistanceRateCar = -0.001;
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

		installEmissionDisutilityCalculatorFactory();
		installEmissionInternalizationListener();

		//link 14 time AND distance AND emissions
		int expectedRoad = 14;
		final InternalizationRoutingTestHandler handler = new InternalizationRoutingTestHandler(expectedRoad) ;

		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(handler);
			}
		};

		this.controler.addControlerListener(startupListener);
		this.controler.run();
		logger.info("Person is driving on route " + handler.actualRoadSelected + "; expected route: " + expectedRoad);
		assertTrue("Person was expected to be routed through link " + expectedRoad + ", but was " + handler.getActualRoadSelected(), handler.expectedRoadSelected() == true);
	}

	private void installEmissionInternalizationListener() {
		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
	}

	private void installEmissionDisutilityCalculatorFactory() {
		final EmissionTravelDisutilityCalculatorFactory emissiondcf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, 
				emissionCostModule, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emissiondcf);
			}
		});
	}

	private void specifyControler() {
		// services settings
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);

        // controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setFirstIteration(0);
		ccg.setLastIteration(0);
		ccg.setMobsim("qsim");
		Set<EventsFileFormat> set = new HashSet<EventsFileFormat>();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
		ccg.setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);

		// qsimConfigGroup
		QSimConfigGroup qcg = config.qsim();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(1);
		qcg.setStorageCapFactor(1);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);

		// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();

		ActivityParams act1Params = new ActivityParams("home");
		act1Params.setTypicalDuration(16 * 3600);
		pcs.addActivityParams(act1Params);

		ActivityParams act2Params = new ActivityParams("work");
		act2Params.setTypicalDuration(8 * 3600);
		pcs.addActivityParams(act2Params);

		pcs.setBrainExpBeta(1.0);

		// strategyConfigGroup
		//		StrategyConfigGroup scg = services.getConfig().strategy();
		//
		//		StrategySettings changePlan = new StrategySettings(Id.create("1", StrategySettings.class));
		//		changePlan.setStrategyName("BestScore");
		////		changePlan.setStrategyName("ChangeExpBeta");
		//		changePlan.setWeight(0.0);
		//
		//		StrategySettings reRoute = new StrategySettings(Id.create("2", StrategySettings.class));
		//		reRoute.setStrategyName("ReRoute");
		//		reRoute.setWeight(1.0);
		//
		//		scg.addStrategySettings(changePlan);
		//		scg.addStrategySettings(reRoute);

		// define emission tool input files	
//
		// since same files are used for multiple test, files are added to ONE MORE level up then the test package directory
		String packageInputDir = this.getPackageInputDirectory();
		String inputFilesDir = packageInputDir.substring(0, packageInputDir.lastIndexOf('/') );
		inputFilesDir = inputFilesDir.substring(0, inputFilesDir.lastIndexOf('/') + 1);
//
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
		controler.getConfig().addModule(ecg);
		ecg.setEmissionRoadTypeMappingFile(inputFilesDir + "/roadTypeMapping.txt");
		ecg.setAverageWarmEmissionFactorsFile(inputFilesDir + "/EFA_HOT_vehcat_2005average.txt");
		ecg.setAverageColdEmissionFactorsFile(inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setUsingVehicleTypeIdAsVehicleDescription(true);

		// TODO: the following does not work yet. Need to force services to always write events in the last iteration.
		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
		vcg.setWritingOutputEvents(false) ;
	}

	private void createPassiveAgents() {
		PopulationFactory pFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		Id<Link> homeLinkId = Id.create("11", Link.class);
		for(Integer i=0; i<10; i++){
			Person person = pFactory.createPerson(Id.create(i.toString(), Person.class));
			Plan plan = pFactory.createPlan();

			Activity home = pFactory.createActivityFromLinkId("home", homeLinkId);
			home.setEndTime(6 * 3600);
			Leg leg = pFactory.createLeg(TransportMode.walk);
			Activity home2 = pFactory.createActivityFromLinkId("home", homeLinkId);

			plan.addActivity(home);
			plan.addLeg(leg);
			plan.addActivity(home2);
			scenario.getPopulation().addPerson(person);
		}
	}

	private void createActiveAgents() {
		PopulationFactory pFactory = scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(Id.create("worker", Person.class));
		Plan plan = pFactory.createPlan();

		Activity home = pFactory.createActivityFromLinkId("home", Id.create("1", Link.class));
		home.setEndTime(6 * 3600);
		plan.addActivity(home);

		Leg leg1 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg1);

		Activity work = pFactory.createActivityFromLinkId("work", Id.create("4", Link.class));
		work.setEndTime(home.getEndTime() + 600 + 8 * 3600);
		plan.addActivity(work);

		Leg leg2 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg2);

		home = pFactory.createActivityFromLinkId("home", Id.create("1", Link.class));
		plan.addActivity(home);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private void createVehicles() {
		this.emissionVehicles = VehicleUtils.createVehiclesContainer();
		Id<VehicleType> vehTypeId = Id.create(HbefaVehicleCategory.PASSENGER_CAR.toString(), VehicleType.class);
		VehicleType vehicleType = this.emissionVehicles.getFactory().createVehicleType(vehTypeId);
		this.emissionVehicles.addVehicleType(vehicleType);
		for(Person person : scenario.getPopulation().getPersons().values()){
			Vehicle vehicle = this.emissionVehicles.getFactory().createVehicle(Id.create(person.getId(), Vehicle.class), vehicleType);
			this.emissionVehicles.addVehicle(vehicle);
		}
	}

	private void createNetwork() {
		Network network = (Network) scenario.getNetwork();

		double x8 = -20000.0;
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x8, 0.0));
		double x7 = -17500.0;
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(x7, 0.0));
		double x6 = -15500.0;
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(x6, 0.0));
		double x5 = -2500.0;
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(x5, 0.0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(0.0, 0.0));
		double x4 = -5000.0;
		double y3 = -8660.0;
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(x4, y3));
		double x3 = -15000.0;
		double y2 = -8660.0;
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create("7", Node.class), new Coord(x3, y2));
		double x2 = -7500.0;
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create("8", Node.class), new Coord(x2, 2500.0));
		double x1 = -7500.0;
		double y1 = -2500.0;
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create("9", Node.class), new Coord(x1, y1));
		double x = -7500.0;
		double y = -5000.0;
		Node node10 = NetworkUtils.createAndAddNode(network, Id.create("10", Node.class), new Coord(x, y));
		final Node fromNode = node1;
		final Node toNode = node2;


		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 2000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode2 = node4;
		final Node toNode2 = node5;
		//      network.createAndAddLink(Id.create("3", Link.class), node3, node4, 75000, 10.42, 3600, 1, null, "22");
		NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode2, toNode2, (double) 2000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode3 = node5;
		final Node toNode3 = node6;
		NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode3, toNode3, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode4 = node6;
		final Node toNode4 = node7;
		NetworkUtils.createAndAddLink(network,Id.create("6", Link.class), fromNode4, toNode4, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode5 = node7;
		final Node toNode5 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("7", Link.class), fromNode5, toNode5, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode6 = node3;
		final Node toNode6 = node8;
		NetworkUtils.createAndAddLink(network,Id.create("8", Link.class), fromNode6, toNode6, (double) 5000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode7 = node3;
		final Node toNode7 = node9;
		NetworkUtils.createAndAddLink(network,Id.create("10", Link.class), fromNode7, toNode7, (double) 5000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode8 = node3;
		final Node toNode8 = node10;
		NetworkUtils.createAndAddLink(network,Id.create("12", Link.class), fromNode8, toNode8, (double) 5000, 27.78, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode9 = node8;
		final Node toNode9 = node4;

		//link 9 time, link 11 distance, link 13 time AND distance, link 14 time AND distance AND emissions
		NetworkUtils.createAndAddLink(network,Id.create("9", Link.class), fromNode9, toNode9, (double) 5000, 50.00, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode10 = node9;
		final Node toNode10 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("11", Link.class), fromNode10, toNode10, (double) 2500, 10.00, (double) 3600, (double) 1, null, (String) "22");
		final Node fromNode11 = node10;
		final Node toNode11 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("13", Link.class), fromNode11, toNode11, (double) 2600, 21.67, (double) 3600, (double) 1, null, (String) "85");
		final Node fromNode12 = node10;
		final Node toNode12 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("14", Link.class), fromNode12, toNode12, (double) 2600, 21.65, (double) 3600, (double) 1, null, (String) "22");
	}
}
