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

/**
 * 
 */
package org.matsim.contrib.accessibility.run;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nagel
 *
 */
public class AccessibilityIntegrationTest {
	
	private static final Logger log = Logger.getLogger(AccessibilityIntegrationTest.class);
	
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
		Config config = ConfigUtils.createConfig();
		
		// test values to define bounding box.
		// these values usually come from a config file
		double min = 0.;
		double max = 200.;

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm);
		acm.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox.toString());
		acm.setBoundingBoxBottom(min);
		acm.setBoundingBoxTop(max);
		acm.setBoundingBoxLeft(min);
		acm.setBoundingBoxRight(max);
		
		// modify config according to needs
		Network network = CreateTestNetwork.createTestNetwork();
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
		
		Controler controler = new Controler(config);
		ControlerConfigGroup controlerCG = config.controler();
		controlerCG.setLastIteration( 10);
		controlerCG.setOutputDirectory(utils.getOutputDirectory());
		
		final MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario( config);
		ScenarioUtils.loadScenario(sc);

		// creating test opportunities (facilities)
		ActivityFacilities opportunities = sc.getActivityFacilities();
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id<ActivityFacility> id = Id.create(link.getId(), ActivityFacility.class);
			Coord coord = link.getCoord();
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(id, coord);
			opportunities.addActivityFacility(facility);
		}
		
		PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(network), mbConfig) ;
		// yyyy the following is taken from AccessibilityTest without any consideration of a good design.
		double cellSize = 100. ;

		GridBasedAccessibilityControlerListenerV3 gacl = new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, sc.getNetwork());
		// activating transport modes of interest
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			gacl.setComputingAccessibilityForMode(mode, true);
		}
//		gacl.setComputingAccessibilityForMode( Modes4Accessibility.pt, false ); 
		// not sure why this is "false"; presumably, the test is not configured. kai, feb'14
		
		// this will be called by the accessibility listener after the accessibility calculations are finished
		// It checks if the SpatialGrid for activated (true) transport modes are instantiated or null if not (false)
		EvaluateTestResults etr = new EvaluateTestResults(true, true, true, true, true);
		gacl.addSpatialGridDataExchangeListener(etr);
		
		// generating measuring points and SpatialGrids by using the bounding box
		gacl.generateGridsAndMeasuringPointsByCustomBoundary(acm.getBoundingBoxLeft(), acm.getBoundingBoxBottom(), acm.getBoundingBoxRight(), acm.getBoundingBoxTop(), cellSize);
		
		controler.addControlerListener(gacl); 
		
		// yy the correct test is essentially already in AccessibilityTest.testAccessibilityMeasure().  kai, jun'13
		// But that test uses the matsim4urbansim setup, which we don't want to use in the present test.

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
		
		// compare some results -> done in EvaluateTestResults
	}
	
	@Test
	public void testWithExtentDeterminedByNetwork() {
		Config config = ConfigUtils.createConfig();

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm);
//		acm.setCellBasedAccessibilityNetwork(true);
		// is now default
		
		// modify config according to needs
		Network network = CreateTestNetwork.createTestNetwork();
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

		Controler controler = new Controler(config);
		ControlerConfigGroup controlerCG = config.controler();
		controlerCG.setLastIteration( 10);
		controlerCG.setOutputDirectory(utils.getOutputDirectory());
		
		final MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario( config);
		ScenarioUtils.loadScenario(sc);
		

		// creating test opportunities (facilities)
		ActivityFacilities opportunities = sc.getActivityFacilities();
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id<ActivityFacility> id = Id.create(link.getId(), ActivityFacility.class);
			Coord coord = link.getCoord();
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(id, coord);
			opportunities.addActivityFacility(facility);
		}
		
//		controler.addControlerListener(new GridBasedAccessibilityControlerListener(...)); 
		// (purely grid based controler listener does not yet exist)
		PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(network), mbConfig) ;
		// yyyy the following is taken from AccessibilityTest without any consideration of a good design.
		double cellSize = 100. ;

		GridBasedAccessibilityControlerListenerV3 gacl = new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, sc.getNetwork());
		// activating transport modes of interest
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			gacl.setComputingAccessibilityForMode(mode, true);
		}
//		gacl.setComputingAccessibilityForMode( Modes4Accessibility.pt, false ); 
		// not sure why this is "false"; presumably, the test is not configured. kai, feb'14
		
		// this will be called by the accessibility listener after the accessibility calculations are finished
		// It checks if the SpatialGrid for activated (true) transport modes are instantiated or null if not (false)
		EvaluateTestResults etr = new EvaluateTestResults(true, true, true, true, true);
		gacl.addSpatialGridDataExchangeListener(etr);
		
		// generating measuring points and SpatialGrids by using the bounding box
		gacl.generateGridsAndMeasuringPointsByNetwork(cellSize);

		controler.addControlerListener(gacl);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
		
		// compare some results -> done in EvaluateTestResults 
        
	}
	
	@Test
	public void testWithExtentDeterminedShapeFile() {
		
		Config config = ConfigUtils.createConfig();

//		URL url = AccessibilityIntegrationTest.class.getClassLoader().getResource(new File(this.utils.getInputDirectory()).getAbsolutePath() + "shapeFile2.shp");

		File f = new File(this.utils.getInputDirectory() + "shapefile.shp"); // shape file completely covers the road network

		if(!f.exists()){
			log.error("Shape file not found! testWithExtentDeterminedShapeFile could not be tested...");
			Assert.assertTrue(f.exists());
		}

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm);
		acm.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile.toString());
