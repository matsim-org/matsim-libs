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
package org.matsim.contrib.improvedPseudoPt.config;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;

/**
 * @author nagel
 *
 */
public class ImprovedPseudoPtConfigUtils {
	
	private static final Logger log = Logger.getLogger(ImprovedPseudoPtConfigUtils.class);
	
	private ImprovedPseudoPtConfigUtils() {} // container for static methods; do not instantiate

	public static ImprovedPseudoPtConfigGroup getConfigModuleAndPossiblyConvert(Config config) {
		Module m = config.getModule(ImprovedPseudoPtConfigGroup.GROUP_NAME);
		if (m instanceof ImprovedPseudoPtConfigGroup) {
			return (ImprovedPseudoPtConfigGroup) m;
		}
		
		ImprovedPseudoPtConfigGroup ippcm = new ImprovedPseudoPtConfigGroup();
		config.addModule( ImprovedPseudoPtConfigGroup.GROUP_NAME, ippcm ) ;
		return ippcm;
	}	
	
	/**
	 * Setting improved pseudo pt into MATSim Config
	 * 
	 * @param matsim4urbansimModule Module containing pt settings
	 * @param config MATSim Config object
	 */
	public static void initImprovedPseudoPtParameter(Module matsim4urbansimModule, Config config){
		
		log.info("Checking improved pseudo pt settings ...");
		ImprovedPseudoPtConfigGroup ippcm = ImprovedPseudoPtConfigUtils.getConfigModuleAndPossiblyConvert(config) ;
		
		if(matsim4urbansimModule != null){
			String usePtStops = matsim4urbansimModule.getValue(ImprovedPseudoPtConfigGroup.PT_STOPS_SWITCH);
			String ptStops = matsim4urbansimModule.getValue(ImprovedPseudoPtConfigGroup.PT_STOPS);
			String useTravelTimesAndDistances =  matsim4urbansimModule.getValue(ImprovedPseudoPtConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH);
			String ptTravelTimes =  matsim4urbansimModule.getValue(ImprovedPseudoPtConfigGroup.PT_TRAVEL_TIMES);
			String ptTravelDistances =  matsim4urbansimModule.getValue(ImprovedPseudoPtConfigGroup.PT_TRAVEL_DISTANCES);

			if(usePtStops != null &&  usePtStops.equalsIgnoreCase("true")){
				log.info(ImprovedPseudoPtConfigGroup.PT_STOPS_SWITCH + " switch is set to true. Trying to find pt stops file ...");
				// checking for pt stops
				if(ptStops != null){
					File ptStopsFile = new File(ptStops);
					if(ptStopsFile.exists()){
						log.info("Found pt stops file " + ptStops);
						ippcm.setUsingPtStops(true);
						ippcm.setPtStopsInputFile(ptStops);
					}
					else{
						log.warn("Pt stops file " + ptStops + " not found! Improved pseudo pt will not be initialized!");
						ippcm.setUsingPtStops(false);
					}
					
					// checking for other input files
					if(useTravelTimesAndDistances != null && useTravelTimesAndDistances.equalsIgnoreCase("true")){
						log.info(ImprovedPseudoPtConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " switch is set to true. Trying to find travel times and distances files ...");
						
						File ptTravelTimesFile = new File(ptTravelTimes);
						File ptTravelDistancesFile = new File(ptTravelDistances); 
						
						if(ptTravelTimesFile.exists() && ptTravelDistancesFile.exists()){
							log.info("Found travel times and travel distances input files:");
							log.info("Travel times input file: " + ptTravelTimes);
							log.info("Travel distances input file: " + ptTravelDistances);
							ippcm.setUsingTravelTimesAndDistances(true);
							ippcm.setPtTravelTimesInputFile(ptTravelTimes);
							ippcm.setPtTravelDistancesInputFile(ptTravelDistances);
							
						}
						else{
							log.warn("Travel times and travel distances input files not found!");
							log.warn("Travel times input file: " + ptTravelTimes);
							log.warn("Travel distances input file: " + ptTravelDistances);
							ippcm.setUsingTravelTimesAndDistances(false);
						}
					}
					else
						log.info(ImprovedPseudoPtConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " switch is set to false. Additional travel times and distances files will not be read!");
					
				}
				else
					log.warn("No pt stops file given. Improved pseudo pt will not be initialized!");
			}
			else
				log.info(ImprovedPseudoPtConfigGroup.PT_STOPS_SWITCH + " switch is set to false. Improved pseudo pt will not be initialized.");
		}
	}
	
}
