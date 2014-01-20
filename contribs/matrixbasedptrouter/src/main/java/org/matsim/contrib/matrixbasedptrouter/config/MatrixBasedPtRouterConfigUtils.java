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
package org.matsim.contrib.matrixbasedptrouter.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;

/**
 * @author nagel
 *
 */
public class MatrixBasedPtRouterConfigUtils {
	
//	private static final Logger log = Logger.getLogger(MatrixBasedPtRouterConfigUtils.class);
	
	private MatrixBasedPtRouterConfigUtils() {} // container for static methods; do not instantiate

	// yy this could go into the config group then this class could be completely removed. kai, jul'13
	public static MatrixBasedPtRouterConfigGroup getConfigModuleAndPossiblyConvert(Config config) {
		Module m = config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		if (m instanceof MatrixBasedPtRouterConfigGroup) {
			return (MatrixBasedPtRouterConfigGroup) m;
		}
		
		MatrixBasedPtRouterConfigGroup ippcm = new MatrixBasedPtRouterConfigGroup();

		config.addModule( ippcm ) ;
		// does not/no longer seem to work if called too late.  no idea why. kai, jan'14
		
		return ippcm;
	}	
	
	// moving the material to the config consistency checker.  kai, jul'13
//	/**
//	 * Setting improved pseudo pt into MATSim Config
//	 * 
//	 * @param infoFromExternalConfig Module containing pt settings
//	 * @param config MATSim Config object
//	 */
//	public static void initMatrixBasedPtRouterParameters(Module infoFromExternalConfig, Config config){
//		
//		log.info("Checking improved pseudo pt settings ...");
//		MatrixBasedPtRouterConfigGroup ippcm = MatrixBasedPtRouterConfigUtils.getConfigModuleAndPossiblyConvert(config) ;
//		
//		if(infoFromExternalConfig != null){
//			String usePtStops = infoFromExternalConfig.getValue(MatrixBasedPtRouterConfigGroup.PT_STOPS_SWITCH);
//			String ptStops = infoFromExternalConfig.getValue(MatrixBasedPtRouterConfigGroup.PT_STOPS);
//			String useTravelTimesAndDistances =  infoFromExternalConfig.getValue(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH);
//			String ptTravelTimes =  infoFromExternalConfig.getValue(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES);
//			String ptTravelDistances =  infoFromExternalConfig.getValue(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_DISTANCES);
//
//			if(usePtStops != null &&  usePtStops.equalsIgnoreCase("true")){
//				log.info(MatrixBasedPtRouterConfigGroup.PT_STOPS_SWITCH + " switch is set to true. Trying to find pt stops file ...");
//				// checking for pt stops
//				if(ptStops != null){
//					File ptStopsFile = new File(ptStops);
//					if(ptStopsFile.exists()){
//						log.info("Found pt stops file " + ptStops);
//						ippcm.setUsingPtStops(true);
//						ippcm.setPtStopsInputFile(ptStops);
//					}
//					else{
//						log.warn("Pt stops file " + ptStops + " not found! Improved pseudo pt will not be initialized!");
//						ippcm.setUsingPtStops(false);
//					}
//					
//					// checking for other input files
//					if(useTravelTimesAndDistances != null && useTravelTimesAndDistances.equalsIgnoreCase("true")){
//						log.info(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " switch is set to true. Trying to find travel times and distances files ...");
//						
//						File ptTravelTimesFile = new File(ptTravelTimes);
//						File ptTravelDistancesFile = new File(ptTravelDistances); 
//						
//						if(ptTravelTimesFile.exists() && ptTravelDistancesFile.exists()){
//							log.info("Found travel times and travel distances input files:");
//							log.info("Travel times input file: " + ptTravelTimes);
//							log.info("Travel distances input file: " + ptTravelDistances);
//							ippcm.setUsingTravelTimesAndDistances(true);
//							ippcm.setPtTravelTimesInputFile(ptTravelTimes);
//							ippcm.setPtTravelDistancesInputFile(ptTravelDistances);
//							
//						}
//						else{
//							log.warn("Travel times and travel distances input files not found!");
//							log.warn("Travel times input file: " + ptTravelTimes);
//							log.warn("Travel distances input file: " + ptTravelDistances);
//							ippcm.setUsingTravelTimesAndDistances(false);
//						}
//					}
//					else
//						log.info(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " switch is set to false. Additional travel times and distances files will not be read!");
//					
//				}
//				else
//					log.warn("No pt stops file given. Improved pseudo pt will not be initialized!");
//			}
//			else
//				log.info(MatrixBasedPtRouterConfigGroup.PT_STOPS_SWITCH + " switch is set to false. Improved pseudo pt will not be initialized.");
//		}
//	}
	
}
