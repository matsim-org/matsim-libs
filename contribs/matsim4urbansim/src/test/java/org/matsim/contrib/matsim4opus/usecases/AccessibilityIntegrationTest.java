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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.config.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.improvedPseudoPt.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class AccessibilityIntegrationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore
	public void testWithBoundingBox() {
		Config config = ConfigUtils.createConfig() ;

		final AccessibilityConfigGroup acm = new AccessibilityConfigGroup();
		config.addModule( acm ) ;
		acm.setUsingCustomBoundingBox(true) ;
		acm.setBoundingBoxBottom(0.) ;// yyyyyy todo
		acm.setBoundingBoxTop(10.) ;// yyyyyy todo
		acm.setBoundingBoxLeft(0.) ;// yyyyyy todo
		acm.setBoundingBoxRight(10.) ;// yyyyyy todo
		
		// modify config according to needs
		// ...
//		Network network = CreateTestNetwork.createTestNetwork();
//		new NetworkWriter(network).write(path+"network.xml");
//		
//		CreateTestMATSimConfig ctmc = new CreateTestMATSimConfig(path, path+"network.xml");
//		String configLocation = ctmc.generate();
//		
//		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4(configLocation);
//		connector.init();
//		config = connector.getConfig();
		
		
		Controler controler = new Controler(config) ;
		
		final ScenarioImpl sc = (ScenarioImpl)controler.getScenario();

		ActivityFacilitiesImpl opportunities = sc.getActivityFacilities() ;
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			Id id = sc.createId( link.getId().toString() ) ;
			Coord coord = link.getCoord() ;
			ActivityFacilityImpl fac = opportunities.createAndAddFacility(id, coord) ;
//			ActivityOptionImpl actOpt = fac.createActivityOption("work") ;
//			actOpt.setCapacity(1000.) ; // 1000 jobs at same facility
		}
		
		PtMatrix ptMatrix = null ;
		
		// yyyy the following is taken from AccessibilityTest without any consideration of a good design.
		double resolution = 100. ;
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		double[] boundary = NetworkUtils.getBoundingBox(sc.getNetwork().getNodes().values());
		
		double minX = boundary[0]-resolution/2;
		double minY = boundary[1]-resolution/2;
		double maxX = boundary[2]+resolution/2;
		double maxY = boundary[3]+resolution/2;

		if(resolution>100){
			
			minX = boundary[0] - 150*(2*resolution/200-1);
			minY = boundary[1] - 150*(2*resolution/200-1);
			maxX = boundary[2] + 150*(2*resolution/200+1);
			maxY = boundary[3] + 150*(2*resolution/200+1);
			
		}
		
		bbox.setCustomBoundaryBox(minX,minY,maxX,maxY);
		ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(resolution, bbox.getXMin(), bbox.getYMin(), bbox.getYMax(), bbox.getYMax());
		
		SpatialGrid gridForFreeSpeedResults = new SpatialGrid(bbox.getBoundingBox(), resolution);
		SpatialGrid gridForCarResults = new SpatialGrid(gridForFreeSpeedResults);
		SpatialGrid gridForBikeResults = new SpatialGrid(gridForFreeSpeedResults);
		SpatialGrid gridForWalkResults = new SpatialGrid(gridForFreeSpeedResults);
		SpatialGrid gridForPtResults = new SpatialGrid(gridForFreeSpeedResults);

		
		controler.addControlerListener(new GridBasedAccessibilityControlerListenerV3(measuringPoints, opportunities, gridForFreeSpeedResults, 
				gridForCarResults,	gridForBikeResults, gridForWalkResults, gridForPtResults, ptMatrix, config, sc.getNetwork())); 
		
		// yy the correct test is essentially already in AccessibilityTest.testAccessibilityMeasure().  kai, jun'13
		// But that test uses the matsim4urbansim setup, which we don't want to use in the present test.
		
		controler.run();
		
		// compare some results
		// ...
		
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
	
	
	
}
