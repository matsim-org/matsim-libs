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

package org.matsim.urbanEV;

import com.google.inject.Inject;
import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.util.HashMap;
import java.util.Map;

public class FinalSoc2VehicleTypeTest {

	private static Scenario scenario;
	private static SOCHandler handler;

	@BeforeClass
	public static void runSim(){
		scenario = CreateUrbanEVTestScenario.createTestScenario();

		scenario.getConfig().controler().setLastIteration(2);
		scenario.getConfig().controler().setOutputDirectory("test/output/urbanEV/FinalSoc2VehicleTypeTest");
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);

		//modify population
		overridePopulation(scenario);

		//insert vehicles
		scenario.getVehicles().getVehicles().keySet().forEach(vehicleId -> scenario.getVehicles().removeVehicle(vehicleId));
		CreateUrbanEVTestScenario.createAndRegisterPersonalCarAndBikeVehicles(scenario);

		handler = new SOCHandler(scenario.getVehicles().getVehicleTypes().get(Id.create("person", VehicleType.class))); //car vehicle type currently is set to the same name as the person id
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
	public void testInitialEnergyInIter0(){
		Assert.assertTrue(handler.iterationInitialSOC.get(0).equals(CreateUrbanEVTestScenario.CAR_INITIAL_ENERGY));
	}

	@Test
	public void testSOCIsDumpedIntoVehicleType(){
		//agent has driven the car so SOC should have changed and should be dumped into the vehicle type
		VehicleType carType = scenario.getVehicles().getVehicleTypes().get(Id.create("person", VehicleType.class));
		Assert.assertNotEquals(EVUtils.getInitialEnergy(carType.getEngineInformation()), CreateUrbanEVTestScenario.CAR_INITIAL_ENERGY);
		Assert.assertEquals(4.04297917065838, EVUtils.getInitialEnergy(carType.getEngineInformation()), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testSOCisTransferredToNextIteration(){
		for(int i = 0; i <= 1; i++){
			Assert.assertTrue(handler.iterationEndSOC.get(i).equals(handler.iterationInitialSOC.get(i+1)));
		}
	}


	private static class SOCHandler implements BeforeMobsimListener, AfterMobsimListener{

		Map<Integer,Double> iterationInitialSOC = new HashMap<>();
		Map<Integer,Double> iterationEndSOC = new HashMap<>();

		VehicleType carType;

		SOCHandler(VehicleType carVehicleType) {
			this.carType = carVehicleType;
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			this.iterationEndSOC.put(event.getIteration(), EVUtils.getInitialEnergy(carType.getEngineInformation()));
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			this.iterationInitialSOC.put(event.getIteration(), EVUtils.getInitialEnergy(carType.getEngineInformation()));
		}
	}



}
