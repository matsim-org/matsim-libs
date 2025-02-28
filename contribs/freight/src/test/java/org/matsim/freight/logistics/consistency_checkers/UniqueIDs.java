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

import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;

import org.matsim.freight.logistics.FreightLogisticsConfigGroup;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.LSPs;
import org.matsim.freight.logistics.consistency_checker.LogisticsConsistencyChecker;
import org.matsim.freight.logistics.io.LSPPlanXmlReader;
import org.matsim.freight.logistics.shipment.*;
import org.matsim.testcases.MatsimTestUtils;



import java.util.Collections;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.logistics.consistency_checker.LogisticsConsistencyChecker.CheckResult.CHECK_FAILED;
import static org.matsim.freight.logistics.consistency_checker.LogisticsConsistencyChecker.CheckResult.CHECK_SUCCESSFUL;


/**
 *  hier gibt es (noch) nichts zu sehen :-)
 *
 */
public class UniqueIDs {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	private final Level lvl = Level.WARN;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * TODO: Bitte noch kleine Beschreibung des Test-Cases einfügen. Danke :)
	 */
	@Test
	public void TestAreResourceIDsUnique_Passes() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml"); //Dahin kopieren..
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.resourcesAreUnique(LSPUtils.getLSPs(scenario), lvl),"At least one resource ID exists more than once.");
	}

	/**
	 * TODO: Bitte noch kleine Beschreibung des Test-Cases einfügen. Danke :)
	 */
	@Test
	public void TestAreResourceIDsUnique_Fails() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml"); //Dahin kopieren..
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.resourcesAreUnique(LSPUtils.getLSPs(scenario), lvl),"All resource IDs are unique.");
	}


	@Test
	public void ShipmentHasPlanSelectedPlanOnly_passes() {
		Config config = ConfigUtils.createConfig();
		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml"); //Dahin kopieren..
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

//		//Versuch mal per Hand einen weitern gleich lautenden ShipmentPlan zu erstellen. Geht nicht :)
//		var lsps = LSPUtils.getLSPs(scenario);
//		var lsp = lsps.getLSPs().get(Id.create("LSP_1", LSP.class));
//		var lspPlan = lsp.getSelectedPlan();
//    	LspShipmentPlan shipmentPlan = LspShipmentUtils.getOrCreateShipmentPlan(lspPlan,Id.create("shipmentSouth", LspShipment.class));
//   	 LspShipmentPlanElement shipmentPlanElement = LspShipmentUtils.LoggedShipmentHandleBuilder.newInstance()
//		 .setEndTime(30).setStartTime(15)
//		.build();
//		shipmentPlan.addPlanElement(Id.create("ABC", LspShipmentPlanElement.class), shipmentPlanElement);
//		lspPlan.addShipmentPlan(shipmentPlan);

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsistencyChecker.shipmentsArePlannedExactlyOnceSelectedPlanOnly(LSPUtils.getLSPs(scenario), lvl),"Not all shipments are planned.");
	}

	/**
	 * should fail, because shipmentSouth is not planned.
	 */
	@Test
	public void ShipmentHasPlanSelectedPlanOnly_fails() {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lsps_fail.xml");

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml"); //Dahin kopieren..
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);

		Assertions.assertEquals(CHECK_FAILED, LogisticsConsistencyChecker.shipmentsArePlannedExactlyOnceSelectedPlanOnly(LSPUtils.getLSPs(scenario), lvl),"All shipments are planned.");
	}
}
