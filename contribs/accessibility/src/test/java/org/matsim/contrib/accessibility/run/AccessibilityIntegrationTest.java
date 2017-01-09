/* *********************************************************************** *
 * project: org.matsim.*
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
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author nagel
 */
public class AccessibilityIntegrationTest {

	private static final Logger LOG = Logger.getLogger(AccessibilityIntegrationTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();


	@SuppressWarnings("static-method")
	@Test
	public void testMainMethod() {
		Config config = ConfigUtils.createConfig();
		final AccessibilityConfigGroup acg = new AccessibilityConfigGroup();
		acg.setCellSizeCellBasedAccessibility(100);
		config.addModule( acg);

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
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox.toString());
		acg.setBoundingBoxBottom(min);
		acg.setBoundingBoxTop(max);
		acg.setBoundingBoxLeft(min);
		acg.setBoundingBoxRight(max);

		// ---

		final Scenario sc = createTestScenario(config);

		// ---

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(true,true,true,true,true);
		module.addSpatialGridDataExchangeListener( evaluateListener ) ;
		controler.addOverridingModule(module);

		// ---

		controler.run();

		// compare some results -> done in EvaluateTestResults.  But check that this was done at all:
		Assert.assertTrue( evaluateListener.isDone() ) ;
	}


	@Test
	public void testWithExtentDeterminedByNetwork() {
		final Config config = createTestConfig() ;
		
		// ---
		
		final Scenario sc = createTestScenario(config) ;

		// ---

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final EvaluateTestResults evaluateListener = new EvaluateTestResults(true,true,true,true,true);
		module.addSpatialGridDataExchangeListener( evaluateListener ) ;
		controler.addOverridingModule(module);

		// ---

		controler.run();

		// compare some results -> done in EvaluateTestResults.  But check that this was done at all:
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
		// set area by shapefile in this test
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile.toString());
		//		acm.setShapeFileCellBasedAccessibility(url.getPath()); // yyyyyy todo
		acg.setShapeFileCellBasedAccessibility(f.getAbsolutePath());

		// ---

		final Scenario sc = createTestScenario(config) ;

		// ---
		
		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		module.addSpatialGridDataExchangeListener( new EvaluateTestResults(true,true,true,true,true) ) ;
		controler.addOverridingModule(module);
		
		// ---
		
		controler.run();

		// compare some results -> done in EvaluateTestResults 
	}
	
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
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromFile.toString());
		acg.setMeasuringPointsFile(f.getAbsolutePath());
		
		final Scenario sc = createTestScenario(config);
		
		Controler controler = new Controler(sc);
		
		final AccessibilityModule module = new AccessibilityModule();
