/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.ev;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class TransferFinalSocToNextIterTest {

	private final Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
	private final SOCHandler handler = new SOCHandler(scenario);
	private static final Integer LAST_ITERATION = 1;
	private static final double INITIAL_SOC = 0.95;

	@RegisterExtension
	private MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	void test() {
		//adapt scenario
		scenario.getConfig().controller().setLastIteration(LAST_ITERATION);
		scenario.getConfig().controller().setOutputDirectory("test/output/playground/vsp/ev/FinalSoc2VehicleTypeTest/");

		var vehicle1 = scenario.getVehicles().getVehicles().get(Id.create("Triple Charger_car", Vehicle.class));
		ElectricFleetUtils.setInitialSoc(vehicle1, INITIAL_SOC );

		//controler
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.addControlerListener(handler);
		controler.run();

		// testInitialEnergyInIter0
		Assertions.assertEquals(INITIAL_SOC, handler.iterationInitialSOC.get(0), 0.0);

		// testSOCIsDumpedIntoVehicleType
		//agent has driven the car so SOC should have changed and should be dumped into the vehicle type
		var vehicle = scenario.getVehicles().getVehicles().get(Id.create("Triple Charger_car", Vehicle.class));
		var evSpec = ElectricFleetUtils.createElectricVehicleSpecificationDefaultImpl(vehicle );
		Assertions.assertNotEquals(evSpec.getInitialSoc(), INITIAL_SOC);
		Assertions.assertEquals(0.7273605127621898, evSpec.getInitialSoc(), MatsimTestUtils.EPSILON); //should not be fully charged

		// testSOCisTransferredToNextIteration
		for (int i = 0; i < LAST_ITERATION; i++) {
			Assertions.assertEquals(handler.iterationEndSOC.get(i), handler.iterationInitialSOC.get(i + 1));
		}
	}

	private static class SOCHandler implements BeforeMobsimListener, AfterMobsimListener {
		private final Map<Integer, Double> iterationInitialSOC = new HashMap<>();
		private final Map<Integer, Double> iterationEndSOC = new HashMap<>();
		private final ElectricVehicleSpecification evSpec;

		SOCHandler(Scenario scenario) {
			var car = scenario.getVehicles().getVehicles().get(Id.create("Triple Charger_car", Vehicle.class));
			evSpec = ElectricFleetUtils.createElectricVehicleSpecificationDefaultImpl(car );
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			this.iterationEndSOC.put(event.getIteration(), evSpec.getInitialSoc());
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			this.iterationInitialSOC.put(event.getIteration(), evSpec.getInitialSoc());
		}
	}
}
