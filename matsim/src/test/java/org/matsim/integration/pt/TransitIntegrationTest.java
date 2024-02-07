/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.integration.pt;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ScoringParameterSet;
import org.matsim.core.controler.Controler;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestUtils;

public class TransitIntegrationTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testPtInteractionParams() {
		assertThrows(RuntimeException.class, () -> {
			Config config = ConfigUtils.createConfig();
			config.controller().setOutputDirectory(utils.getOutputDirectory());
			ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
			params.setScoringThisActivityAtAll(true);
			params.setTypicalDuration(60.0);
			config.scoring().addActivityParams(params);
			// ---
			config.controller().setLastIteration(0); // in case the exception is _not_ thrown, we don't need 100 iterations to find that out ...
			// ---
			Controler controler = new Controler(config);
			controler.run();
		});
	}


	@Test
	void testSubpopulationParams() {
		assertThrows(RuntimeException.class, () -> {
			Config config = ConfigUtils.createConfig();
			config.controller().setOutputDirectory(utils.getOutputDirectory());
			ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams("home");
			params.setScoringThisActivityAtAll(true);
			params.setTypicalDuration(60.0);
			ScoringParameterSet sps = config.scoring().getOrCreateScoringParameters("one");
			sps.addActivityParams(params);
			ScoringParameterSet sps2 = config.scoring().getOrCreateScoringParameters("two");
			sps2.addActivityParams(params);
			// ---
			config.controller().setLastIteration(0); // in case the exception is _not_ thrown, we don't need 100 iterations to find that out ...
			config.checkConsistency();

			Controler controler = new Controler(config);
			controler.run();
		});
	}

}
