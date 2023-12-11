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
package org.matsim.contrib.parking.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.parking.parkingchoice.run.RunParkingChoiceExample;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunParkingChoiceExampleIT {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link org.matsim.contrib.parking.parkingchoice.run.RunParkingChoiceExample#run(org.matsim.core.config.Config)}.
	 */
	@Test
	final void testRun() {
		Config config = ConfigUtils.loadConfig("./src/main/resources/parkingchoice/config.xml");
		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setLastIteration(0);
		RunParkingChoiceExample.run(config);

	}

}
