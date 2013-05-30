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
package org.matsim.contrib.matsim4opus.usecases;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.matsim4opus.config.modules.AccessibilityConfigModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class AccessibilityIntegrationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore
	public void testIntegration() {
		Config config = ConfigUtils.createConfig() ;
		config.addModule( new AccessibilityConfigModule() ) ;
		
		// modify config according to needs
		// ...
		
		
		Controler controler = new Controler(config) ;
		
//		controler.addControlerListener(new GridBasedAccessibilityControlerListener(...)); 
		// (purely grid based controler listener does not yet exist)
		
		controler.run();
		
		// compare some results
		// ...
		
	}
}
