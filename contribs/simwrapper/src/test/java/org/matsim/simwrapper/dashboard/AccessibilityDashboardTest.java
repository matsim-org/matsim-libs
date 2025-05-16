package org.matsim.simwrapper.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class AccessibilityDashboardTest {


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void kelheimDrt() {


		//CONFIG
		Config config = DrtTestScenario.loadConfig(utils);
		config.controller().setLastIteration(1);
		config.controller().setWritePlansInterval(1);
		config.controller().setWriteEventsInterval(1);

		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);

		//simwrapper
		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.sampleSize = 0.001;
		group.defaultParams().mapCenter = "11.891000, 48.911000";

		//drt
		//we have 2 operators ('av' + 'drt'), configure one of them to be areaBased (the other remains stopBased)
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		DrtConfigGroup drtConfigGroup = multiModeDrtConfigGroup.getModalElements().stream().filter(x -> x.mode.equals("drt")).findFirst().get();
		config.removeModule(MultiModeDrtConfigGroup.GROUP_NAME);
		MultiModeDrtConfigGroup multiModeDrtConfigGroup2 = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		multiModeDrtConfigGroup2.addParameterSet(drtConfigGroup);
//
//		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
//			if (drtCfg.getMode().equals("av")){
//				drtCfg.operationalScheme = DrtConfigGroup.OperationalScheme.serviceAreaBased;
//				drtCfg.drtServiceAreaShapeFile = "drt-zones/drt-zonal-system.shp";
//			}
//		}

		//accessibility

		double mapCenterX = 712144.17;
		double mapCenterY = 5422153.87;

		double tileSize = 100;
		double num_rows = 45;

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxLeft(mapCenterX - num_rows * tileSize - tileSize / 2);
		acg.setBoundingBoxRight(mapCenterX + num_rows * tileSize + tileSize / 2);
		acg.setBoundingBoxBottom(mapCenterY - num_rows * tileSize - tileSize / 2);
		acg.setBoundingBoxTop(mapCenterY + num_rows * tileSize + tileSize / 2);
		acg.setTileSize_m((int) tileSize);


		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.freespeed, Modes4Accessibility.car, Modes4Accessibility.estimatedDrt);

		for(Modes4Accessibility mode : accModes) {
			acg.setComputingAccessibilityForMode(mode, true);
		}
		acg.setUseParallelization(false);

		// CONTROLLER
		SimWrapper sw = SimWrapper.create(config).addDashboard(new AccessibilityDashboard(config.global().getCoordinateSystem(), List.of("trainStation", "cityCenter"), accModes));

		Controler controler = MATSimApplication.prepare(new DrtTestScenario(config), config);

		ActivityFacilitiesFactory af = controler.getScenario().getActivityFacilities().getFactory();
		// train station
		double trainStationX = 715041.71;
		double trainStationY = 5420617.28;
		ActivityFacility fac1 = af.createActivityFacility(Id.create("xxx", ActivityFacility.class), new Coord(trainStationX, trainStationY));
		ActivityOption ao = af.createActivityOption("trainStation");
		fac1.addActivityOption(ao);
		controler.getScenario().getActivityFacilities().addActivityFacility(fac1);

		// innenstadt
		double cityCenterX = 711144.17;
		double cityCenterY = 5422153.87;
		ActivityFacility fac2 = af.createActivityFacility(Id.create("yyy", ActivityFacility.class), new Coord(cityCenterX, cityCenterY));
		ActivityOption ao2 = af.createActivityOption("cityCenter");
		fac2.addActivityOption(ao2);
		controler.getScenario().getActivityFacilities().addActivityFacility(fac2);

		controler.addOverridingModule(new SimWrapperModule(sw));
		controler.run();

	}

