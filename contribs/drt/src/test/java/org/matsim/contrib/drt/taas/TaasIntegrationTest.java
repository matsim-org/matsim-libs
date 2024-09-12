package org.matsim.contrib.drt.taas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.taas.TaasScenarioBuilder.TestScenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.testcases.MatsimTestUtils;

public class TaasIntegrationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testBaseline() {
		TestScenario test = new TaasScenarioBuilder(utils) //
				.addVehicle("veh", 0, 0, 4) //
				.addDemand("A", 3600.0, 0, 0, 5, 5) //
				.build();

		DrtOptimizationConstraintsParams constraintsContainer = new DrtOptimizationConstraintsParams();
		test.drt().addParameterSet(constraintsContainer);

		DefaultDrtOptimizationConstraintsSet constraints = new DefaultDrtOptimizationConstraintsSet();
		constraints.name = "default";
		constraints.maxWaitTime = 300.0;
		constraints.maxTravelTimeBeta = 1.0;
		constraints.maxTravelTimeAlpha = 600.0;
		constraintsContainer.addParameterSet(constraints);

		SquareGridZoneSystemParams zoneSystemParams = new SquareGridZoneSystemParams();
		zoneSystemParams.cellSize = 100.0;

		DvrpTravelTimeMatrixParams matrixParams = DvrpConfigGroup.get(test.config()).getTravelTimeMatrixParams();
		matrixParams.addParameterSet(zoneSystemParams);

		test.config().controller().setWriteEventsInterval(1);
		test.controller().run();

		assertEquals(3631.0, test.tracker().getInformation("A").pickupTime);
	}
}
