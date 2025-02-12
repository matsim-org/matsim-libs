package org.matsim.freight.carriers.consistency_checkers;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 *
 *  @author antonstock
 *
 *
 * 	 */
public class CheckBeforeTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	Level lvl = Level.ERROR;
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	void checkBefore_passes() {
		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriersShipmentsPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		CarrierConsistencyCheckers.CheckResult testResult = CarrierConsistencyCheckers.checkBefore(carriers, lvl);
		Assertions.assertEquals(CarrierConsistencyCheckers.CheckResult.CHECK_SUCCESSFUL,testResult,"At least one check failed.");
	}
	@Test
	void checkBefore_fails() {
		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriersShipmentsFAIL.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		CarrierConsistencyCheckers.CheckResult testResult = CarrierConsistencyCheckers.checkBefore(carriers, lvl);
		Assertions.assertEquals(CarrierConsistencyCheckers.CheckResult.CHECK_FAILED,testResult,"All checks passed.");
	}
}
