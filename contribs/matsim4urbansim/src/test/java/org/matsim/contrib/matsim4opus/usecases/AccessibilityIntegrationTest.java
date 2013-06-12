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
package org.matsim.contrib.matsim4opus.usecases;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.config.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.improvedPseudoPt.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.CreateTestNetwork;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class AccessibilityIntegrationTest {
	
	// logger
	private static final Logger log = Logger.getLogger(AccessibilityIntegrationTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWithBoundingBox() {
		Config config = ConfigUtils.createConfig() ;

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm ) ;
		acm.setUsingCustomBoundingBox(true) ;
		acm.setBoundingBoxBottom(0.) ;// yyyyyy todo
		acm.setBoundingBoxTop(100.) ;// yyyyyy todo
		acm.setBoundingBoxLeft(0.) ;// yyyyyy todo
		acm.setBoundingBoxRight(100.) ;// yyyyyy todo
		
		// modify config according to needs
		Network network = CreateTestNetwork.createTestNetwork();
		String networkFile = utils.getOutputDirectory() +"network.xml";
		new NetworkWriter(network).write(networkFile);
		config.network().setInputFile( networkFile );

		Controler controler = new Controler(config) ;
		ControlerConfigGroup controlerCG = config.controler() ;
		controlerCG.setLastIteration( 10 );
		controlerCG.setOutputDirectory(utils.getOutputDirectory());
		
		final ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario( config );
		// final ScenarioImpl sc = (ScenarioImpl)controler.getScenario();
		// sc.setNetwork(network);
		ScenarioUtils.loadScenario(sc);

		
		
		// creating test opportunities (facilities)
		ActivityFacilitiesImpl opportunities = sc.getActivityFacilities() ;
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id id = sc.createId( link.getId().toString() ) ;
			Coord coord = link.getCoord() ;
			opportunities.createAndAddFacility(id, coord) ;
		}
		
		PtMatrix ptMatrix = null ;
		// yyyy the following is taken from AccessibilityTest without any consideration of a good design.
		double cellSize = 100. ;

		GridBasedAccessibilityControlerListenerV3 gacl = new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, sc.getNetwork());
		// activating transport modes of interest
		// possible modes are: free speed and congested car,
		// 					   bicycle, walk and pt
		gacl.useFreeSpeedGrid();
		gacl.useCarGrid();
		gacl.useBikeGrid();
		gacl.useWalkGrid();
//		gacl.usePtGrid();
		
		// this will be called by the accessibility listener
		EvaluateTestResults etr = new EvaluateTestResults(true, true, true, true, false);
		gacl.addSpatialGridDataExchangeListener(etr);
		
		// generating measuring points and SpatialGrids by using the bounding box
		gacl.generateGridsAndMeasuringPointsByCustomBoundary(acm.getBoundingBoxLeft(), acm.getBoundingBoxBottom(), acm.getBoundingBoxRight(), acm.getBoundingBoxTop(), cellSize);
		
		controler.addControlerListener(gacl); 
		
		// yy the correct test is essentially already in AccessibilityTest.testAccessibilityMeasure().  kai, jun'13
		// But that test uses the matsim4urbansim setup, which we don't want to use in the present test.
		
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
		
		// compare some results -> done in EvaluateTestResults (see above)
	}
	
	@Test
	@Ignore
	public void testWithExtentDeterminedByNetwork() {
		Config config = ConfigUtils.createConfig() ;

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm ) ;
		acm.setCellBasedAccessibilityNetwork(true) ;
		
		// modify config according to needs
		// ...
		
		
		Controler controler = new Controler(config) ;
		
		final ScenarioImpl sc = (ScenarioImpl)controler.getScenario();

		ActivityFacilitiesImpl facilities = sc.getActivityFacilities() ;
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id id = sc.createId( link.getId().toString() ) ;
			Coord coord = link.getCoord() ;
			ActivityFacilityImpl fac = facilities.createAndAddFacility(id, coord) ;
//			ActivityOptionImpl actOpt = fac.createActivityOption("work") ;
//			actOpt.setCapacity(1000.) ; // 1000 jobs at same facility
		}
		
//		controler.addControlerListener(new GridBasedAccessibilityControlerListener(...)); 
		// (purely grid based controler listener does not yet exist)
		
		controler.run();
		
		// compare some results
		// ...
		
	}
	
	
	@Test
	@Ignore
	public void testWithExtentDeterminedShapeFile() {
		Config config = ConfigUtils.createConfig() ;

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm ) ;
		acm.setCellBasedAccessibilityShapeFile(true);
		acm.setShapeFileCellBasedAccessibility("") ; // yyyyyy todo
		
		// modify config according to needs
		// ...
		
		
		Controler controler = new Controler(config) ;
		
		final ScenarioImpl sc = (ScenarioImpl)controler.getScenario();

		ActivityFacilitiesImpl facilities = sc.getActivityFacilities() ;
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id id = sc.createId( link.getId().toString() ) ;
			Coord coord = link.getCoord() ;
			ActivityFacilityImpl fac = facilities.createAndAddFacility(id, coord) ;
//			ActivityOptionImpl actOpt = fac.createActivityOption("work") ;
//			actOpt.setCapacity(1000.) ; // 1000 jobs at same facility
		}
		
//		controler.addControlerListener(new GridBasedAccessibilityControlerListener(...)); 
		// (purely grid based controler listener does not yet exist)
		
		controler.run();
		
		// compare some results
		// ...
		
	}
	
	public class EvaluateTestResults implements SpatialGridDataExchangeInterface{
		
		private boolean usingFreeSpeedGrid, usingCarGrid, usingBikeGrid, usingWalkGrid, usingPtGrid;
		
		public EvaluateTestResults(boolean usingFreeSpeedGrid, boolean usingCarGrid, boolean usingBikeGrid, boolean usingWalkGrid, boolean usingPtGrid){
			this.usingFreeSpeedGrid = usingFreeSpeedGrid;
			this.usingCarGrid = usingCarGrid;
			this.usingBikeGrid = usingBikeGrid;
			this.usingWalkGrid = usingWalkGrid;
			this.usingPtGrid = usingPtGrid;
		}
		
		public void getAndProcessSpatialGrids(SpatialGrid freeSpeedGrid, SpatialGrid carGrid, SpatialGrid bikeGrid, SpatialGrid walkGrid, SpatialGrid ptGrid){
			
			log.info("Evaluating resuts ...");
			
			if(this.usingFreeSpeedGrid)
				Assert.assertTrue( freeSpeedGrid != null );
			else
				Assert.assertTrue( freeSpeedGrid == null );
			
			if(this.usingCarGrid)
				Assert.assertTrue( carGrid != null );
			else
				Assert.assertTrue( carGrid == null );
			
			if(this.usingBikeGrid)
				Assert.assertTrue( bikeGrid != null );
			else
				Assert.assertTrue( bikeGrid == null );
			
			if(this.usingWalkGrid)
				Assert.assertTrue( walkGrid != null );
			else
				Assert.assertTrue( walkGrid == null );
			
			if(this.usingPtGrid)
				Assert.assertTrue( ptGrid != null );
			else
				Assert.assertTrue( ptGrid == null );
			
			log.info("... done!");
		}
	}
}
