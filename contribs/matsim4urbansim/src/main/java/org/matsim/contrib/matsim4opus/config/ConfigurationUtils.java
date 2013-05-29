/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.matsim4opus.config;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Module;

public class ConfigurationUtils {
	
	public static AccessibilityConfigModule getAccessibilityParameterConfigModule(Scenario scenario){
		Module m = scenario.getConfig().getModule(AccessibilityConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityConfigModule) {
			return (AccessibilityConfigModule) m;
		}
		return null;
	}

	public static MATSim4UrbanSimControlerConfigModuleV3 getMATSim4UrbaSimControlerConfigModule(Scenario scenario){
		Module m = scenario.getConfig().getModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModuleV3) {
			return (MATSim4UrbanSimControlerConfigModuleV3) m;
		}
		return null;
	}
	
	public static UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfigModule(Scenario scenario){
		Module m = scenario.getConfig().getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		return null;
	}
}
