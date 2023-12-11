/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.hermes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.hermes.HermesTest.Fixture;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Map;

/**
 * Tests different aspects of flow capacity in Hermes
 *
 */
public class FlowCapacityTest {

	private final static Logger log = LogManager.getLogger(FlowCapacityTest.class);

	@BeforeEach
	public void setup() {
		Id.resetCaches();
	}

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents driving over a link.
	 *
	 * @author mrieser
	 */
	@Test
	void testFlowCapacityDriving() {
		Fixture f = new Fixture();

		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 12000; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			/* exact dep. time: 6:29:48. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 10 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 10 + 1 = 12 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 12 = 7*3600 - 1812
			 */
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.plans.addPerson(person);
		}



		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();

		/* finish */
		int[] volume1 = vAnalyzer.getVolumesForLink(f.link1.getId());
		System.out.println("#vehicles 3-4: " + volume1[3]);
		System.out.println("#vehicles 4-5: " + volume1[4]);
		System.out.println("#vehicles 5-6: " + volume1[5]);
		System.out.println("#vehicles 6-7: " + volume1[6]);
		System.out.println("#vehicles 7-8: " + volume1[7]);
		System.out.println("#vehicles 8-9: " + volume1[8]);
		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 3-4: " + volume[3]);
		System.out.println("#vehicles 4-5: " + volume[4]);
		System.out.println("#vehicles 5-6: " + volume[5]);
		System.out.println("#vehicles 6-7: " + volume[6]);
		System.out.println("#vehicles 7-8: " + volume[7]);
		System.out.println("#vehicles 8-9: " + volume[8]);

