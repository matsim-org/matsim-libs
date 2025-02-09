package org.matsim.freight.carriers.consistency_checkers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;
import org.junit.jupiter.api.Test;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 *
 * @author antonstock
 * doc:
 *
 */

public class TourTest {
	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test

	void testTour_passes() {
		String pathToInput = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		//@KMT: Hier meckert er leider. Ich glaube der Fehler liegt in der Art und Weise, wie die XML aufgebaut ist, allerdings habe ich den
		// Aufbau von den Beispielen, selbst mit einer 1:1 Kopie der Beispiele kommt der gleiche Fehler...
		freightConfigGroup.setCarriersFile(pathToInput+"CCPlansCarrierWithServicesPASS.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput+"CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		CarrierConsistencyCheckers.allJobsInTourCheckResult testResult = CarrierConsistencyCheckers.allJobsInTours(carriers);
		Assertions.assertEquals(CarrierConsistencyCheckers.allJobsInTourCheckResult.ALL_JOBS_IN_TOURS, testResult, "At least one job is not scheduled correctly!");
	}
//TODO: Weitere Testszenarien:
	// 1) Job mehr als einmal verplant
	// 2) Job gar nicht verplant
	// 3) weitere...?
}
