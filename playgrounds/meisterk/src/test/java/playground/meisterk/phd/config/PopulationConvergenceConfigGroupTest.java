/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationConvergenceConfigGroupTest.java
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

package playground.meisterk.phd.config;


import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.testcases.MatsimTestCase;

public class PopulationConvergenceConfigGroupTest extends MatsimTestCase {


	public void testopulationConvergenceConfigGroup() {
		
		PopulationConvergenceConfigGroup testee = new PopulationConvergenceConfigGroup();
		
		assertEquals( 
				Double.parseDouble(PopulationConvergenceConfigGroup.PopulationConvergenceConfigParameter.ALPHA_SELECTED.getDefaultValue()), 
				testee.getAlphaSelected());
	}

	public void testAddParam() {

		Config config = new Config();
		PopulationConvergenceConfigGroup testee = new PopulationConvergenceConfigGroup();
		config.addModule(PopulationConvergenceConfigGroup.GROUP_NAME, testee);
		new MatsimConfigReader(config).readFile(this.getInputDirectory() + "config.xml", null);

		assertEquals( 0.567, testee.getAlphaSelected() );
	}

}
