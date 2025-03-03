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
 *  hier gibt es (noch) nichts zu sehen :-)
 *
 */
public class UniqueIdsTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.WARN;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * this test will check if resourcesAreUnique-method works as intended and should pass, because all resource IDs in lsps.xml are unique.
	 */
	@Test
	public void TestAreResourceIDsUnique_Passes() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.resourcesAreUnique(LSPUtils.getLSPs(scenario), lvl),"At least one resource ID exists more than once.");
	}

	/**
	 * this test will check if resourcesAreUnique-method works as intended and should fail, because there are two resources named "carrierSouth" in lsps_fail.xml
	 */
	@Test
	public void TestAreResourceIDsUnique_Fails() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.resourcesAreUnique(LSPUtils.getLSPs(scenario), lvl),"All resource IDs are unique.");
	}

	/**
	 * this test will check if shipmentPlanForEveryShipment-method works as intended and should pass, because all shipments in lsps.xml have got a plan in the selected plan.
	 */
	@Test
	public void ShipmentHasPlanSelectedPlanOnly_passes() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.shipmentPlanForEveryShipmentSelectedPlanOnly(LSPUtils.getLSPs(scenario), lvl),"Not all shipments are planned.");
	}

	/**
	 * this test will check if shipmentPlanForEveryShipmentSelectedPlanOnly-method works as intended and should fail, because shipmentWOPlan is not planned.
	 */
	@Test
	public void AllShipmentsGotPlanSelectedPlanOnly_fails() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.shipmentPlanForEveryShipmentSelectedPlanOnly(LSPUtils.getLSPs(scenario), lvl),"All shipments are planned.");
	}



	/**
	 * this test will check if shipmentPlanForEveryShipmentAllPlans-method works as intended and should pass
	 */
	@Test
	public void AllShipmentsGotPlanAllPlans_passes() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.shipmentPlanForEveryShipmentAllPlans(LSPUtils.getLSPs(scenario), lvl),"All shipments are planned.");
	}
	/**
	 * this test will check if shipmentPlanForEveryShipment-method works as intended and should fail, because shipmentWOPlan is not planned in any plan.
	 */
	@Test
	public void AllShipmentsGotPlanAllPlans_fails() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.shipmentPlanForEveryShipmentAllPlans(LSPUtils.getLSPs(scenario), lvl),"All shipments are planned.");
	}

	/**
	 * this test will check if shipmentForEveryShipmentPlanSelectedPlanOnly-method works as intended and should fail, because planWOShipment is not planned in any plan.
	 * @KMT: funktioniert nicht
	 */
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
}


