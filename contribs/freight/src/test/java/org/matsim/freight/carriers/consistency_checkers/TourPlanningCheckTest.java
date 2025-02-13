package org.matsim.freight.carriers.consistency_checkers;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 * @author antonstock
 * TestTourPlanningCheck checks, if all jobs are scheduled correctly, i.e. every job is part of only one tour.
 * both Tests are designed to succeed, checkResult of _passes should be "CHECK_SUCCESSFUL" while checkResult of _fails should be "CHECK_FAILED".
 * 	 */
public class TourPlanningCheckTest {
	//Please specify wanted logger level here:
	//Level.ERROR -> all log-messages will be displayed as errors in red.
	//Level.WARN -> all log-messages will be displayed as warnings in red.
	//Level.INFO -> all log-messages will be displayed as information in white.
	Level lvl = Level.ERROR;
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void tourPlanningCheck_passes() {
		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCPlansCarrierWithServicesPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		CarrierConsistencyCheckers.CheckResult checkResult = CarrierConsistencyCheckers.tourPlanningCheck(carriers, lvl);
		Assertions.assertEquals(CarrierConsistencyCheckers.CheckResult.CHECK_SUCCESSFUL,checkResult,"At least one check failed.");
	}

	@Test
	void tourPlanningCheck_fails() {
		Config config = ConfigUtils.createConfig();
		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCPlansCarrierWithServicesFAIL_1.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		CarrierConsistencyCheckers.CheckResult checkResult = CarrierConsistencyCheckers.tourPlanningCheck(carriers, lvl);
		Assertions.assertEquals(CarrierConsistencyCheckers.CheckResult.CHECK_FAILED,checkResult,"All checks passed.");
	}
}
