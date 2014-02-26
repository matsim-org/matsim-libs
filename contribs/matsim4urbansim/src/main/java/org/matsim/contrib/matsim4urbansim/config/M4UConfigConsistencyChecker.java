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
package org.matsim.contrib.matsim4urbansim.config;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matsim4urbansim.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.utils.io.Paths;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

/**
 * @author nagel
 *
 */
public class M4UConfigConsistencyChecker implements ConfigConsistencyChecker {
	private static final Logger log = Logger.getLogger(M4UConfigConsistencyChecker.class);

	@Override
	public void checkConsistency(Config config) {
		boolean problem = false ;
		
		@SuppressWarnings("unused")
		M4UControlerConfigModuleV3 matsim4urbansimModule = (M4UControlerConfigModuleV3) config.getModule(M4UControlerConfigModuleV3.GROUP_NAME) ;
		@SuppressWarnings("unused")
		UrbanSimParameterConfigModuleV3 urbansimParameterModule = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME ) ;
		@SuppressWarnings("unused")
		AccessibilityConfigGroup accessibilityConfigModule = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class) ;
		MatrixBasedPtRouterConfigGroup ippcm = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.GROUP_NAME, MatrixBasedPtRouterConfigGroup.class) ;
		
		if ( ippcm.isUsingTravelTimesAndDistances() ) {
			if ( !ippcm.isUsingPtStops() ) {
			problem = true ;
			log.error("As far as I understand, improved pseudo pt will not work when pt stops are switched off.  There is no obvious conceptual " +
					"reason for this; it is just how it is currently implemented.  Either switch off " 
					+ MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " or switch on " 
					+ MatrixBasedPtRouterConfigGroup.PT_STOPS + " plus provide a pt stops file.  Aborting ...") ;
			}
		}
		
		if ( ippcm.isUsingPtStops() ) {
			if ( !ippcm.getPtStopsInputFile().isEmpty() && !Paths.pathExsits( ippcm.getPtStopsInputFile() )) {
				problem = true ;
				log.error( MatrixBasedPtRouterConfigGroup.USING_PT_STOPS + " is set to true but pt stops file not found.  Aborting ... ") ;
			}
			
			if ( ippcm.isUsingTravelTimesAndDistances() ) {
				if ( !ippcm.getPtTravelTimesInputFile().isEmpty() && !Paths.pathExsits( ippcm.getPtTravelTimesInputFile() )) {
					problem = true ;
					log.error( MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " is set to true but pt travel times file not found.  Aborting ... ") ;
				}
				if ( !ippcm.getPtTravelDistancesInputFile().isEmpty() && !Paths.pathExsits( ippcm.getPtTravelDistancesInputFile() )) {
					problem = true ;
					log.error( MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH + " is set to true but pt distances file not found.  Aborting ... ") ;
				}
			}
		}

		if ( config.network().getInputFile().isEmpty() ) {
			problem = true ;
			log.error("Missing MATSim network! The network must be specified either directly in the " +
					"MATSim4UrbanSim configuration or in an external MATSim configuration.");
		}
		
//		if ( accessibilityConfigModule.getLogitScaleParameter() != 1. ) {
//			log.warn("using a logit scale parameter != 1. for accessibility computation.  Not recommended; will make interpretation of results more indirect") ;
//		}
//		if ( !accessibilityConfigModule.isUsingCarParametersFromMATSim() ) {
//			problem = true ;
//			log.error("using car beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
//		}
//		if ( !accessibilityConfigModule.isUsingPtParametersFromMATSim() ) {
//			problem = true ;
//			log.error("using pt beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
//		}
//		if ( !accessibilityConfigModule.isUsingWalkParametersFromMATSim() ) {
//			problem = true ;
//			log.error("using walk beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
//		}
//		if ( !accessibilityConfigModule.isUsingBikeParametersFromMATSim() ) {
//			problem = true ;
//			log.error("using bike beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
//		}
		
		if ( problem ) {
			throw new RuntimeException("serious problem in MATSim4UrbanSimConfigConsistencyChecker; aborting ...") ;
		}
	}

}
