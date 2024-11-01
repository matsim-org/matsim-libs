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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.*;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.testcases.MatsimTestUtils;

/**
 * I can't say how similar or different to {@link AccessibilityIntegrationTest} this one here is.  kai, feb'17
 *
 * @author nagel, dziemke
 */
public class AccessibilityIntegrationTest {

	private static final Logger LOG = LogManager.getLogger(AccessibilityIntegrationTest.class);

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled
	@Test
	void testRunAccessibilityExample() {
		Config config = ConfigUtils.loadConfig("./examples/RunAccessibilityExample/config.xml");

		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		org.matsim.contrib.accessibility.run.RunAccessibilityExample.run(scenario);

		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName("./output/work/accessibilities.csv");
		tabFileParserConfig.setDelimiterRegex(",");
        new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
        	double x, y, value;

            public void startRow(String[] row) {
            	if (row.length == 3) {
	            	x = Double.parseDouble(row[0]);
	            	x = Double.parseDouble(row[1]);
	            	value = Double.parseDouble(row[2]);

	            	if (x == 50) {
	            		if (y == 50) {
	            			Assertions.assertEquals(2.1486094237531126, value, utils.EPSILON, "Wrong work accessibility value at x=" + x + ", y=" + y + ":");
	            		} else if (y == 150){
	            			Assertions.assertEquals(2.1766435716006005, value, utils.EPSILON, "Wrong work accessibility value at x=" + x + ", y=" + y + ":");
	            		}
	            	} else if (x == 150) {
	            		if (y == 50) {
	            			Assertions.assertEquals(2.1486094237531126, value, utils.EPSILON, "Wrong work accessibility value at x=" + x + ", y=" + y + ":");
	            		} else if (y == 150){
	            			Assertions.assertEquals(2.2055702759681273, value, utils.EPSILON, "Wrong work accessibility value at x=" + x + ", y=" + y + ":");
	            		}
	            	}
            	}
            }
        });
	}


	@Test
	void testWithBoundingBoxConfigFile() {
		Config config = ConfigUtils.loadConfig(utils.getInputDirectory() + "config.xml");

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		// acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		// acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.matrixBasedPt, false);
		acg.setUseParallelization(false);

		ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
		config.scoring().addModeParams(ptParams);

		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class) ;

		config.routing().setRoutingRandomness(0.);

		final Scenario sc = ScenarioUtils.loadScenario(config);
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator(false);
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();
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

		config.routing().setRoutingRandomness(0.);

		final Scenario sc = createTestScenario(config);

		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator(false);
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();
	}


	@Test
	void testWithBoundingBoxUsingOpportunityWeights() {
		final Config config = createTestConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min);
		acg.setBoundingBoxTop(max);
		acg.setBoundingBoxLeft(min);
		acg.setBoundingBoxRight(max);

		acg.setUseOpportunityWeights(true);
		acg.setWeightExponent(2.);

		config.routing().setRoutingRandomness(0.);

		final Scenario sc = createTestScenarioUsingOpportunityWeights(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator(true);
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();
	}


	@Test
	void testWithExtentDeterminedByNetwork() {
		final Config config = createTestConfig() ;

		config.routing().setRoutingRandomness(0.);

		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator(false);
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();
	}


	@Test
	void testWithExtentDeterminedShapeFile() {
		Config config = createTestConfig() ;

		File f = new File(this.utils.getInputDirectory() + "shapefile.shp"); // shape file completely covers the road network

		if(!f.exists()){
			LOG.error("Shape file not found! testWithExtentDeterminedShapeFile could not be tested...");
			Assertions.assertTrue(f.exists());
		}

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTileSize_m(100);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
		//		acg.setShapeFileCellBasedAccessibility(url.getPath()); // yyyyyy todo
		acg.setShapeFileCellBasedAccessibility(f.getAbsolutePath());

		config.routing().setRoutingRandomness(0.);

		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator(false);
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();
	}


	@Test
	void testWithPredefinedMeasuringPoints() {
		Config config = createTestConfig() ;

		File f = new File(this.utils.getInputDirectory() + "measuringPoints.xml");

		if(!f.exists()){
			LOG.error("Facilities file with measuring points not found! testWithMeasuringPointsInFacilitiesFile could not be performed...");
			Assertions.assertTrue(f.exists());
		}

		Scenario measuringPointsSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(measuringPointsSc).readFile(f.getAbsolutePath());
		ActivityFacilities measuringPoints = (ActivityFacilities) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(measuringPointsSc, null);

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);

		acg.setTileSize_m(100);

		acg.setEnvelope(new Envelope(0, 200, 0, 200));

		acg.setMeasuringPointsFacilities(measuringPoints);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromFacilitiesObject);

		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.routing(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		config.routing().setRoutingRandomness(0.);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator(false);
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();
	}


	@Disabled
	@Test
	void testWithFile(){
		/*TODO Complete - JWJ, Dec'16 */
		Config config = createTestConfig();

		File f = new File(this.utils.getInputDirectory() + "pointFile.csv");
		if(!f.exists()){
			LOG.error("Point file not found! testWithFile could not be tested...");
			Assertions.assertTrue(f.exists());
		}

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromFacilitiesFile);
		acg.setMeasuringPointsFile(f.getAbsolutePath());

		final Scenario sc = createTestScenario(config);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