//		 acm.setShapeFileCellBasedAccessibility(url.getPath()); // yyyyyy todo
		acm.setShapeFileCellBasedAccessibility(f.getAbsolutePath());
		
		// modify config according to needs
		Network network = CreateTestNetwork.createTestNetwork();
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

		Controler controler = new Controler(config);
		ControlerConfigGroup controlerCG = config.controler();
		controlerCG.setLastIteration( 10);
		controlerCG.setOutputDirectory(utils.getOutputDirectory());
		
		final MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario( config);
		ScenarioUtils.loadScenario(sc);
		

		// creating test opportunities (facilities)
		ActivityFacilities opportunities = sc.getActivityFacilities();
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id<ActivityFacility> id = Id.create(link.getId(), ActivityFacility.class);
			Coord coord = link.getCoord();
			ActivityFacility facility = opportunities.getFactory().createActivityFacility(id, coord);
			opportunities.addActivityFacility(facility);
		}
		
		PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), BoundingBox.createBoundingBox(network), mbConfig) ;
//		controler.addControlerListener(new GridBasedAccessibilityControlerListener(...)); 
		// (purely grid based controler listener does not yet exist)
		// yyyy the following is taken from AccessibilityTest without any consideration of a good design.
		double cellSize = 100. ;

		GridBasedAccessibilityControlerListenerV3 gacl = new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, sc.getNetwork());
		// activating transport modes of interest
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			gacl.setComputingAccessibilityForMode(mode, true);
		}
//		gacl.setComputingAccessibilityForMode( Modes4Accessibility.pt, false ); 
		// not sure why this is "false"; presumably, the test is not configured. kai, feb'14
		
		// this will be called by the accessibility listener after the accessibility calculations are finished
		// It checks if the SpatialGrid for activated (true) transport modes are instantiated or null if not (false)
		EvaluateTestResults etr = new EvaluateTestResults(true, true, true, true, true);
		gacl.addSpatialGridDataExchangeListener(etr);
		
		// generating measuring points and SpatialGrids by using the bounding box
		gacl.generateGridsAndMeasuringPointsByShapeFile(acm.getShapeFileCellBasedAccessibility(), cellSize);

		controler.addControlerListener(gacl);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);
        controler.run();
        
		// compare some results -> done in EvaluateTestResults 
        
	}
	
	/**
	 * This is called by the GridBasedAccessibilityListener and 
	 * gets the resulting SpatialGrids. This test checks if the 
	 * SpatialGrids for activated transport modes (see above) are 
	 * instantiated or null if the specific transport mode is not 
	 * activated.
	 * 
	 * @author thomas
	 *
	 */
	public class EvaluateTestResults implements SpatialGridDataExchangeInterface{
		
		private Map<Modes4Accessibility,Boolean> isComputingMode = new HashMap<Modes4Accessibility,Boolean>();
		
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
			this.isComputingMode.put( Modes4Accessibility.freeSpeed, usingFreeSpeedGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.car, usingCarGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.bike, usingBikeGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.walk, usingWalkGrid ) ;
			this.isComputingMode.put( Modes4Accessibility.pt, usingPtGrid ) ;
		}
		
		/**
		 * This gets the resulting SpatialGrids from the GridBasedAccessibilityListener.
		 * - SpatialGrids for transport modes with "useXXXGrid=false"must be null
		 * - SpatialGrids for transport modes with "useXXXGrid=true"must not be null
		 * 
		 */
		@Override
		public void setAndProcessSpatialGrids( Map<Modes4Accessibility,SpatialGrid> spatialGrids ){
			
			log.info("Evaluating resuts ...");
			
			for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
				if ( this.isComputingMode.get(mode)) {
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
						expected.accessibilityPt = 0.461863556339195;
						
					} else if(x == 50 && y == 150){
						
						expected.accessibilityFreespeed = 2.1555292541877;
						expected.accessibilityCar = 2.1555292541877;
						expected.accessibilityBike = 2.20170415738971;
						expected.accessibilityWalk = 1.88907197432798;
						expected.accessibilityPt = 0.461863556339195;
						
					} else if(x == 150 && y == 150){
						
						expected.accessibilityFreespeed = 2.18445595855523;
						expected.accessibilityCar = 2.18445595855523;
						expected.accessibilityBike = 2.22089493905874;
						expected.accessibilityWalk = 1.9683225787191;
						expected.accessibilityPt = 0.624928280738513;
						
					}

					final AccessibilityResults actual = new AccessibilityResults();
					actual.accessibilityFreespeed = spatialGrids.get(Modes4Accessibility.freeSpeed).getValue(new Coord(x, y));
					actual.accessibilityCar = spatialGrids.get(Modes4Accessibility.car).getValue(new Coord(x, y));
					actual.accessibilityBike = spatialGrids.get(Modes4Accessibility.bike).getValue(new Coord(x, y));
					actual.accessibilityWalk = spatialGrids.get(Modes4Accessibility.walk).getValue(new Coord(x, y));
					actual.accessibilityPt = spatialGrids.get(Modes4Accessibility.pt).getValue(new Coord(x, y));

					Assert.assertTrue(
							"accessibility at coord " + x + "," + y + " does not match for "+
									expected.nonMatching( actual , MatsimTestUtils.EPSILON ),
							expected.equals(actual, MatsimTestUtils.EPSILON ) );
				}
				
			}
			
			log.info("... done!");
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
}
