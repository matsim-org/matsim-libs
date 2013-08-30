/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

public class EquilTest {

	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testScenario() {
		
		String inputDir = this.utils.getClassInputDirectory();
		Config config = ConfigUtils.loadConfig(inputDir + "testScenario/config.xml");

        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup == null) {
            multiModalConfigGroup = new MultiModalConfigGroup();
            config.addModule(multiModalConfigGroup);
        }
        multiModalConfigGroup.setMultiModalSimulationEnabled(true);
		
        // disabled running the controler until within-day refactoring has been completed!
//        MyWithinDayControler.start(config);
	}

}