		Assertions.assertEquals(0, volume[5], 1);    // no vehicles
		Assertions.assertEquals(3004, volume[6], 1);  // we should have half of the maximum flow in this hour
		Assertions.assertEquals(6000, volume[7], 1);  // we should have maximum flow in this hour
		Assertions.assertEquals(2996, volume[8], 1);  // all the rest

	}

	/**
	 * Tests downscaling of flow capacity works
	 *
	 * @author jfbischoff
	 */

	@Test
	void testFlowCapacityDrivingFlowCapacityFactors() {
		Fixture f = new Fixture();
		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 1200; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.plans.addPerson(person);
		}
		f.config.hermes().setFlowCapacityFactor(0.1);

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();

		/* finish */
		int[] volume1 = vAnalyzer.getVolumesForLink(f.link1.getId());
		System.out.println("#vehicles 3-4: " + volume1[3]);
		System.out.println("#vehicles 4-5: " + volume1[4]);
		System.out.println("#vehicles 5-6: " + volume1[5]);
		System.out.println("#vehicles 6-7: " + volume1[6]);
		System.out.println("#vehicles 7-8: " + volume1[7]);
		System.out.println("#vehicles 8-9: " + volume1[8]);
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 3-4: " + volume[3]);
		System.out.println("#vehicles 4-5: " + volume[4]);
		System.out.println("#vehicles 5-6: " + volume[5]);
		System.out.println("#vehicles 6-7: " + volume[6]);
		System.out.println("#vehicles 7-8: " + volume[7]);
		System.out.println("#vehicles 8-9: " + volume[8]);

		Assertions.assertEquals(0, volume[5], 1);     // no vehicles
		Assertions.assertEquals(301, volume[6], 1);  // we should have half of the maximum flow in this hour
		Assertions.assertEquals(600, volume[7], 1);  // we should have maximum flow in this hour
		Assertions.assertEquals(299, volume[8], 1);  // all the rest

	}

	/**
	 * Tests flow efficiency factors, e.g. for AVs, are working
	 *
	 * @author jfbischoff
	 */

	@Test
	void testFlowCapacityDrivingFlowEfficiencyFactors() {
		Fixture f = new Fixture();
		ScenarioImporter.flush();

		VehicleType av = VehicleUtils.createVehicleType(Id.create("av", VehicleType.class));
		av.setFlowEfficiencyFactor(2.0);
		f.scenario.getVehicles().addVehicleType(av);

		VehicleType car = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
		car.setFlowEfficiencyFactor(1.0);
		f.scenario.getVehicles().addVehicleType(car);
		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 12000; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.scenario.getPopulation().addPerson(person);

			//every second plan gets a super flowy AV
			Vehicle vehicle = i % 2 == 1 ? VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), av) : VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), car);
			f.scenario.getVehicles().addVehicle(vehicle);
			VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));

		}
		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();


		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 3-4: " + volume[3]);
		System.out.println("#vehicles 4-5: " + volume[4]);
		System.out.println("#vehicles 5-6: " + volume[5]);
		System.out.println("#vehicles 6-7: " + volume[6]);
		System.out.println("#vehicles 7-8: " + volume[7]);
		System.out.println("#vehicles 8-9: " + volume[8]);


		Assertions.assertEquals(0, volume[5]);    // no vehicles
		Assertions.assertEquals(4005, volume[6]); // we should have half of the maximum flow in this hour * 1.5, because every second vehicle is super flowy
		Assertions.assertEquals(7995, volume[7]); // all the rest
		Assertions.assertEquals(0, volume[8]); // nothing

	}

	/**
	 * Tests flow efficiency factors, e.g. for AVs, are working also in combination with downscaling
	 *
	 * @author jfbischoff
	 */

	@Test
	void testFlowCapacityDrivingFlowEfficiencyFactorsWithDownscaling() {
		Fixture f = new Fixture();
		ScenarioImporter.flush();

		VehicleType av = VehicleUtils.createVehicleType(Id.create("av", VehicleType.class));
		av.setFlowEfficiencyFactor(2.0);
		f.scenario.getVehicles().addVehicleType(av);

		VehicleType car = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
		car.setFlowEfficiencyFactor(1.0);
		f.scenario.getVehicles().addVehicleType(car);

		f.config.hermes().setFlowCapacityFactor(0.1);
		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 1200; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.scenario.getPopulation().addPerson(person);

			//every second plan gets a super flowy AV
			Vehicle vehicle = i % 2 == 1 ? VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), av) : VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), car);
			f.scenario.getVehicles().addVehicle(vehicle);
			VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));
		}
		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();


		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 3-4: " + volume[3]);
		System.out.println("#vehicles 4-5: " + volume[4]);
		System.out.println("#vehicles 5-6: " + volume[5]);
		System.out.println("#vehicles 6-7: " + volume[6]);
		System.out.println("#vehicles 7-8: " + volume[7]);
		System.out.println("#vehicles 8-9: " + volume[8]);

		Assertions.assertEquals(0, volume[5], 1);    // no vehicles
		Assertions.assertEquals(401, volume[6], 1); // we should have half of the maximum flow in this hour * 1.3333, because every second vehicle is super flowy
		Assertions.assertEquals(799, volume[7], 1);  // all the rest
		Assertions.assertEquals(0, volume[8], 1);  // nothing
	}

	/**
	 * Tests flow efficiency factors, e.g. for AVs, are working with downscaling and a value <1 for flow efficiency
	 *
	 * @author jfbischoff
	 */

	@Test
	void testFlowCapacityEfficiencyFactorWithLowValueAndDownscaling() {
		Fixture f = new Fixture();
		ScenarioImporter.flush();

		VehicleType tractor = VehicleUtils.createVehicleType(Id.create("tractor", VehicleType.class));
		tractor.setFlowEfficiencyFactor(0.5);
		f.scenario.getVehicles().addVehicleType(tractor);
		f.config.hermes().setFlowCapacityFactor(0.1);
		VehicleType car = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
		car.setPcuEquivalents(1.0);
		f.scenario.getVehicles().addVehicleType(car);
		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 1200; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.scenario.getPopulation().addPerson(person);

			//every second person gets an unflowy, but speedy tractor

			Vehicle vehicle = i % 2 == 1 ? VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), tractor) : VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), car);
			f.scenario.getVehicles().addVehicle(vehicle);
			VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));

		}
		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 10*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();


		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 3-4: " + volume[3]);
		System.out.println("#vehicles 4-5: " + volume[4]);
		System.out.println("#vehicles 5-6: " + volume[5]);
		System.out.println("#vehicles 6-7: " + volume[6]);
		System.out.println("#vehicles 7-8: " + volume[7]);
		System.out.println("#vehicles 8-9: " + volume[8]);
		Assertions.assertEquals(0, volume[5], 1);    // no vehicles
		Assertions.assertEquals(201, volume[6], 1); // we should have half of the maximum flow in this hour * 1.3333, because every second vehicle is super flowy
		Assertions.assertEquals(400, volume[7], 1);
		Assertions.assertEquals(400, volume[8], 1);
		Assertions.assertEquals(199, volume[9], 1);

	}

	/**
	 * Tests that on a link with a flow capacity of 0.25 vehicles per time step, after the first vehicle
	 * at time step t, the second vehicle may pass in time step t + 4 and the third in time step t+8.
	 *
	 * @author michaz
	 */
	@Test
	void testFlowCapacityDrivingFraction() {
		Fixture f = new Fixture();
		ScenarioImporter.flush();
		f.link2.setCapacity(900.0); // One vehicle every 4 seconds

		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 3; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			/* exact dep. time: 6:29:48. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 10 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 10 + 1 = 12 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 12 = 7*3600 - 1812
			 */
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(1, 7*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		Assertions.assertEquals(1, volume[7*3600 - 1801]); // First vehicle
		Assertions.assertEquals(1, volume[7*3600 - 1801 + 4]); // Second vehicle
		Assertions.assertEquals(1, volume[7*3600 - 1801 + 8]); // Third vehicle
	}

}
