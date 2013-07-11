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

import org.matsim.core.config.Config;
import contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.testcases.MatsimTestCase;

public class EquilTest extends MatsimTestCase {

	public void testScenario(){
		Config config = this.loadConfig(this.getInputDirectory() + "config.xml");
        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup == null) {
            multiModalConfigGroup = new MultiModalConfigGroup();
            config.addModule(multiModalConfigGroup);
        }
        multiModalConfigGroup.setMultiModalSimulationEnabled(true);
		MyWithinDayControler.start(config);
	}

}