//	@Test
//	void accessibilityLessSymmetricTestNetwork(){
//
//
//		// CONFIG
//
//		// general
//		final Config config = ConfigUtils.createConfig();
//
//		config.controller().setLastIteration(1);
//		config.controller().setOutputDirectory(utils.getOutputDirectory());
//		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//		config.routing().setRoutingRandomness(0.);
//		config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings().setStrategyName("ChangeExpBeta").setWeight(1.));
//		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("dummy").setTypicalDuration(60));
//
//		// acc config
//		double min = 0.; // Values for bounding box usually come from a config file
//		double max = 200.;
//
//		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
//		acg.setTileSize_m(100);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//		acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
//		acg.setBoundingBoxBottom(min).setBoundingBoxTop(max ).setBoundingBoxLeft(min ).setBoundingBoxRight(max );
//		acg.setUseParallelization(false);
//
//		// simwrapper config
//		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
//
//		// SCENARIO
//		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
//
//		//create network
//		{
//		/*
//		 * (2)		(5)------(8)
//		 * 	|		 |
//		 * 	|		 |
//		 * (1)------(4)------(7)
//		 * 	|		 |
//		 * 	|		 |
//		 * (3)		(6)------(9)
//		 */
//			double freespeed = 2.7;
//			double capacity = 500.;
//			double numLanes = 1.;
//
//
//			Network network = scenario.getNetwork();
//
//			// Nodes
//			Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 100));
//			Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 10, (double) 190));
//			Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 10, (double) 10));
//			Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 120, (double) 100));
//			Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 120, (double) 190));
//			Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 120, (double) 10));
//			Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 190, (double) 100));
//			Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 190, (double) 190));
//			Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 190, (double) 10));
//
//			Set<String> modes = new HashSet<>();
//			modes.add("car");
//
//			// Links (bi-directional)
//			NetworkUtils.createAndAddLink(network, Id.create(1, Link.class), node1, node2, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(1, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(2, Link.class), node2, node1, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(2, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(3, Link.class), node1, node3, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(3, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(4, Link.class), node3, node1, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(4, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(5, Link.class), node1, node4, (double) 110, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(5, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(6, Link.class), node4, node1, (double) 110, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(6, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(7, Link.class), node4, node5, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(7, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(8, Link.class), node5, node4, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(8, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(9, Link.class), node4, node6, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(9, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(10, Link.class), node6, node4, (double) 90, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(10, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(11, Link.class), node4, node7, (double) 70, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(11, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(12, Link.class), node7, node4, (double) 70, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(12, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(13, Link.class), node5, node8, (double) 70, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(13, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(14, Link.class), node8, node5, (double) 70, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(14, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(15, Link.class), node6, node9, (double) 70, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(15, Link.class)).setAllowedModes(modes);
//
//			NetworkUtils.createAndAddLink(network, Id.create(16, Link.class), node9, node6, (double) 70, freespeed, capacity, numLanes);
//			network.getLinks().get(Id.create(16, Link.class)).setAllowedModes(modes);
//
//			scenario.setNetwork(network);
//		}
//
//		//create population
//		{
//			Random rnd = new Random();
//			PopulationFactory pf = scenario.getPopulation().getFactory();
//			for (int i = 0; i < 1000; i++) {
//				Person person = pf.createPerson(Id.createPersonId(i));
//				Plan plan = pf.createPlan();
//				Activity home = pf.createActivityFromCoord("dummy", new Coord(rnd.nextInt(200), rnd.nextInt(200)));
//				home.setEndTime(10 * 60 * 60);
//				Leg leg = pf.createLeg(TransportMode.car);
//				Activity work = pf.createActivityFromCoord("dummy", new Coord(rnd.nextInt(200), rnd.nextInt(200)));
//				plan.addActivity(home);
//				plan.addLeg(leg);
//				plan.addActivity(work);
//				person.addPlan(plan);
//				scenario.getPopulation().addPerson(person);
//			}
//		}
//
//		// ---
//		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link
//
//		ActivityOption supermarketActivityOption = scenario.getActivityFacilities().getFactory().createActivityOption("supermarket");
//		final ActivityFacilities opportunities = scenario.getActivityFacilities();
//		ActivityFacility facility1 = opportunities.getFactory().createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(200, 0));
//		facility1.addActivityOption(supermarketActivityOption);
//		opportunities.addActivityFacility(facility1);
//		ActivityFacility facility2 = opportunities.getFactory().createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(200, 200));
//		facility2.addActivityOption(supermarketActivityOption);
//		opportunities.addActivityFacility(facility2);
//		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
//
//		// ---
//
//		SimWrapper sw = SimWrapper.create(config).addDashboard(new AccessibilityDashboard(config.global().getCoordinateSystem()));
//
//		Controler controler = new Controler(scenario);
//		controler.addOverridingModule(new SimWrapperModule(sw));
//
//		controler.run();
//
//
//	}



}
