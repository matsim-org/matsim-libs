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

import org.matsim.contrib.matsim4opus.config.modules.ImprovedPseudoPtConfigModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;

/**
 * @author nagel
 *
 */
public class M4UImprovedPseudoPtConfigUtils {
	private M4UImprovedPseudoPtConfigUtils() {} // container for static methods; do not instantiate

	public static ImprovedPseudoPtConfigModule getConfigModuleAndPossiblyConvert(Config config) {
		Module m = config.getModule(ImprovedPseudoPtConfigModule.GROUP_NAME);
		if (m instanceof ImprovedPseudoPtConfigModule) {
			return (ImprovedPseudoPtConfigModule) m;
		}
		
		ImprovedPseudoPtConfigModule ippcm = new ImprovedPseudoPtConfigModule();
		config.addModule( ImprovedPseudoPtConfigModule.GROUP_NAME, ippcm ) ;
		return ippcm;
	}	
}
