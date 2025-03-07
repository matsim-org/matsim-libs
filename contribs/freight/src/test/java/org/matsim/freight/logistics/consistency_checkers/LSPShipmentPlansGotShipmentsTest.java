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
import org.matsim.freight.logistics.consistency_checker.LogisticsConsistencyChecker;
import org.matsim.testcases.MatsimTestUtils;


import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.logistics.consistency_checker.LogisticsConsistencyChecker.CheckResult.CHECK_FAILED;
import static org.matsim.freight.logistics.consistency_checker.LogisticsConsistencyChecker.CheckResult.CHECK_SUCCESSFUL;


/**
 *  this class will check if LogisticsConsistencyChecker.shipmentForEveryShipmentPlanSelectedPlanOnly AND ... are working as intended.
 *	Tests are designed to succeed.
 */
public class LSPShipmentPlansGotShipmentsTest {
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

		//Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.shipmentForEveryShipmentPlanAllPlans(LSPUtils.getLSPs(scenario), lvl),"At least one plan has not got a shipment.");
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

		//Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.shipmentForEveryShipmentPlanAllPlans(LSPUtils.getLSPs(scenario), lvl),"All plans got a shipment.");
	}
}


