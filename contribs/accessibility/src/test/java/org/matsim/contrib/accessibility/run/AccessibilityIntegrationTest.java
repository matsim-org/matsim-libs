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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * I can't say how similar or different to {@link AccessibilityIntegrationTest} this one here is.  kai, feb'17
 * 
 * @author nagel, dziemke
 */
public class AccessibilityIntegrationTest {

	private static final Logger LOG = Logger.getLogger(AccessibilityIntegrationTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@SuppressWarnings("static-method")
	@Ignore
	@Test
	public void testMainMethod() {
		Config config = ConfigUtils.createConfig();
		final AccessibilityConfigGroup acg = new AccessibilityConfigGroup();
		acg.setCellSizeCellBasedAccessibility(100);
		config.addModule(acg);

		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Network network = CreateTestNetwork.createTestNetwork();

		ScenarioUtils.ScenarioBuilder builder = new ScenarioUtils.ScenarioBuilder(config) ;
		builder.setNetwork(network);
		Scenario sc = builder.build() ;

		// creating test opportunities (facilities)
		ActivityFacilities opportunities = sc.getActivityFacilities();
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id<ActivityFacility> id = Id.create(link.getId(), ActivityFacility.class);
			Coord coord = link.getCoord();
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(id, coord);
			{
				ActivityOption option = new ActivityOptionImpl("w") ;
				facility.addActivityOption(option);
			}
			{
				ActivityOption option = new ActivityOptionImpl("h") ;
				facility.addActivityOption(option);
			}
			opportunities.addActivityFacility(facility);
		}

		org.matsim.contrib.accessibility.run.RunAccessibilityExample.run(sc);
	}


	@Test
	public void testWithBoundingBox() {
		final Config config = createTestConfig();

		// set bounding box manually in this test
		// test values to define bounding box; these values usually come from a config file
		double min = 0.;
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
//		final EvaluateTestResults evaluateListener = new EvaluateTestResults(true, true, true, true, true, true);
		final EvaluateTestResults evaluateListener = new EvaluateTestResults();
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


	@Ignore
	@Test
	public void testWithExtentDeterminedByNetwork() {
		final Config config = createTestConfig() ;
		
		final Scenario sc = createTestScenario(config) ;
		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults();
//		final EvaluateTestResults evaluateListener = new EvaluateTestResults(true, true, true, true, false, true);
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


	@Ignore
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
		final EvaluateTestResults evaluateListener = new EvaluateTestResults();
//		final EvaluateTestResults evaluateListener = new EvaluateTestResults(true, true, true, true, false, true);
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
		
		//
//		List<String> mainModes = new ArrayList<>();
//		mainModes.add("car");
//		mainModes.add("bus");
//		config.qsim().setMainModes(mainModes);
		//

		config.transit().setUseTransit(true);
//		config.transit().setTransitScheduleFile(utils.getClassInputDirectory() + "schedule.xml");
//		config.transit().setVehiclesFile(utils.getClassInputDirectory() + "vehicles.xml");
		config.transit().setTransitScheduleFile(utils.getClassInputDirectory() + "schedule2.xml");
		config.transit().setVehiclesFile(utils.getClassInputDirectory() + "vehicles.xml");
		
		//
		Set<String> transitModes = new HashSet<>();
		transitModes.add(TransportMode.pt);
		config.transit().setTransitModes(transitModes);
		//
		
		//
//		{
//			ModeRoutingParams walkPars = new ModeRoutingParams(TransportMode.walk);
//			walkPars.setBeelineDistanceFactor(1.3);
//			walkPars.setTeleportedModeSpeed(4.);
//			config.plansCalcRoute().addModeRoutingParams(walkPars);
//		}
		//

//		{
			ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
			config.planCalcScore().addModeParams(ptParams);
//		}
		
//		{
//			ModeParams ptParams = new ModeParams(TransportMode.pt);
//			ptParams.setMarginalUtilityOfDistance(1.);
//			config.planCalcScore().addModeParams(ptParams);
//		}

		MatrixBasedPtRouterConfigGroup mbConfig = new MatrixBasedPtRouterConfigGroup();
		mbConfig.setPtStopsInputFile(utils.getClassInputDirectory() + "ptStops.csv");
		mbConfig.setPtTravelDistancesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setPtTravelTimesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setUsingPtStops(true);
		mbConfig.setUsingTravelTimesAndDistances(true);
		config.addModule(mbConfig);

//		config.controler().setLastIteration(10);
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

//		private Map<String,Boolean> isComputingMode = new HashMap<>();
		
		private boolean isDone = false ;

		public EvaluateTestResults(){
//		public EvaluateTestResults(boolean usingFreeSpeedGrid, boolean usingCarGrid, boolean usingBikeGrid, boolean usingWalkGrid, boolean usingPtGrid, boolean usingMatrixBasedPtGrid){
//			this.isComputingMode.put("freespeed", usingFreeSpeedGrid);
//			this.isComputingMode.put(TransportMode.car, usingCarGrid);
//			this.isComputingMode.put(TransportMode.bike, usingBikeGrid);
//			this.isComputingMode.put(TransportMode.walk, usingWalkGrid);
//			this.isComputingMode.put(TransportMode.pt, usingPtGrid);
//			this.isComputingMode.put("matrixBasedPt", usingMatrixBasedPtGrid);
		}

		/**
		 * This gets the resulting SpatialGrids from the GridBasedAccessibilityListener.
		 * - SpatialGrids for transport modes with "useXXXGrid=false"must be null
		 * - SpatialGrids for transport modes with "useXXXGrid=true"must not be null
		 */
		@Override
		public void setAndProcessSpatialGrids( Map<String,SpatialGrid> spatialGrids ){

			LOG.info("Evaluating resuts ...");

//			for (Modes4Accessibility modeEnum : Modes4Accessibility.values()) {
//				String mode = modeEnum.toString(); // TODO only temporarily
//				LOG.info("mode=" + mode);
//				Gbl.assertNotNull(spatialGrids);
////				if (this.isComputingMode.get(mode) != null) {
//					// this was without the !=null yesterday but I cannot say what it was doing or why it was working or not.  kai, dec'16
//				if (this.isComputingMode.get(mode)) { // I think it should check for "not false" rather than "not null". dz, mar'17
//					Assert.assertNotNull(spatialGrids.get(mode));
//				} else {
//					Assert.assertNull(spatialGrids.get(mode));
//				}
//			}

			for(double x = 50; x < 200; x += 100){
				for(double y = 50; y < 200; y += 100){
					final AccessibilityResults expected = new AccessibilityResults();

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
						// expected.accessibilityBike = 1.; // deliberately wrong for testing
						expected.accessibilityWalk = 1.7719146868026079;
						expected.accessibilityPt = 2.057596373646452;
						expected.accessibilityMatrixBasedPt = 0.461863556339195;
						// expected.accessibilityMatrixBasedPt = 1.; // deliberately wrong for testing
					} else if (x == 150 && y == 150) {
						expected.accessibilityFreespeed = 2.2055702759681273;
						expected.accessibilityCar = 2.2052225231109226;
						expected.accessibilityBike = 2.2637376515333636;
						expected.accessibilityWalk = 1.851165291193725;
						expected.accessibilityPt = 1.9202710265495115;
						expected.accessibilityMatrixBasedPt = 0.624928280738513;
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