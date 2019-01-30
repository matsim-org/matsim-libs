/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.raptor;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.minibus.RunMinibus;
import org.matsim.testcases.MatsimTestUtils;

public class RunMinibusExampleRaptorTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRunScenarioWithRaptor() {
		String[] args = {"test/input/org/matsim/contrib/minibus/example-scenario/config_raptor_minibus.xml"};

		final RunMinibus minibus = new RunMinibus(args);

		minibus.getConfig().controler().setLastIteration(1);
		minibus.getConfig().controler().setOutputDirectory(utils.getOutputDirectory());

		minibus.run();
	}

}
