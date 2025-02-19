package org.matsim.freight.logistics.consistency_checkers;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;

import org.matsim.freight.logistics.FreightLogisticsConfigGroup;

import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.LSPs;
import org.matsim.freight.logistics.consistency_checker.LogisticsConsitencyChecker;
import org.matsim.freight.logistics.io.LSPPlanXmlReader;
import org.matsim.testcases.MatsimTestUtils;



import java.util.Collections;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;
import static org.matsim.freight.logistics.consistency_checker.LogisticsConsitencyChecker.CheckResult.CHECK_SUCCESSFUL;


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

	@Test
	public void myFirstLSPTst() {

		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = addOrGetModule(config, FreightLogisticsConfigGroup.class);
		//logisticsConfigGroup.setLspsFile(utils.getPackageInputDirectory() + "lspTestFile.xml"); //Does not work atm.

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "carriers.xml"); //Dahin kopieren..
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);


		LSPUtils.addLSPs(scenario, new LSPs(Collections.emptyList()));

		new LSPPlanXmlReader(LSPUtils.getLSPs(scenario), CarriersUtils.getCarriers(scenario)).readFile(utils.getPackageInputDirectory() + "lsps.xml");

		Assertions.assertEquals(CHECK_SUCCESSFUL, LogisticsConsitencyChecker.resourcesAreUnique(LSPUtils.getLSPs(scenario), lvl),"At least one resource ID exists more than once.");

	}
}
