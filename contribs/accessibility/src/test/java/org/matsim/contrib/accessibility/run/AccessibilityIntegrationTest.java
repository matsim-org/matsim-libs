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
import java.util.Map;

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
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
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
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", 2.1486094237531126, value, utils.EPSILON);
	            		} else if (y == 150){
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", 2.1766435716006005, value, utils.EPSILON);
	            		} 
	            	} else if (x == 150) {
	            		if (y == 50) {
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", 2.1486094237531126, value, utils.EPSILON);
	            		} else if (y == 150){
	            			Assert.assertEquals("Wrong work accessibility value at x=" + x + ", y=" + y + ":", 2.2055702759681273, value, utils.EPSILON);
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

		//		URL url = AccessibilityIntegrationTest.class.getClassLoader().getResource(new File(this.utils.getInputDirectory()).getAbsolutePath() + "shapeFile2.shp");
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
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromFile);
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
		Network network = CreateTestNetwork.createTestNetwork(); // this is a little odd. kai, dec'16
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

			LOG.info("Evaluating results ...");

			for(double x = 50; x < 200; x += 100){
				for(double y = 50; y < 200; y += 100){
					final AccessibilityResults expected = new AccessibilityResults();

					if (!useOpportunityWeights) {
						if (x == 50 && y == 50) {
							expected.accessibilityFreespeed = 2.14486658890362;
							expected.accessibilityCar = 2.14486658890362;
							expected.accessibilityBike = 2.224157412491891;
							expected.accessibilityWalk = 1.6634857793433138;
							expected.accessibilityPt = 2.1581641260040683;
							expected.accessibilityMatrixBasedPt = 1.6542905235735796;
						} else if (x == 150 && y == 50) {
							expected.accessibilityFreespeed = 2.14486658890362;
							expected.accessibilityCar = 2.14486658890362;
							expected.accessibilityBike = 2.224157412491891;
							expected.accessibilityWalk = 1.6634857793433138;
							expected.accessibilityPt = 2.0032465393091434;
							expected.accessibilityMatrixBasedPt = 1.6542905235735796;
						} else if (x == 50 && y == 150) {
							expected.accessibilityFreespeed = 2.207441799716032;
							expected.accessibilityCar = 2.207441799716032;
							expected.accessibilityBike = 2.2645288908389554;
							expected.accessibilityWalk = 1.8697283849051263;
							expected.accessibilityPt = 2.1581641260040683;
							expected.accessibilityMatrixBasedPt = 1.6542905235735796;
						} else if (x == 150 && y == 150) {
							expected.accessibilityFreespeed = 2.235503385314382;
							expected.accessibilityCar = 2.235503385314382;
							expected.accessibilityBike = 2.2833435568892395;
							expected.accessibilityWalk = 1.9418539664691532;
							expected.accessibilityPt = 2.0032465393091434;
							expected.accessibilityMatrixBasedPt = 1.5073890466447624;
						}
					} else {
						if (x == 50 && y == 50) {
							expected.accessibilityFreespeed = 3.531160950023511;
							expected.accessibilityCar = 3.531160950023511;
							expected.accessibilityBike = 3.610451773611781;
							expected.accessibilityWalk = 3.0497801404632043;
							expected.accessibilityPt = 3.5444584871239586;
							expected.accessibilityMatrixBasedPt = 3.0405848846934704;
						} else if (x == 150 && y == 50) {
							expected.accessibilityFreespeed = 3.531160950023511;
							expected.accessibilityCar = 3.531160950023511;
							expected.accessibilityBike = 3.610451773611781;
							expected.accessibilityWalk = 3.0497801404632043;
							expected.accessibilityPt = 3.389540900429034;
							expected.accessibilityMatrixBasedPt = 3.0405848846934704;
						} else if (x == 50 && y == 150) {
							expected.accessibilityFreespeed = 3.5937361608359226;
							expected.accessibilityCar = 3.5937361608359226;
							expected.accessibilityBike = 3.650823251958846;
							expected.accessibilityWalk = 3.256022746025017;
							expected.accessibilityPt = 3.5444584871239586;
							expected.accessibilityMatrixBasedPt = 3.0405848846934704;
						} else if (x == 150 && y == 150) {
							expected.accessibilityFreespeed = 3.621797746434273;
							expected.accessibilityCar = 3.621797746434273;
							expected.accessibilityBike = 3.66963791800913;
							expected.accessibilityWalk = 3.328148327589044;
							expected.accessibilityPt = 3.389540900429034;
							expected.accessibilityMatrixBasedPt = 2.893683407764653;
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