//		module.addSpatialGridDataExchangeListener( new EvaluateTestResults(true,true,true,true,true) ) ;
		controler.addOverridingModule(module);
		
		controler.run();
		
		/* FIXME This currently does NOTHING... it completely ignores the 
		 * file-based instruction.
		 */
	}



	/**
	 * This is called by the GridBasedAccessibilityListener and gets the resulting SpatialGrids. This test checks if the 
	 * SpatialGrids for activated transport modes (see above) are instantiated or null if the specific transport mode is
	 * not activated.
	 * 
	 * @author thomas
	 */
	 static class EvaluateTestResults implements SpatialGridDataExchangeInterface{

		private Map<Modes4Accessibility,Boolean> isComputingMode = new HashMap<>();
		
		private boolean isDone = false ;

		/**
		 * constructor
		 * 
		 * Determines for each transport mode if its activated (true) or not (false):
		 * - For transport modes with "useXXXGrid=false" the SpatialGrid must be null
		 * - For transport modes with "useXXXGrid=true" the SpatialGrid must not be null
		 * 
		 * @param usingFreeSpeedGrid
		 * @param usingCarGrid
		 * @param usingBikeGrid
		 * @param usingWalkGrid
		 * @param usingPtGrid
		 */
		public EvaluateTestResults(boolean usingFreeSpeedGrid, boolean usingCarGrid, boolean usingBikeGrid, boolean usingWalkGrid, boolean usingPtGrid){
			this.isComputingMode.put( Modes4Accessibility.freespeed, usingFreeSpeedGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.car, usingCarGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.bike, usingBikeGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.walk, usingWalkGrid ) ;

			//			this.isComputingMode.put( Modes4Accessibility.pt, usingPtGrid ) ;
			// this wasn't commented out yesterday, but I cannot say what it was doing in which way. kai, dec'16
			// actually it looks like it was working at some time since there are commented-out regression tests for pt.  But they 
			// were already commented out when I looked at this.  kai, dec'16
		}

		/**
		 * This gets the resulting SpatialGrids from the GridBasedAccessibilityListener.
		 * - SpatialGrids for transport modes with "useXXXGrid=false"must be null
		 * - SpatialGrids for transport modes with "useXXXGrid=true"must not be null
		 * 
		 */
		@Override
		public void setAndProcessSpatialGrids( Map<String,SpatialGrid> spatialGrids ){

			LOG.info("Evaluating resuts ...");

			for ( Modes4Accessibility modeEnum : Modes4Accessibility.values() ) {
				String mode = modeEnum.toString(); // TODO only temporarily
				LOG.info("mode="+mode);
				Gbl.assertNotNull(spatialGrids);
				if ( this.isComputingMode.get(modeEnum)!=null) {
					// this was without the !=null yesterday but I cannot say what it was doing or why it was working or not.  kai, dec'16
					Assert.assertNotNull( spatialGrids.get(mode) ) ;
				} else {
					Assert.assertNull( spatialGrids.get(mode) ) ;
				}
			}

			for(double x = 50; x < 200; x += 100){

				for(double y = 50; y < 200; y += 100){

					final AccessibilityResults expected = new AccessibilityResults();

					if( (x == 50 || x == 150) && y == 50){

						//expected.accessibilityFreespeed = 2.20583781881484;
						expected.accessibilityFreespeed = 2.1486094237531126;
						expected.accessibilityCar = 2.14860942375311;
						expected.accessibilityBike = 2.2257398663221;
						expected.accessibilityWalk = 1.70054725728361;
						//						expected.accessibilityPt = 0.461863556339195;

					} else if(x == 50 && y == 150){

						// corrected with change in orthogonal projection computation
						//						expected.accessibilityFreespeed = 2.1555292541877;
						//						expected.accessibilityCar = 2.1555292541877;
						//						expected.accessibilityBike = 2.20170415738971;
						//						expected.accessibilityWalk = 1.88907197432798;
						//						expected.accessibilityPt = 0.461863556339195;
						expected.accessibilityFreespeed = 2.1766435716006005;
						expected.accessibilityCar = 2.1766435716006005;
						expected.accessibilityBike = 2.2445468698643367;
						//						expected.accessibilityBike = 1.; // deliberately wrong for testing
						expected.accessibilityWalk = 1.7719146868026079;
						//						expected.accessibilityPt = 0.461863556339195;

					} else if(x == 150 && y == 150){

						// corrected with change in orthogonal projection computation
						//						expected.accessibilityFreespeed = 2.18445595855523;
						//						expected.accessibilityCar = 2.18445595855523;
						//						expected.accessibilityBike = 2.22089493905874;
						//						expected.accessibilityWalk = 1.9683225787191;
						//						expected.accessibilityPt = 0.624928280738513;
						expected.accessibilityFreespeed = 2.2055702759681273;
						expected.accessibilityCar = 2.2055702759681273;
						expected.accessibilityBike = 2.2637376515333636;
						expected.accessibilityWalk = 1.851165291193725;
						//						expected.accessibilityPt = 0.624928280738513;

					}

					final AccessibilityResults actual = new AccessibilityResults();
					//					actual.accessibilityFreespeed = spatialGrids.get(Modes4Accessibility.freespeed).getValue(new Coord(x, y));
					//					actual.accessibilityCar = spatialGrids.get(Modes4Accessibility.car).getValue(new Coord(x, y));
					//					actual.accessibilityBike = spatialGrids.get(Modes4Accessibility.bike).getValue(new Coord(x, y));
					//					actual.accessibilityWalk = spatialGrids.get(Modes4Accessibility.walk).getValue(new Coord(x, y));
					//					actual.accessibilityPt = spatialGrids.get(Modes4Accessibility.pt).getValue(new Coord(x, y));
					actual.accessibilityFreespeed = spatialGrids.get("freespeed").getValue(new Coord(x, y));
					actual.accessibilityCar = spatialGrids.get(TransportMode.car).getValue(new Coord(x, y));
					actual.accessibilityBike = spatialGrids.get(TransportMode.bike).getValue(new Coord(x, y));
					actual.accessibilityWalk = spatialGrids.get(TransportMode.walk).getValue(new Coord(x, y));
					//					actual.accessibilityPt = spatialGrids.get(Modes4Accessibility.pt).getValue(new Coord(x, y));

					Assert.assertTrue(
							"accessibility at coord " + x + "," + y + " does not match for "+
									expected.nonMatching( actual , MatsimTestUtils.EPSILON ),
									expected.equals(actual, MatsimTestUtils.EPSILON ) );
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

		public String nonMatching(  final AccessibilityResults o , final double epsilon ) {
			return
					matchingMessage( "PT ", o.accessibilityPt , accessibilityPt , epsilon ) +
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
			return Double.compare(that.accessibilityPt, accessibilityPt) == 0;

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
					'}';
		}
	}
	
	private Config createTestConfig() {
		final Config config = ConfigUtils.createConfig();

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(100);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);

		// modify config according to needs
		Network network = CreateTestNetwork.createTestNetwork(); // this is a little odd. kai, dec'16
		String networkFile = utils.getOutputDirectory() +"network.xml";
		new NetworkWriter(network).write(networkFile);
		config.network().setInputFile( networkFile);

		MatrixBasedPtRouterConfigGroup mbConfig = new MatrixBasedPtRouterConfigGroup();
		mbConfig.setPtStopsInputFile(utils.getClassInputDirectory() + "ptStops.csv");
		mbConfig.setPtTravelDistancesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setPtTravelTimesInputFile(utils.getClassInputDirectory() + "ptTravelInfo.csv");
		mbConfig.setUsingPtStops(true);
		mbConfig.setUsingTravelTimesAndDistances(true);
		config.addModule(mbConfig);

		config.controler().setLastIteration(10);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}

	
	private static Scenario createTestScenario(final Config config) {
		final Scenario sc = ScenarioUtils.loadScenario(config);

		MatrixBasedPtRouterConfigGroup mbConfig = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.class ) ;
		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(sc.getNetwork()), mbConfig) ;
		sc.addScenarioElement(PtMatrix.NAME, ptMatrix);

		// creating test opportunities (facilities)
		final ActivityFacilities opportunities = sc.getActivityFacilities();
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id<ActivityFacility> id = Id.create(link.getId(), ActivityFacility.class);
			Coord coord = link.getCoord();
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(id, coord);
			opportunities.addActivityFacility(facility);
		}
		return sc;
	}



}