/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.extensions.roadpricing;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.roadpricing.RunRoadPricingExample;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author vsp-gleich
 *
 */
public class RoadPricingByConfigfileTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	/**
	 * Test method for {@link RunRoadPricingExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		RunRoadPricingExample matsim = new RunRoadPricingExample( new String[]{"scenarios/equil-extended/config-with-roadpricing.xml"} );

		Config config = matsim.prepareConfig();

		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 5 );

		matsim.run();
	}

}
