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
package org.matsim.contrib.accessibility.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;

/**
 * @author nagel
 *
 */
public class M4UAccessibilityConfigUtils {
	private M4UAccessibilityConfigUtils() {} // static methods only
	
	// This used to copy params from urbansim into the matsim config. But it is not doing this any more.  kai,jul'13
//	public static void initAccessibilityParameters(Config config){
//	
//		AccessibilityConfigGroup module = getConfigModuleAndPossiblyConvert(config);
//	
//		// accessibility destination sampling rate:
//		double accessibilityDestinationSamplingRate = 1.; // matsim4UrbanSimParamsFromU.getAccessibilityParameter().getAccessibilityDestinationSamplingRate();
//		// (maybe leave in UrbanSim)
//		if ( module.getAccessibilityDestinationSamplingRate() != null ) {
//			accessibilityDestinationSamplingRate = module.getAccessibilityDestinationSamplingRate() ;
//		}
//		module.setAccessibilityDestinationSamplingRate(accessibilityDestinationSamplingRate);
//
//		// raw sums?
//		// (leave in urbansim)
//		boolean useRawSum						= false; // matsim4UrbanSimParamsFromU.getAccessibilityParameter().isUseRawSumsWithoutLn();
//		if ( module.isUsingRawSumsWithoutLn() != null ) {
//			useRawSum = module.isUsingRawSumsWithoutLn() ;
//		}
//		module.setUsingRawSumsWithoutLn(useRawSum);
//
//	}

	public static AccessibilityConfigGroup getConfigModuleAndPossiblyConvert(Config config) {
		Module m = config.getModule(AccessibilityConfigGroup.GROUP_NAME);
		if (m instanceof AccessibilityConfigGroup) {
			return (AccessibilityConfigGroup) m;
		}
		AccessibilityConfigGroup module = new AccessibilityConfigGroup();
		//		config.getModules().put(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME, mccm);
		// yyyyyy the above code does NOT convert but throws the config entries away.
		// In contrast, config.addModule(...) would convert.  kai, may'13
		// I just changed that:
		config.addModule(AccessibilityConfigGroup.GROUP_NAME, module ) ;
		return module;
	}


}
