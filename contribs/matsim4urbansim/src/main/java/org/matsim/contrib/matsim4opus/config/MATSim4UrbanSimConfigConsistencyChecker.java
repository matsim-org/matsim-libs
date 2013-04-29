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

import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

/**
 * @author nagel
 *
 */
public class MATSim4UrbanSimConfigConsistencyChecker implements ConfigConsistencyChecker {

	@Override
	public void checkConsistency(Config config) {
		boolean problem = false ;
		
		MATSim4UrbanSimControlerConfigModuleV3 matsim4urbansimModule = (MATSim4UrbanSimControlerConfigModuleV3) config.getModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME) ;
		UrbanSimParameterConfigModuleV3 urbansimParameterModule = (UrbanSimParameterConfigModuleV3) config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME ) ;
		AccessibilityParameterConfigModule accessibilityParameterModule = (AccessibilityParameterConfigModule) config.getModule(AccessibilityParameterConfigModule.GROUP_NAME) ;
		
		if ( problem ) {
			throw new RuntimeException("serious problem in MATSim4UrbanSimConfigConsistencyChecker; aborting ...") ;
		}
	}

}
