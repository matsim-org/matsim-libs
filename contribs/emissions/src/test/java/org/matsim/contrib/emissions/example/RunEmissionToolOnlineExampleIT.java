/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.example;

import static org.junit.Assert.*;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.example.archive.RunEmissionToolOnlineExample;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

/**
 * @author nagel
 *
 */
public class RunEmissionToolOnlineExampleIT {

	/**
	 * Test method for {@link RunEmissionToolOnlineExampleV2#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		try {
			Config config = RunEmissionToolOnlineExampleV2.prepareConfig( null ) ;

			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			// otherwise the test fails on jenkins
			
			config.controler().setLastIteration( 1 );
			
			Scenario scenario = RunEmissionToolOnlineExampleV2.prepareScenario( config ) ;
			
			RunEmissionToolOnlineExampleV2.run( scenario ) ;

		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail("something did not work" ) ;
		}
	}

}
