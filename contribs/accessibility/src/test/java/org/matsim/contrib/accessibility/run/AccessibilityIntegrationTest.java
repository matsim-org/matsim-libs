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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
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

	private static final Logger LOG = Logger.getLogger(AccessibilityIntegrationTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();	
	
	@Test
	public void testRunAccessibilityExample() {
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
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", value, 2.1486094237531126, utils.EPSILON);
	            		} else if (y == 150){
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", value, 2.1766435716006005, utils.EPSILON);
	            		} 
	            	} else if (x == 150) {
	            		if (y == 50) {
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", value, 2.1486094237531126, utils.EPSILON);
	            		} else if (y == 150){
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", value, 2.2055702759681273, utils.EPSILON);
	            		}
	            	}
            	}
            }
        });
	}
	
	
	@Test
	public void testWithBoundingBoxConfigFile() {
		Config config = ConfigUtils.loadConfig(utils.getInputDirectory() + "config.xml");

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.matrixBasedPt, true);

		ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
		config.planCalcScore().addModeParams(ptParams);
		
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class) ;
		
		final Scenario sc = ScenarioUtils.loadScenario(config);
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);
		
		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(false);
		module.addSpatialGridDataExchangeListener(evaluateListener);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		controler.run();

		// Compare some results -> done in EvaluateTestResults. Check here that this was done at all
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}
	

	@Test
	public void testWithBoundingBox() {
		final Config config = createTestConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min);
		acg.setBoundingBoxTop(max);
		acg.setBoundingBoxLeft(min);
		acg.setBoundingBoxRight(max);
		
		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);
		
		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(false);
		module.addSpatialGridDataExchangeListener(evaluateListener);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});

		controler.run();

		// Compare some results -> done in EvaluateTestResults. Check here that this was done at all
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}
	
	
	@Test
	public void testWithBoundingBoxUsingOpportunityWeights() {
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
		
		final Scenario sc = createTestScenarioUsingOpportunityWeights(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);
		
		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(true);
		module.addSpatialGridDataExchangeListener(evaluateListener);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});

		controler.run();

		// Compare some results -> done in EvaluateTestResults. Check here that this was done at all
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}


	@Test
	public void testWithExtentDeterminedByNetwork() {
		final Config config = createTestConfig() ;
		
		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(false);
		module.addSpatialGridDataExchangeListener(evaluateListener);
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});

		controler.run();

		// Compare some results -> done in EvaluateTestResults. Check here that this was done at all
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}


	@Test
	public void testWithExtentDeterminedShapeFile() {
		Config config = createTestConfig() ;

		File f = new File(this.utils.getInputDirectory() + "shapefile.shp"); // shape file completely covers the road network

		if(!f.exists()){
			LOG.error("Shape file not found! testWithExtentDeterminedShapeFile could not be tested...");
			Assert.assertTrue(f.exists());
		}

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(100);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
		//		acg.setShapeFileCellBasedAccessibility(url.getPath()); // yyyyyy todo
		acg.setShapeFileCellBasedAccessibility(f.getAbsolutePath());

		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(false);
		module.addSpatialGridDataExchangeListener(evaluateListener) ;
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		
		controler.run();

		// Compare some results -> done in EvaluateTestResults. Check here that this was done at all
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}
	
	
	@Test
	public void testWithPredefinedMeasuringPoints() {
		Config config = createTestConfig() ;

		File f = new File(this.utils.getInputDirectory() + "measuringPoints.xml");

		if(!f.exists()){
			LOG.error("Facilities file with measuring points not found! testWithMeasuringPointsInFacilitiesFile could not be performed...");
			Assert.assertTrue(f.exists());
		}
		
		Scenario measuringPointsSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(measuringPointsSc).readFile(f.getAbsolutePath());
		ActivityFacilities measuringPoints = (ActivityFacilities) AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(measuringPointsSc, null);

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		
		//
		acg.setCellSizeCellBasedAccessibility(100);
		//
		
		acg.setMeasuringPointsFacilities(measuringPoints);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromFacilitiesObject);

		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(false);
		module.addSpatialGridDataExchangeListener(evaluateListener) ;
		controler.addOverridingModule(module);
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});
		
		controler.run();

		// Compare some results -> done in EvaluateTestResults. Check here that this was done at all
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}

	
	@Ignore
	@Test
	public void testWithFile(){
		/*TODO Complete - JWJ, Dec'16 */
		Config config = createTestConfig();
		
		File f = new File(this.utils.getInputDirectory() + "pointFile.csv");
		if(!f.exists()){
			LOG.error("Point file not found! testWithFile could not be tested...");
			Assert.assertTrue(f.exists());
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
		acg.setCellSizeCellBasedAccessibility(100);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.matrixBasedPt, true);
		
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
		config.planCalcScore().addModeParams(ptParams);

		MatrixBasedPtRouterConfigGroup mbConfig = new MatrixBasedPtRouterConfigGroup();
		mbConfig.setPtStopsInputFile(utils.getClassInputDirectory() + "ptStops.csv");
		mbConfig.setPtTravelDistancesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setPtTravelTimesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setUsingPtStops(true);
		mbConfig.setUsingTravelTimesAndDistances(true);
		config.addModule(mbConfig);

		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}

	
	private static Scenario createTestScenario(final Config config) {
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(Id.create(link.getId(), ActivityFacility.class), link.getCoord());
			opportunities.addActivityFacility(facility);
		}
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
		return scenario;
	}
	
	
	private static Scenario createTestScenarioUsingOpportunityWeights(final Config config) {
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link; with a weight
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(Id.create(link.getId(), ActivityFacility.class), link.getCoord());
			facility.getCustomAttributes().put("weight", 2.);
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

		// add nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 100));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 0, (double) 200));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 0, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 100, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 100, (double) 200));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 100, (double) 0));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 200, (double) 100));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 200, (double) 200));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 200, (double) 0));
		
		//
		Set<String> modes = new HashSet<>();
		modes.add("car");
//		modes.add("bus");
		//

		// add links (bi-directional)
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

	
	/**
	 * This is called by the GridBasedAccessibilityListener and gets the resulting SpatialGrids. This test checks if the 
	 * SpatialGrids for activated transport modes (see above) are instantiated or null if the specific transport mode is
	 * not activated.
	 * 
	 * @author thomas
	 */
	 static class EvaluateTestResults implements SpatialGridDataExchangeInterface{

		private boolean useOpportunityWeights = false;
		private boolean isDone = false;

		public EvaluateTestResults(boolean useOpportunityWeights){
			this.useOpportunityWeights = useOpportunityWeights;
		}

		/**
		 * This gets the resulting SpatialGrids from the GridBasedAccessibilityListener.
		 * - SpatialGrids for transport modes with "useXXXGrid=false"must be null
		 * - SpatialGrids for transport modes with "useXXXGrid=true"must not be null
		 */
		@Override
		public void setAndProcessSpatialGrids( Map<String,SpatialGrid> spatialGrids ){

			LOG.info("Evaluating resuts ...");

			for(double x = 50; x < 200; x += 100){
				for(double y = 50; y < 200; y += 100){
					final AccessibilityResults expected = new AccessibilityResults();

					if (!useOpportunityWeights) {
						if (x == 50 && y == 50) {
							expected.accessibilityFreespeed = 2.1486094237531126;
							expected.accessibilityCar = 2.1482840466191093;
							expected.accessibilityBike = 2.2257398663221;
							expected.accessibilityWalk = 1.70054725728361;
							expected.accessibilityPt = 2.1581641260040683;
							expected.accessibilityMatrixBasedPt = 0.461863556339195;
						} else if (x == 150 && y == 50) {
							expected.accessibilityFreespeed = 2.1486094237531126;
							expected.accessibilityCar = 2.1482840466191093;
							expected.accessibilityBike = 2.2257398663221;
							expected.accessibilityWalk = 1.70054725728361;
							expected.accessibilityPt = 2.0032465393091434;
							expected.accessibilityMatrixBasedPt = 0.461863556339195;
						} else if (x == 50 && y == 150) {
							expected.accessibilityFreespeed = 2.1766435716006005;
							expected.accessibilityCar = 2.176238564675181;
							expected.accessibilityBike = 2.2445468698643367;
							expected.accessibilityWalk = 1.7719146868026079;
							expected.accessibilityPt = 2.057596373646452;
							expected.accessibilityMatrixBasedPt = 0.461863556339195;
						} else if (x == 150 && y == 150) {
							expected.accessibilityFreespeed = 2.2055702759681273;
							expected.accessibilityCar = 2.2052225231109226;
							expected.accessibilityBike = 2.2637376515333636;
							expected.accessibilityWalk = 1.851165291193725;
							expected.accessibilityPt = 1.9202710265495115;
							expected.accessibilityMatrixBasedPt = 0.624928280738513;
						}
					} else {
						if (x == 50 && y == 50) {
							expected.accessibilityFreespeed = 3.534903784873003;
							expected.accessibilityCar = 3.534578407739;
							expected.accessibilityBike = 3.6120342274419914;
							expected.accessibilityWalk = 3.086841618403501;
							expected.accessibilityPt = 3.5444584871239586;
							expected.accessibilityMatrixBasedPt = 1.8481579174590859;
						} else if (x == 150 && y == 50) {
							expected.accessibilityFreespeed = 3.534903784873003;
							expected.accessibilityCar = 3.534578407739;
							expected.accessibilityBike = 3.6120342274419914;
							expected.accessibilityWalk = 3.086841618403501;
							expected.accessibilityPt = 3.389540900429034;
							expected.accessibilityMatrixBasedPt = 1.8481579174590859;
						} else if (x == 50 && y == 150) {
							expected.accessibilityFreespeed = 3.562937932720491;
							expected.accessibilityCar = 3.5625329257950717;
							expected.accessibilityBike = 3.6308412309842275;
							expected.accessibilityWalk = 3.1582090479224982;
							expected.accessibilityPt = 3.443890734766343;
							expected.accessibilityMatrixBasedPt = 1.8481579174590859;
						} else if (x == 150 && y == 150) {
							expected.accessibilityFreespeed = 3.5918646370880176;
							expected.accessibilityCar = 3.591516884230813;
							expected.accessibilityBike = 3.6500320126532544;
							expected.accessibilityWalk = 3.2374596523136154;
							expected.accessibilityPt = 3.3065653876694023;
							expected.accessibilityMatrixBasedPt = 2.0112226418584043;
						}
					}

					final AccessibilityResults actual = new AccessibilityResults();
					actual.accessibilityFreespeed = spatialGrids.get("freespeed").getValue(new Coord(x, y));
					actual.accessibilityCar = spatialGrids.get(TransportMode.car).getValue(new Coord(x, y));
					actual.accessibilityBike = spatialGrids.get(TransportMode.bike).getValue(new Coord(x, y));
					actual.accessibilityWalk = spatialGrids.get(TransportMode.walk).getValue(new Coord(x, y));
					actual.accessibilityPt = spatialGrids.get(TransportMode.pt).getValue(new Coord(x, y));
					actual.accessibilityMatrixBasedPt = spatialGrids.get("matrixBasedPt").getValue(new Coord(x, y));

					Assert.assertTrue(
							"Accessibility at coord " + x + "," + y + " does not match for " +
									expected.nonMatching(actual , MatsimTestUtils.EPSILON),
									expected.equals(actual, MatsimTestUtils.EPSILON));
				}
			}
			isDone = true ;
			LOG.info("... done!");
		}
		boolean isDone() {
			return isDone ;
		}
	}

	 
	// Allows getting information on all accessibilities,
	// even if several fails
	// Would be nicer to make one test per mode
	private static class AccessibilityResults {
		double accessibilityFreespeed = Double.NaN;
		double accessibilityCar = Double.NaN;
		double accessibilityBike = Double.NaN;
		double accessibilityWalk = Double.NaN;
		double accessibilityPt = Double.NaN;
		double accessibilityMatrixBasedPt = Double.NaN;

		public String nonMatching(  final AccessibilityResults o , final double epsilon ) {
			return
					matchingMessage( "PT ", o.accessibilityPt , accessibilityPt , epsilon ) +
					matchingMessage( "MATRIXBASEDPT ", o.accessibilityMatrixBasedPt , accessibilityMatrixBasedPt , epsilon ) +
					matchingMessage( "CAR " , o.accessibilityCar , accessibilityCar , epsilon ) +
					matchingMessage( "FREESPEED", o.accessibilityFreespeed , accessibilityFreespeed , epsilon ) +
					matchingMessage( "BIKE ", o.accessibilityBike , accessibilityBike , epsilon ) +
					matchingMessage( "WALK", o.accessibilityWalk , accessibilityWalk , epsilon );
		}

		public boolean equals( final AccessibilityResults o , final double epsilon ) {
			return nonMatching( o , epsilon ).isEmpty();
		}

		private String matchingMessage( String mode , double d1 , double d2 , double epsilon ) {
			final boolean match = (Double.isNaN( d1 ) && Double.isNaN( d2 )) ||
					Math.abs( d1 - d2 ) < epsilon;
			if ( match ) return "";
			return mode+" (actual="+d1+", expected="+d2+")";
		}

		// equals and hashCode automatically generated by intellij
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			AccessibilityResults that = (AccessibilityResults) o;

			if (Double.compare(that.accessibilityFreespeed, accessibilityFreespeed) != 0) return false;
			if (Double.compare(that.accessibilityCar, accessibilityCar) != 0) return false;
			if (Double.compare(that.accessibilityBike, accessibilityBike) != 0) return false;
			if (Double.compare(that.accessibilityWalk, accessibilityWalk) != 0) return false;
			if (Double.compare(that.accessibilityPt, accessibilityPt) != 0) return false;
			return Double.compare(that.accessibilityMatrixBasedPt, accessibilityMatrixBasedPt) == 0;

		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			temp = Double.doubleToLongBits(accessibilityFreespeed);
			result = (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(accessibilityCar);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(accessibilityBike);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(accessibilityWalk);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(accessibilityPt);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(accessibilityMatrixBasedPt);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "AccessibilityResults{" +
					"accessibilityFreespeed=" + accessibilityFreespeed +
					", accessibilityCar=" + accessibilityCar +
					", accessibilityBike=" + accessibilityBike +
					", accessibilityWalk=" + accessibilityWalk +					
					", accessibilityPt=" + accessibilityPt +
					", accessibilityMatrixBasedPt=" + accessibilityMatrixBasedPt +
					'}';
		}
	}
}