package org.matsim.contrib.drt.taas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.taas.TaasScenarioBuilder.TestScenario;
import org.matsim.contrib.dvrp.fleet.ScalarVehicleLoad;
import org.matsim.testcases.MatsimTestUtils;

public class TaasIntegrationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testExample() {
		TestScenario test = new TaasScenarioBuilder(utils) //
				.addVehicle("veh", 0, 0, new ScalarVehicleLoad(4)) //
				.addDemand("passenger", "A", 3600.0, 0, 0, 5, 5) //
				.addDemand("parcel", "B", 3600.0, 5, 5, 2, 2) //
				.build();

		test.service().maximumPassengerWaitTime = 300.0;
		test.service().maximumPassengerTravelDelay = 600.0;
		test.service().passengerInteractionDuration = 30.0;

		test.service().parcelLatestDeliveryTime = 20.0 * 3600.0;
		test.service().parcelPickupDuration = 30.0;
		test.service().parcelDeliveryDuration = 240.0;

		test.controller().run();

		assertEquals(5673.0, test.tracker().getInformation("A").dropoffTime);
		assertEquals(7522.0, test.tracker().getInformation("B").dropoffTime);
	}
}
