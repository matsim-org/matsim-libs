/*
 *   *********************************************************************** *
 *   project: org.matsim.*													 *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2025 by the members listed in the COPYING, 		 *
 *                          LICENSE and WARRANTY file.  					 *
 *   email           : info at matsim dot org   							 *
 *                                                                         	 *
 *   *********************************************************************** *
 *                                                                        	 *
 *     This program is free software; you can redistribute it and/or modify  *
 *      it under the terms of the GNU General Public License as published by *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.								     *
 *     See also COPYING, LICENSE and WARRANTY file						 	 *
 *                                                                           *
 *   *********************************************************************** *
 */

package org.matsim.freight.logistics.consistency_checkers;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.logistics.FreightLogisticsConfigGroup;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.logistics.consistency_checkers.LogisticsConsistencyChecker.CheckResult.CHECK_FAILED;
import static org.matsim.freight.logistics.consistency_checkers.LogisticsConsistencyChecker.CheckResult.CHECK_SUCCESSFUL;


/**
 *  This class will check if LogisticsConsistencyChecker are working as intended.
 *  They are using the more general (public) calls of the checker methods.
 *	Tests are designed to succeed.
 */
public class LspConsistencyCheckerTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.WARN;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This test will check if checksBeforePlanning work as intended and should pass.
	 */
	@Test
	public void checkBeforePlanning_Passes() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.checkBeforePlanning(LSPUtils.getLSPs(scenario), lvl),"At least one resource Id exists more than once.");
	}

	/**
	 * This test will check if checksBeforePlanning work as intended and should fail, because there are two resources named "carrierSouth" in lsps_fail.xml
	 */
	@Test
	public void checkBeforePlanning_Fails() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.checkBeforePlanning(LSPUtils.getLSPs(scenario), lvl),"All resource IDs are unique.");
	}


	// ***********************************
	// * 	Tests for checkAfterPlanning  *
	// ***********************************

	/**
	 * This test will check if checksAfterPlanning work  as intended and should succeed
	 */
	@Test
	public void checkBeforePlanning_passes() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.checkAfterPlanning(LSPUtils.getLSPs(scenario), lvl));
	}

	/**
	 * This test will check if checksAfterPlanning works as intended and should succeed, because shipmentWOPlan is not planned in any plan.
	 */
	@Test
	public void checkBeforePlanning_fails() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.checkAfterPlanning(LSPUtils.getLSPs(scenario), lvl));
	}
}


