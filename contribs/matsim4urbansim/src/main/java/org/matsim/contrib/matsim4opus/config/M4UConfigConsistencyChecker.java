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
package org.matsim.contrib.matsim4opus.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
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
		
		M4UControlerConfigModuleV3 matsim4urbansimModule = (M4UControlerConfigModuleV3) config.getModule(M4UControlerConfigModuleV3.GROUP_NAME) ;
		UrbanSimParameterConfigModuleV3 urbansimParameterModule = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME ) ;
		AccessibilityConfigModule accessibilityConfigModule = (AccessibilityConfigModule) config.getModule(AccessibilityConfigModule.GROUP_NAME) ;
		
		if ( accessibilityConfigModule.getLogitScaleParameter() != 1. ) {
			log.warn("using a logit scale parameter != 1. for accessibility computation.  Not recommended; will make interpretation of results more indirect") ;
		}
		if ( !accessibilityConfigModule.isUsingCarParametersFromMATSim() ) {
			problem = true ;
			log.error("using car beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
		}
		if ( !accessibilityConfigModule.isUsingPtParametersFromMATSim() ) {
			problem = true ;
			log.error("using pt beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
		}
		if ( !accessibilityConfigModule.isUsingWalkParametersFromMATSim() ) {
			problem = true ;
			log.error("using walk beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
		}
		if ( !accessibilityConfigModule.isUsingBikeParametersFromMATSim() ) {
			problem = true ;
			log.error("using bike beta parameters not from matsim currently not allowed since interpretation not fully clear") ;
		}
		
		if ( problem ) {
			throw new RuntimeException("serious problem in MATSim4UrbanSimConfigConsistencyChecker; aborting ...") ;
		}
	}

}
