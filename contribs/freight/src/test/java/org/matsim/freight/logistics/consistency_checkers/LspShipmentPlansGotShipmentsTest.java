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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.freight.carriers.FreightCarriersConfigGroup;

import org.matsim.freight.logistics.FreightLogisticsConfigGroup;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.testcases.MatsimTestUtils;


import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.logistics.consistency_checkers.LogisticsConsistencyChecker.CheckResult.CHECK_FAILED;
import static org.matsim.freight.logistics.consistency_checkers.LogisticsConsistencyChecker.CheckResult.CHECK_SUCCESSFUL;


/**
 *  this class will check if LogisticsConsistencyChecker.shipmentForEveryShipmentPlanSelectedPlanOnly AND ... are working as intended.
 *	Tests are designed to succeed.
 */
public class LspShipmentPlansGotShipmentsTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.WARN;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();
	/**
	 * this test will check if shipmentForEveryShipmentPlanSelectedPlanOnly-method works as intended and should succeed, because all plans got shipments.
	 */
	@Test
	public void AllShipmentPlansGotShipmentSelectedPlanOnly_passes() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.shipmentForEveryShipmentPlanSelectedPlanOnly(LSPUtils.getLSPs(scenario), lvl),"At least one plan has not got a shipment.");
	}
	/**
	 * this test will check if shipmentForEveryShipmentPlanSelectedPlanOnly-method works as intended and should succeed, because planWOShipmentSelected has no shipment
	 */
	//@KMT: Dieser Test findet nur Pläne, die auch Shipments haben.
	@Test
	public void AllShipmentPlansGotShipmentSelectedPlanOnly_fails() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		//manually add a shipment plan to the selcted plan of LSP_1. The shipmentId of this plan does NOT match to the shipments defined in the lsp itself.
		//This must be done here in code, because when reading in the XML file, only shipment plans are added, if their shipmentId that matches to the shipments defined in the lsp.
		LSP lsp1 = LSPUtils.addOrGetLsps(scenario).getLSPs().get(Id.create("LSP_1", LSP.class));
		LspShipmentUtils.getOrCreateShipmentPlan(lsp1.getSelectedPlan(), Id.create("planWOShipmentSelected", LspShipment.class));

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.shipmentForEveryShipmentPlanSelectedPlanOnly(LSPUtils.getLSPs(scenario), lvl),"All plans got a shipment.");
	}

	/**
	 * this test will check if shipmentForEveryShipmentPlanAllPlans-method works as intended and should succeed, because all plans got shipments.
	 */
	@Test
	public void AllShipmentPlansGotShipmentAllPlans_passes() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.shipmentForEveryShipmentPlanAllPlans(LSPUtils.getLSPs(scenario), lvl),"At least one plan has not got a shipment.");
	}
	/**
	 * this test will check if shipmentForEveryShipmentPlanAllPlans-method works as intended and should succeed, because planWOShipmentSelected and planWOShipmentAll have no shipments.
	 */
	@Test
	public void AllShipmentPlansGotShipmentAllPlans_fails() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		//manually add a shipment plan to the selcted plan of LSP_1. The shipmentId of this plan does NOT match to the shipments defined in the lsp itself.
		//This must be done here in code, because when reading in the XML file, only shipment plans are added, if their shipmentId that matches to the shipments defined in the lsp.
		LSP lsp1 = LSPUtils.addOrGetLsps(scenario).getLSPs().get(Id.create("LSP_1", LSP.class));
		LSPPlan lspPlan = null;
		for (LSPPlan lspPlanTemp : lsp1.getPlans()) {
			if (!lspPlanTemp.equals(lsp1.getSelectedPlan())){
				lspPlan=lspPlanTemp;
			}
		}
		assert lspPlan != null;
		LspShipmentPlan planWoShipmentInJobs = LspShipmentUtils.getOrCreateShipmentPlan(lspPlan, Id.create("planWOShipmentSelected", LspShipment.class));

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.shipmentForEveryShipmentPlanAllPlans(LSPUtils.getLSPs(scenario), lvl),"All plans got a shipment.");
	}
}


