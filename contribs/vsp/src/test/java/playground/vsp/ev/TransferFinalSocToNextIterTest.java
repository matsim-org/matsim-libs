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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
	private static final double INITIAL_ENERGY = 9.5;

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void test() {
		//adapt scenario
		scenario.getConfig().controler().setLastIteration(LAST_ITERATION);
		scenario.getConfig().controler().setOutputDirectory("test/output/playground/vsp/ev/FinalSoc2VehicleTypeTest/");

		var vehicle1 = scenario.getVehicles().getVehicles().get(Id.create("Triple Charger_car", Vehicle.class));
		EVUtils.setInitialEnergy(vehicle1, INITIAL_ENERGY);

		//controler
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.addControlerListener(handler);
		controler.run();

		// testInitialEnergyInIter0
		Assert.assertEquals(INITIAL_ENERGY, handler.iterationInitialSOC.get(0), 0.0);

		// testSOCIsDumpedIntoVehicleType
		//agent has driven the car so SOC should have changed and should be dumped into the vehicle type
		var vehicle = scenario.getVehicles().getVehicles().get(Id.create("Triple Charger_car", Vehicle.class));
		Assert.assertNotEquals(EVUtils.getInitialEnergy(vehicle), INITIAL_ENERGY);
		Assert.assertEquals(10, EVUtils.getInitialEnergy(vehicle), MatsimTestUtils.EPSILON); //should be fully charged

		// testSOCisTransferredToNextIteration
		for (int i = 0; i < LAST_ITERATION; i++) {
			Assert.assertEquals(handler.iterationEndSOC.get(i), handler.iterationInitialSOC.get(i + 1));
		}
	}

	private static class SOCHandler implements BeforeMobsimListener, AfterMobsimListener {
		private final Map<Integer, Double> iterationInitialSOC = new HashMap<>();
		private final Map<Integer, Double> iterationEndSOC = new HashMap<>();
		private final Vehicle car;

		SOCHandler(Scenario scenario) {
			this.car = scenario.getVehicles().getVehicles().get(Id.create("Triple Charger_car", Vehicle.class));
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			this.iterationEndSOC.put(event.getIteration(), EVUtils.getInitialEnergy(car));
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			this.iterationInitialSOC.put(event.getIteration(), EVUtils.getInitialEnergy(car));
		}
	}
}
