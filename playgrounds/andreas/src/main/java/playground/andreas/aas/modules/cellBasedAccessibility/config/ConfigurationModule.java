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

package playground.andreas.aas.modules.cellBasedAccessibility.config;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;

public class ConfigurationModule {
	
	public static AccessibilityParameterConfigModule getAccessibilityParameterConfigModule(ScenarioImpl scenario){
		ConfigGroup m = scenario.getConfig().getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		return null;
	}

	public static MATSim4UrbanSimControlerConfigModule getMATSim4UrbaSimControlerConfigModule(ScenarioImpl scenario){
		ConfigGroup m = scenario.getConfig().getModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModule) {
			return (MATSim4UrbanSimControlerConfigModule) m;
		}
		return null;
	}
	
	public static UrbanSimParameterConfigModule getUrbanSimParameterConfigModule(ScenarioImpl scenario){
		ConfigGroup m = scenario.getConfig().getModule(UrbanSimParameterConfigModule.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModule) {
			return (UrbanSimParameterConfigModule) m;
		}
		return null;
	}
}
