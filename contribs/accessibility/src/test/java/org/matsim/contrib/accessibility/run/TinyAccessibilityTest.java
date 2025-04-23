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

package org.matsim.contrib.accessibility.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.google.inject.multibindings.MapBinder;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.accessibility.*;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.*;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * A small test that enables to easily compare results with hand-computed results.
 *
 * @author dziemke
 */
public class TinyAccessibilityTest {

	private static final Logger LOG = LogManager.getLogger(TinyAccessibilityTest.class);

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runFromEvents() {
		final Config config = createTestConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min).setBoundingBoxTop(max ).setBoundingBoxLeft(min ).setBoundingBoxRight(max );
		acg.setUseParallelization(false);

		// ---

		final Scenario scenario = createTestScenario(config);

		// ---

		final String eventsFile = utils.getClassInputDirectory() + "output_events.xml.gz";

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , eventsFile );
		builder.addDataListener( new ResultsComparator() );
		builder.build().run() ;

	}
	@Test
	void fakeTestToProduceOutput() {

		createCongestedEventsFile("output_events_test.xml.gz", 10 * 60 * 60);

	}


	void createCongestedEventsFile(String congestedFileName, double congestionTime){
		final Config config = createTestConfig();
		config.controller().setLastIteration(1);
		config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings().setStrategyName("ChangeExpBeta").setWeight(1.));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("dummy").setTypicalDuration(60));


		final Scenario scenario = createTestScenario(config);

		// ---

		Random rnd = new Random();
		PopulationFactory pf = scenario.getPopulation().getFactory();
		for (int i = 0; i < 1000; i++) {
			Person person = pf.createPerson(Id.createPersonId(i));
			Plan plan = pf.createPlan();
			Activity home = pf.createActivityFromCoord("dummy", new Coord(rnd.nextInt(200), rnd.nextInt(200)));
			home.setEndTime(congestionTime);
			Leg leg = pf.createLeg(TransportMode.car);
			Activity work = pf.createActivityFromCoord("dummy", new Coord(rnd.nextInt(200), rnd.nextInt(200)));
			plan.addActivity(home);
			plan.addLeg(leg);
			plan.addActivity(work);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}


		Controler controler = new Controler(scenario);
		controler.run();

		try {
			Files.copy(Path.of(utils.getOutputDirectory() + "output_events.xml.gz"),Path.of(congestedFileName), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
	@Test
	void runFromEventsCongested() {

		String congestedFileName = utils.getClassInputDirectory() + "output_events_congested.xml.gz";
		double congestionTime = 10 * 60 * 60;

		//The following line creates runs one iteration with 1000 agents, created a congested network at the above time.
		// This only has to be run once
		if (!Files.exists(Path.of(congestedFileName))) {
			createCongestedEventsFile(congestedFileName, congestionTime);
		}

		final Config config = createTestConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min).setBoundingBoxTop(max).setBoundingBoxLeft(min).setBoundingBoxRight(max);
		acg.setTimeOfDay(congestionTime);
		acg.setUseParallelization(false);

		// ---

		final Scenario scenario = createTestScenario(config);

		// ---

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , congestedFileName );
//		builder.addDataListener( new ResultsComparator() );
		builder.build().run() ;

	}

	@Test
	public void runFromEventsDrt() throws IOException {
		final Config config = createTestConfig();

		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
//		drtParams.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(drtParams);

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min ).setBoundingBoxTop(max ).setBoundingBoxLeft(min ).setBoundingBoxRight(max );
		acg.setUseParallelization(false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true);


		String stopsInputFileName = utils.getClassInputDirectory() + "drtStops.xml";

		if (!Files.exists(Path.of(stopsInputFileName))) {
			createDrtStopsFile(stopsInputFileName);
		}


		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
		drtConfigGroup.transitStopFile = stopsInputFileName;

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 200;


		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);

		// ---

		final Scenario scenario = createTestScenario(config);

		// ---

		final String eventsFile = utils.getClassInputDirectory() + "output_events.xml.gz";

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , eventsFile );
		builder.addDataListener( new ResultsComparator() );
		builder.build().run() ;

	}

	@Test
	public void runFromEventsDrtCongested() throws IOException {
		String stopsInputFileName = utils.getClassInputDirectory() + "drtStops.xml";
		if (!Files.exists(Path.of(stopsInputFileName))) {
			createDrtStopsFile(stopsInputFileName);
		}

		String congestedFileName = utils.getClassInputDirectory() + "output_events_congested.xml.gz";
		double congestionTime = 10 * 60 * 60;

		if (!Files.exists(Path.of(congestedFileName))) {
			createCongestedEventsFile(congestedFileName, congestionTime);
		}

		final Config config = createTestConfig();

		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
//		drtParams.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(drtParams);

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min).setBoundingBoxTop(max).setBoundingBoxLeft(min).setBoundingBoxRight(max);
		acg.setUseParallelization(false);
		acg.setTimeOfDay(congestionTime);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true);


		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
		drtConfigGroup.transitStopFile = stopsInputFileName;

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 200;


		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);

		// ---

		final Scenario scenario = createTestScenario(config);

		// ---


		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , congestedFileName );
