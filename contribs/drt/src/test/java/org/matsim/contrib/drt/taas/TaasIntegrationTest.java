package org.matsim.contrib.drt.taas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.taas.TaasScenarioBuilder.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

public class TaasIntegrationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testExample() {
		TestScenario test = new TaasScenarioBuilder(utils) //
				.addVehicle("veh", 0, 0, 4) //
				.addDemand("passenger", "A", 3600.0, 0, 0, 5, 5) //
				.addDemand("parcel", "B", 3600.0, 5, 5, 2, 2) //
				.build();

		test.controller().run();

		assertEquals(5642.0, test.tracker().getInformation("A").dropoffTime);
		assertEquals(7281.0, test.tracker().getInformation("B").dropoffTime);
	}
}