//		module.addSpatialGridDataExchangeListener( new EvaluateTestResults(true,true,true,true,true) ) ;
		controler.addOverridingModule(module);

		controler.run();

		/* FIXME This currently does NOTHING... it completely ignores the
		 * file-based instruction.  (presumably JWJ, dec'16)
		 *
		 * This is now in principle working; I fixed at least one bug.  But pointFile.csv is empty. --> disabling the test.  kai, feb'17
		 */
	}


	private Config createTestConfig() {
		final Config config = ConfigUtils.createConfig();

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTileSize_m(100);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.matrixBasedPt, false);
		acg.setUseParallelization(false);

		// modify config according to needs
		Network network = createTestNetwork(); // this is a little odd. kai, dec'16
		String networkFile = utils.getOutputDirectory() + "network.xml";
		new NetworkWriter(network).write(networkFile);
		config.network().setInputFile(networkFile);

		config.transit().setUseTransit(true);
//		config.transit().setTransitScheduleFile(utils.getClassInputDirectory() + "schedule.xml");
		config.transit().setTransitScheduleFile(utils.getClassInputDirectory() + "schedule2.xml");
		config.transit().setVehiclesFile(utils.getClassInputDirectory() + "vehicles.xml");

		ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
		config.scoring().addModeParams(ptParams);

		MatrixBasedPtRouterConfigGroup mbConfig = new MatrixBasedPtRouterConfigGroup();
		mbConfig.setPtStopsInputFile(utils.getClassInputDirectory() + "ptStops.csv");
		mbConfig.setPtTravelDistancesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setPtTravelTimesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setUsingPtStops(true);
		mbConfig.setUsingTravelTimesAndDistances(true);
		config.addModule(mbConfig);

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}


	private static Scenario createTestScenario(final Config config) {
		final Scenario scenario = ScenarioUtils.loadScenario(config);
//		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
//		Network network = createTestNetwork();
//		network.getAttributes().putAttribute("coordinateReferenceSystem", TransformationFactory.ATLANTIS);
//		scenario.setNetwork(network);

		for( Link link : scenario.getNetwork().getLinks().values() ){
			Set<String> modes = new HashSet<>( link.getAllowedModes() ) ;
			modes.add( TransportMode.walk ) ;
			modes.add( TransportMode.bike ) ;
			link.setAllowedModes( modes );
		}

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(Id.create(link.getId(), ActivityFacility.class), link.getCoord());
			System.err.println("facility = " + facility.toString() + " -- " + facility.getCoord());
			opportunities.addActivityFacility(facility);
		}
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
		return scenario;
	}


	private static Scenario createTestScenarioUsingOpportunityWeights(final Config config) {
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		for( Link link : scenario.getNetwork().getLinks().values() ){
			Set<String> modes = new HashSet<>( link.getAllowedModes() ) ;
			modes.add( TransportMode.walk ) ;
			modes.add( TransportMode.bike ) ;
			link.setAllowedModes( modes );
		}

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link; with a weight
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(Id.create(link.getId(), ActivityFacility.class), link.getCoord());
			facility.getAttributes().putAttribute( Labels.WEIGHT, 2. );
			opportunities.addActivityFacility(facility);
		}
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
	public static Network createTestNetwork() {
		/*
		 * (2)		(5)------(8)
		 * 	|		 |
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)		(6)------(9)
		 */
		// TODO 2.7m/s does obviously not correspond to 50km/h; changing this will alter results, dz, july'17
		double freespeed = 2.7;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = (Network) scenario.getNetwork();

		// Nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 100));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 0, (double) 200));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 0, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 100, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 100, (double) 200));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 100, (double) 0));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 200, (double) 100));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 200, (double) 200));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 200, (double) 0));

		Set<String> modes = new HashSet<>();
		modes.add("car");

		// Links (bi-directional)
		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), node1, node2, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(1, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), node2, node1, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(2, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), node1, node3, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(3, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), node3, node1, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(4, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(5, Link.class), node1, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(5, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(6, Link.class), node4, node1, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(6, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(7, Link.class), node4, node5, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(7, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(8, Link.class), node5, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(8, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(9, Link.class), node4, node6, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(9, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), node6, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(10, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), node4, node7, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(11, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), node7, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(12, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), node5, node8, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(13, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(14, Link.class), node8, node5, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(14, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(15, Link.class), node6, node9, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(15, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(16, Link.class), node9, node6, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(16, Link.class)).setAllowedModes(modes);

		return network;
	}


	static class ResultsComparator implements FacilityDataExchangeInterface{
		private Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>();

		private boolean useOpportunityWeights = false;

		public ResultsComparator(boolean useOpportunityWeights){
			this.useOpportunityWeights = useOpportunityWeights;
		}

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
				if (!useOpportunityWeights) {
					if (tuple.getFirst().getCoord().getX() == 50.) {
						if (tuple.getFirst().getCoord().getY() == 50.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) { // commented values are before (a) Marcel's change of the QuadTree in Oct'18, (b) change in TravelTimeCalculator in Apr'21
									case "freespeed": Assertions.assertEquals(2.207441799716032, value, MatsimTestUtils.EPSILON); break; // (a) 2.1486094237531126
									case TransportMode.car: Assertions.assertEquals(2.2058369602991204, value, MatsimTestUtils.EPSILON); break; // (a) 2.1482840466191093  (b) 2.205836861444427
									case TransportMode.bike: Assertions.assertEquals(2.2645288908389554, value, MatsimTestUtils.EPSILON); break; // (a) 2.2257398663221
									case TransportMode.walk: Assertions.assertEquals(1.8697283849051263, value, MatsimTestUtils.EPSILON); break; // (a) 1.70054725728361
									case TransportMode.pt: Assertions.assertEquals(2.1581641260040683, value, MatsimTestUtils.EPSILON); break;
									case "matrixBasedPt": Assertions.assertEquals(1.6542905235735796, value, MatsimTestUtils.EPSILON); break; // (a) 0.461863556339195
								}
							}
						}
						else if (tuple.getFirst().getCoord().getY() == 150.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(2.207441799716032, value, MatsimTestUtils.EPSILON); break; // (a) 2.1766435716006005
									case TransportMode.car: Assertions.assertEquals(2.207441960299121, value, MatsimTestUtils.EPSILON); break; // (a) 2.176238564675181  (b) 2.207441799716032
									case TransportMode.bike: Assertions.assertEquals(2.2645288908389554, value, MatsimTestUtils.EPSILON); break; // (a) 2.2445468698643367
									case TransportMode.walk: Assertions.assertEquals(1.8697283849051263, value, MatsimTestUtils.EPSILON); break; // (a) 1.7719146868026079
									case TransportMode.pt: Assertions.assertEquals(2.1581641260040683, value, MatsimTestUtils.EPSILON); break; // (a) 2.057596373646452
									case "matrixBasedPt": Assertions.assertEquals(1.6542905235735796, value, MatsimTestUtils.EPSILON); break; // (a) 0.461863556339195
								}
							}
						}
					}
					if (tuple.getFirst().getCoord().getX() == 150.) {
						if (tuple.getFirst().getCoord().getY() == 50.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(2.235503385314382, value, MatsimTestUtils.EPSILON); break; // (a) 2.1486094237531126
									case TransportMode.car: Assertions.assertEquals(2.23550352057971, value, MatsimTestUtils.EPSILON); break; // (a) 2.1482840466191093  (b) 2.235503385314382
									case TransportMode.bike: Assertions.assertEquals(2.2833435568892395, value, MatsimTestUtils.EPSILON); break; // (a) 2.2257398663221
									case TransportMode.walk: Assertions.assertEquals(1.9418539664691532, value, MatsimTestUtils.EPSILON); break; // (a) 1.70054725728361
									case TransportMode.pt: Assertions.assertEquals(2.0032465393091434, value, MatsimTestUtils.EPSILON); break;
									case "matrixBasedPt": Assertions.assertEquals(1.6542905235735796, value, MatsimTestUtils.EPSILON); break; // (a) 0.461863556339195
								}
							}
						}
						else if (tuple.getFirst().getCoord().getY() == 150.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(2.235503385314382, value, MatsimTestUtils.EPSILON); break; // (a) 2.2055702759681273
									case TransportMode.car: Assertions.assertEquals(2.23550352057971, value, MatsimTestUtils.EPSILON); break; // (a) 2.2052225231109226  (b) 2.235503385314382
									case TransportMode.bike: Assertions.assertEquals(2.2833435568892395, value, MatsimTestUtils.EPSILON); break; // (a) 2.2637376515333636
									case TransportMode.walk: Assertions.assertEquals(1.9418539664691532, value, MatsimTestUtils.EPSILON); break; // (a) 1.851165291193725
									case TransportMode.pt: Assertions.assertEquals(2.0032465393091434, value, MatsimTestUtils.EPSILON); break; // (a) 1.9202710265495115
									case "matrixBasedPt": Assertions.assertEquals(1.5073890466447624, value, MatsimTestUtils.EPSILON); break; // (a) 0.624928280738513
								}
							}
						}
					}
				}
				else {
					if (tuple.getFirst().getCoord().getX() == 50.) {
						if (tuple.getFirst().getCoord().getY() == 50.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(3.5937361608359226, value, MatsimTestUtils.EPSILON); break; // (a) 3.534903784873003
									case TransportMode.car: Assertions.assertEquals(3.592131321419011, value, MatsimTestUtils.EPSILON); break; // (a) 3.534578407739  (b) 3.592131222564318
									case TransportMode.bike: Assertions.assertEquals(3.650823251958846, value, MatsimTestUtils.EPSILON); break; // (a) 3.6120342274419914
									case TransportMode.walk: Assertions.assertEquals(3.256022746025017, value, MatsimTestUtils.EPSILON); break; // (a) 3.086841618403501
									case TransportMode.pt: Assertions.assertEquals(3.5444584871239586, value, MatsimTestUtils.EPSILON); break;
									case "matrixBasedPt": Assertions.assertEquals(3.0405848846934704, value, MatsimTestUtils.EPSILON); break; // (a) 1.8481579174590859
								}
							}
						}
						else if (tuple.getFirst().getCoord().getY() == 150.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(3.5937361608359226, value, MatsimTestUtils.EPSILON); break; // (a) 3.562937932720491
									case TransportMode.car: Assertions.assertEquals(3.5937363214190112, value, MatsimTestUtils.EPSILON); break; // (a) 3.5625329257950717  (b) 3.5937361608359226
									case TransportMode.bike: Assertions.assertEquals(3.650823251958846, value, MatsimTestUtils.EPSILON); break; // (a) 3.6308412309842275
									case TransportMode.walk: Assertions.assertEquals(3.256022746025017, value, MatsimTestUtils.EPSILON); break; // (a) 3.1582090479224982
									case TransportMode.pt: Assertions.assertEquals(3.5444584871239586, value, MatsimTestUtils.EPSILON); break; // (a) 3.443890734766343
									case "matrixBasedPt": Assertions.assertEquals(3.0405848846934704, value, MatsimTestUtils.EPSILON); break; // (a) 1.8481579174590859
								}
							}
						}
					}
					if (tuple.getFirst().getCoord().getX() == 150.) {
						if (tuple.getFirst().getCoord().getY() == 50.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(3.621797746434273, value, MatsimTestUtils.EPSILON); break; // (a) 3.534903784873003
									case TransportMode.car: Assertions.assertEquals(3.621797881699601, value, MatsimTestUtils.EPSILON); break; // (a) 3.534578407739  (b) 3.621797746434273
									case TransportMode.bike: Assertions.assertEquals(3.66963791800913, value, MatsimTestUtils.EPSILON); break; // (a) 3.6120342274419914
									case TransportMode.walk: Assertions.assertEquals(3.328148327589044, value, MatsimTestUtils.EPSILON); break; // (a) 3.086841618403501
									case TransportMode.pt: Assertions.assertEquals(3.389540900429034, value, MatsimTestUtils.EPSILON); break;
									case "matrixBasedPt": Assertions.assertEquals(3.0405848846934704, value, MatsimTestUtils.EPSILON); break; // (a) 1.8481579174590859
								}
							}
						}
						else if (tuple.getFirst().getCoord().getY() == 150.) {
							for (String mode : accessibilitiesMap.get(tuple).keySet()) {
								double value = accessibilitiesMap.get(tuple).get(mode);
								switch (mode) {
									case "freespeed": Assertions.assertEquals(3.621797746434273, value, MatsimTestUtils.EPSILON); break; // (a) 3.5918646370880176
									case TransportMode.car: Assertions.assertEquals(3.621797881699601, value, MatsimTestUtils.EPSILON); break; // (a) 3.591516884230813  (b) 3.621797746434273
									case TransportMode.bike: Assertions.assertEquals(3.66963791800913, value, MatsimTestUtils.EPSILON); break; // (a) 3.6500320126532544
									case TransportMode.walk: Assertions.assertEquals(3.328148327589044, value, MatsimTestUtils.EPSILON); break; // (a) 3.2374596523136154
									case TransportMode.pt: Assertions.assertEquals(3.389540900429034, value, MatsimTestUtils.EPSILON); break; // (a) 3.3065653876694023
									case "matrixBasedPt": Assertions.assertEquals(2.893683407764653, value, MatsimTestUtils.EPSILON); break; // (a) 2.0112226418584043
								}
							}
						}
					}
				}
			}
		}
	}
}