//		builder.addDataListener( new ResultsComparator() );
		builder.build().run() ;

	}


	@Test
	public void runFromEventsDrtCongestedKelheim() throws IOException {
		String stopsInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/drt-stops-land.xml";

		String eventsFile = "/Users/jakob/git/matsim-kelheim/output/output-kelheim-v3.1-1pct/kelheim-v3.1-1pct.output_events.xml.gz";

		double accMeasTime = 12 * 60 * 60.;

		final Config config = createTestConfig();

		config.global().setCoordinateSystem("EPSG:25832");

		config.network().setInputFile("/Users/jakob/git/matsim-kelheim/output/output-kelheim-v3.1-1pct/kelheim-v3.1-1pct.output_network.xml.gz");

		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
		drtParams.setMarginalUtilityOfDistance(-2.5E-4);
		drtParams.setMarginalUtilityOfTraveling(0.0);

		config.scoring().addModeParams(drtParams);

		ScoringConfigGroup.ModeParams walkParams = config.scoring().getModes().get(TransportMode.walk);
		walkParams.setMarginalUtilityOfTraveling(0.0);
		config.scoring().addModeParams(walkParams);

		double mapCenterX = 721455;
		double mapCenterY = 5410601;

		double tileSize = 200;
		double num_rows = 0.25;//50;

		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		accConfig.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
		accConfig.setBoundingBoxLeft(mapCenterX - num_rows*tileSize - tileSize/2);
		accConfig.setBoundingBoxRight(mapCenterX + num_rows*tileSize + tileSize/2);
		accConfig.setBoundingBoxBottom(mapCenterY - num_rows*tileSize - tileSize/2);
		accConfig.setBoundingBoxTop(mapCenterY + num_rows*tileSize + tileSize/2);
		accConfig.setTileSize_m((int) tileSize);

		accConfig.setTimeOfDay(accMeasTime);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, true);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);


		ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
		drtConfigGroup.transitStopFile = stopsInputFile;

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 200;


		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);

		// ---

		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		String filePath = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kelheim/kelheim-drt-accessibility-JB-master/input/pois_complete.csv";
		readPoiCsv(scenario.getActivityFacilities(), filePath);


		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, List.of("train_station"));

		ConfigUtils.writeConfig(config, utils.getOutputDirectory() + "config.xml");

		builder.build().run();

	}

	private static void readPoiCsv(ActivityFacilities activityFacilities, String filePath) {

		ActivityFacilitiesFactory af = activityFacilities.getFactory();
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) new URL(filePath).openConnection();
			connection.setRequestMethod("GET");
		} catch (ProtocolException | MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (CSVParser parser = new CSVParser(new BufferedReader(new InputStreamReader(connection.getInputStream())),

			CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {

				String id = record.get("id");
				double x = Double.parseDouble(record.get("x"));
				double y = Double.parseDouble(record.get("y"));
				String type = record.get("type");
				ActivityFacility fac = af.createActivityFacility(Id.create(id, ActivityFacility.class), new Coord(x, y));
				ActivityOption ao = af.createActivityOption(type);
				fac.addActivityOption(ao);
				activityFacilities.addActivityFacility(fac);


			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void createDrtStopsFile(String stopsInputFileName) throws IOException {
		Scenario dummyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleFactory tf = dummyScenario.getTransitSchedule().getFactory();
		TransitStopFacility a = tf.createTransitStopFacility(Id.create("a", TransitStopFacility.class), new Coord(120, 100), false);
		a.setLinkId(Id.createLinkId("7"));
		dummyScenario.getTransitSchedule().addStopFacility(a);


		if (!Files.exists(Path.of(utils.getInputDirectory()))) {
			Files.createDirectories(Path.of(utils.getInputDirectory()));
		}

		new TransitScheduleWriter(dummyScenario.getTransitSchedule()).writeFile(stopsInputFileName);
	}

	@Test
	void testWithBoundingBox() {
		final Config config = createTestConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min);
		acg.setBoundingBoxTop(max);
		acg.setBoundingBoxLeft(min);
		acg.setBoundingBoxRight(max);
		acg.setUseParallelization(false);

		final Scenario sc = createTestScenario(config);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator();
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);

		controler.run();
	}


	private Config createTestConfig() {
		final Config config = ConfigUtils.createConfig();

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTileSize_m(100);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.routing().setRoutingRandomness(0.);

		return config;
	}


	private static Scenario createTestScenario(final Config config) {
//		final Scenario scenario = ScenarioUtils.loadScenario(config);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		Network network = createLessSymmetricTestNetwork();
		scenario.setNetwork(network);

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		ActivityFacility facility1 = opportunities.getFactory().createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(200, 0));
		opportunities.addActivityFacility(facility1);
		ActivityFacility facility2 = opportunities.getFactory().createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(200, 200));
		opportunities.addActivityFacility(facility2);
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
		return scenario;
	}


	/**
	 * This method creates a test network. It is used for example in PtMatrixTest.java to test the pt simulation in MATSim.
	 * The network has 9 nodes and 8 links (see the sketch below).
	 *
	 * @return the created test network
	 *
	 * @author thomas
	 * @author tthunig
	 */
	public static Network createLessSymmetricTestNetwork() {
		/*
		 * (2)		(5)------(8)
		 * 	|		 |
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)		(6)------(9)
		 */
		double freespeed = 2.7;
		double capacity = 500.;
		double numLanes = 1.;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = (Network) scenario.getNetwork();

		// Nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 100));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 10, (double) 190));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 10, (double) 10));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 120, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 120, (double) 190));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 120, (double) 10));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 190, (double) 100));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 190, (double) 190));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 190, (double) 10));

		Set<String> modes = new HashSet<>();
		modes.add("car");

		// Links (bi-directional)
		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), node1, node2, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(1, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), node2, node1, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(2, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), node1, node3, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(3, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), node3, node1, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(4, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(5, Link.class), node1, node4, (double) 110, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(5, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(6, Link.class), node4, node1, (double) 110, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(6, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(7, Link.class), node4, node5, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(7, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(8, Link.class), node5, node4, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(8, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(9, Link.class), node4, node6, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(9, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), node6, node4, (double) 90, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(10, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), node4, node7, (double) 70, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(11, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), node7, node4, (double) 70, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(12, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), node5, node8, (double) 70, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(13, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(14, Link.class), node8, node5, (double) 70, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(14, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(15, Link.class), node6, node9, (double) 70, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(15, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(16, Link.class), node9, node6, (double) 70, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(16, Link.class)).setAllowedModes(modes);

		return network;
	}


	static class ResultsComparator implements FacilityDataExchangeInterface{
		private Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>() ;

		@Override
		public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, String mode, double accessibility) {
			Tuple<ActivityFacility, Double> key = new Tuple<>(measurePoint, timeOfDay);
			if (!accessibilitiesMap.containsKey(key)) {
				Map<String,Double> accessibilitiesByMode = new HashMap<>();
				accessibilitiesMap.put(key, accessibilitiesByMode);
			}
			accessibilitiesMap.get(key).put(mode, accessibility);
		}

		@Override
		public void finish() {
			for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
				LOG.warn("CHECK X = " + tuple.getFirst().getCoord().getX() + " -- Y = " + tuple.getFirst().getCoord().getY() + " -- freespeed value = " + accessibilitiesMap.get(tuple).get("freespeed"));
				LOG.warn("CHECK X = " + tuple.getFirst().getCoord().getX() + " -- Y = " + tuple.getFirst().getCoord().getY() + " -- car value = " + accessibilitiesMap.get(tuple).get(TransportMode.car));
				if (tuple.getFirst().getCoord().getX() == 50.) {
					if (tuple.getFirst().getCoord().getY() == 50.) {
						Assertions.assertEquals(-0.017248522428805767, accessibilitiesMap.get(tuple).get("freespeed"), MatsimTestUtils.EPSILON);
						Assertions.assertEquals(-0.017240250823867296, accessibilitiesMap.get(tuple).get(TransportMode.car), MatsimTestUtils.EPSILON);
					} else if (tuple.getFirst().getCoord().getY() == 150.) {
						Assertions.assertEquals(-0.017248522428805767, accessibilitiesMap.get(tuple).get("freespeed"), MatsimTestUtils.EPSILON);
						Assertions.assertEquals(-0.017240250823867296, accessibilitiesMap.get(tuple).get(TransportMode.car), MatsimTestUtils.EPSILON);
					}
				}
				if (tuple.getFirst().getCoord().getX() == 150.) {
					if (tuple.getFirst().getCoord().getY() == 50.) {
						Assertions.assertEquals(0.2758252376673665, accessibilitiesMap.get(tuple).get("freespeed"), MatsimTestUtils.EPSILON);
						Assertions.assertEquals(0.27582980607476704, accessibilitiesMap.get(tuple).get(TransportMode.car), MatsimTestUtils.EPSILON);
					} else if (tuple.getFirst().getCoord().getY() == 150.) {
						Assertions.assertEquals(0.2758252376673665, accessibilitiesMap.get(tuple).get("freespeed"), MatsimTestUtils.EPSILON);
						Assertions.assertEquals(0.27582980607476704, accessibilitiesMap.get(tuple).get(TransportMode.car), MatsimTestUtils.EPSILON);
					}
				}
			}
		}
	}
	static class FinderBridge extends AbstractDvrpModeModule {
		FinderBridge( String mode ){
			super( mode );
		}
		@Override public void install(){
			// we bind modal material using the modal binders.  but how do we get it back?  The ModalProviders have methods getModalInstance(...), which return what we need ...
			// ... however, they take it out of the injector, which means that we really need to use providers, since their getters are called _after_ the injector is created.

			MapBinder<String, DvrpRoutingModule.AccessEgressFacilityFinder> mapBinder = MapBinder.newMapBinder( binder(), String.class,
					DvrpRoutingModule.AccessEgressFacilityFinder.class );

			mapBinder.addBinding( getMode() ).toProvider( this.modalProvider( getter -> getter.getModal( DvrpRoutingModule.AccessEgressFacilityFinder.class ) ) );
			// (I think that this works as follows:
			// * getter.getModal(...) takes whatever is needed out of the injector.
			// * however, the provider that is bound to the mapBinder is not activated until this is really needed.
			// I think.  kai, oct'24)

		}
	}
}
