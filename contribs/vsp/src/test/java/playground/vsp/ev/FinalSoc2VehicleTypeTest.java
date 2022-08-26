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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

//TODO: remove or better move to EV contrib
public class FinalSoc2VehicleTypeTest {

	private final Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
	private final SOCHandler handler = new SOCHandler(scenario);
	private static final Integer LAST_ITERATION = 2;
	private static final double INITIAL_ENERGY = 9.5;

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	private void runSim() {
		scenario.getConfig().controler().setLastIteration(LAST_ITERATION);
		scenario.getConfig().controler().setOutputDirectory("test/output/playground/vsp/ev/FinalSoc2VehicleTypeTest/");

		//modify population
		//		overridePopulation(scenario);

		//		//insert vehicles
		//		scenario.getVehicles().getVehicles().keySet().forEach(vehicleId -> {
		//			Id<VehicleType> type = scenario.getVehicles().getVehicles().get(vehicleId).getType().getId();
		//			scenario.getVehicles().removeVehicle(vehicleId);
		//			scenario.getVehicles().removeVehicleType(type);
		//		});
		//		RunUrbanEVExample.createAndRegisterPersonalCarAndBikeVehicles(scenario);

		VehicleType vehicleType = scenario.getVehicles()
				.getVehicleTypes()
				.get(Id.create("Triple Charger", VehicleType.class));
		EVUtils.setInitialEnergy(vehicleType.getEngineInformation(), INITIAL_ENERGY);

		//controler
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.addControlerListener(handler);
		controler.run();
	}

	private static void overridePopulation(Scenario scenario) {
		scenario.getPopulation().getPersons().clear();
		PopulationFactory factory = scenario.getPopulation().getFactory();

		Person person = factory.createPerson(Id.createPersonId("person"));
		Plan plan = factory.createPlan();

		Activity home21 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home21.setEndTime(8 * 3600);
		plan.addActivity(home21);
		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work21 = factory.createActivityFromLinkId("work", Id.createLinkId("175"));
		work21.setEndTime(10 * 3600);
		plan.addActivity(work21);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work22 = factory.createActivityFromLinkId("work", Id.createLinkId("60"));
		work22.setEndTime(12 * 3600);
		plan.addActivity(work22);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity shopping21 = factory.createActivityFromLinkId("shopping", Id.createLinkId("9"));
		shopping21.setMaximumDuration(1200);

		plan.addActivity(shopping21);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work23 = factory.createActivityFromLinkId("work", Id.createLinkId("5"));
		work23.setEndTime(13 * 3600);
		plan.addActivity(work23);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity home22 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home22.setEndTime(15 * 3600);
		plan.addActivity(home22);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		scenario.getPopulation().addPerson(person);
	}

	@Test
	public void test() {
		runSim();

		// testInitialEnergyInIter0
		Assert.assertTrue(handler.iterationInitialSOC.get(0).equals(INITIAL_ENERGY));

		// testSOCIsDumpedIntoVehicleType
		//agent has driven the car so SOC should have changed and should be dumped into the vehicle type
		VehicleType carType = scenario.getVehicles()
				.getVehicleTypes()
				.get(Id.create("Triple Charger", VehicleType.class));
		Assert.assertNotEquals(EVUtils.getInitialEnergy(carType.getEngineInformation()), INITIAL_ENERGY);
		Assert.assertEquals(10, EVUtils.getInitialEnergy(carType.getEngineInformation()),
				MatsimTestUtils.EPSILON); //should be fully charged

		// testSOCisTransferredToNextIteration
		for (int i = 0; i <= LAST_ITERATION - 1; i++) {
			Assert.assertEquals(handler.iterationEndSOC.get(i), handler.iterationInitialSOC.get(i + 1));
		}
	}

	private static class SOCHandler implements BeforeMobsimListener, AfterMobsimListener {
		private final Map<Integer, Double> iterationInitialSOC = new HashMap<>();
		private final Map<Integer, Double> iterationEndSOC = new HashMap<>();
		private final VehicleType carType;

		SOCHandler(Scenario scenario) {
			this.carType = (scenario.getVehicles()
					.getVehicleTypes()
					.get(Id.create("Triple Charger", VehicleType.class)));
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			this.iterationEndSOC.put(event.getIteration(), EVUtils.getInitialEnergy(carType.getEngineInformation()));
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			this.iterationInitialSOC.put(event.getIteration(),
					EVUtils.getInitialEnergy(carType.getEngineInformation()));
		}
	}
}
