/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
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
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.lspScoring;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ExampleLSPScoringTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain() {

		Config config = ExampleLSPScoring.prepareConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ExampleLSPScoring.prepareScenario(config);

		Controller controller = ExampleLSPScoring.prepareController(scenario);

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controller.run();

		for (LSP lsp : LSPUtils.getLSPs(scenario).getLSPs().values()) {
			Assertions.assertEquals(13.245734044444207, lsp.getSelectedPlan().getScore(), Double.MIN_VALUE);
		}

	}
}
