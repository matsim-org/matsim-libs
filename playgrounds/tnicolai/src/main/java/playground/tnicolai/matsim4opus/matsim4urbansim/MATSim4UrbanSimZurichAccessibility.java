
/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimERSA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.scenario.ZurichUtilitiesOSMNetwork;
import playground.tnicolai.matsim4opus.scenario.ZurichUtilitiesZurichBigRoads;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;
import playground.tnicolai.matsim4opus.utils.io.Paths;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author thomas
 *
 */
class MATSim4UrbanSimZurichAccessibility extends MATSim4UrbanSimV2{
	
	// Logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimZurichAccessibility.class);
	
	private String shapeFile = null;
	private double gridSizeInMeter = -1;
	double jobSample = 1.;
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimZurichAccessibility(String args[]){
		super(args);
		// set the resolution, this is used for setting 
		// the starting points for accessibility measures
		checkAndSetShapeFile(args);
		checkAndSetGridSize(args);
		checkAndSetJobSample(args);
	}

	/**
	 * Set the shape file path in order to determine 
	 * the starting points for accessibility computation
	 * 
	 * @param args
	 */
	private void checkAndSetShapeFile(String[] args) {

		if( args.length >= 2 ){
			shapeFile = args[1].trim();
			log.info("The shape file path was set to " + shapeFile);
			if(!Paths.pathExsits(shapeFile))
				throw new RuntimeException("Given path to shape file does not exist: " + shapeFile);
		} else{
			log.error("Missing shape file!!!");
			System.exit(-1);
		}
	}
	
	/**
	 * Set the grid size for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetGridSize(String[] args) {
		
		if(args.length >= 3){
			gridSizeInMeter = Double.parseDouble( args[2].trim() );
			log.info("The resolution was set to " + String.valueOf(gridSizeInMeter) );
		} else{
			log.error("Missing resolution!!!");
			System.exit(-1);
		}
	}
	
	/**
	 * Set the jobSample for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetJobSample(String[] args) {

		if (args.length >= 4) {
			jobSample = Double.parseDouble(args[3].trim());
			log.info("The jobSample was set to " + String.valueOf(jobSample));
		} else {
			log.error("Missing jobSample!!!");
			System.exit(-1);
		}
	}
	
	/**
	 * This modifies the MATSim network according to the given
	 * test parameter in the MATSim config file (from UrbanSim)
	 */
	@Override
	void modifyNetwork(Network network){
		log.info("");
		log.info("Checking for network modifications ...");
		// check given test parameter for desired modifications
		String testParameter = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TEST_PARAMETER_PARAM);
		if(testParameter.equals("")){
			log.info("No modifications to perform.");
			log.info("");
			return;
		}
		else{
			String scenarioArray[] = testParameter.split(",");
			// ZurichUtilitiesZurichBigRoads.modifyNetwork(network, scenarioArray);
			ZurichUtilitiesOSMNetwork.modifyNetwork(network, scenarioArray);
			log.info("Done modifying network.");
			log.info("");
		}
	}
	
	/**
	 * This removes plan elements from existing plans that
	 * contain a removed link
	 */
	@Override
	void modifyPopulation(Population population){
		// ZurichUtilitiesZurichBigRoads.deleteRoutesContainingRemovedLinks(population);
		ZurichUtilitiesOSMNetwork.deleteRoutesContainingRemovedLinks(population);
	}
	
	/**
	 * This method allows to add additional listener
	 * to the super class
	 */
	@Override
	void addFurtherControlerListener(Controler controler, ActivityFacilitiesImpl parcels){
		
		// tnicolai: make this configurable 
		boolean computeGridBasedAccessibilities = true;
		
		if(computeGridBasedAccessibilities){
			
			Geometry boundary = GridUtils.getBoundary(shapeFile);
			ZoneLayer<ZoneAccessibilityObject> startZones = GridUtils.createGridLayerByGridSize(gridSizeInMeter, boundary);
			
			SpatialGrid<Double> travelTimeAccessibilityGrid = GridUtils.createSpatialGrid(gridSizeInMeter, boundary);
			SpatialGrid<Double> travelCostAccessibilityGrid = GridUtils.createSpatialGrid(gridSizeInMeter, boundary);
			SpatialGrid<Double> travelDistanceAccessibilityGrid = GridUtils.createSpatialGrid(gridSizeInMeter, boundary);
			
			controler.addControlerListener( new ERSAControlerListener(startZones, 
																	  readUrbansimJobs(parcels, jobSample),
																	  travelTimeAccessibilityGrid, 
																	  travelCostAccessibilityGrid, 
																	  travelDistanceAccessibilityGrid, 
																	  benchmark) );
		}
	}
	
	/**
	 * This is the program entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		long startTime;
		long endTime;
		long time;
		
		startTime = System.currentTimeMillis();
		
		MATSim4UrbanSimZurichAccessibility m4uZurich = new MATSim4UrbanSimZurichAccessibility(args);
		m4uZurich.runMATSim();
		
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 60000;
		
		log.info("Computation took " + time + " minutes. Computation done!");
	}

}

