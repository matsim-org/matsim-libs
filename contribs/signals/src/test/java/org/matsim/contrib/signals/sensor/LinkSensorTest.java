package org.matsim.contrib.signals.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

public class LinkSensorTest {

	@Test
	void testGetAvgVehiclesPerSecondAfterBucketCollection() {
		//test if average is working for constant flow
		LinkSensor sensor = new LinkSensor(null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second
			if (time % 3 == 0)
				sensor.handleEvent(new LinkEnterEvent(time, Id.createVehicleId("exampleCar"), null));
			//after enough buckets are collected the average should be 0,333 per second.
			if (time > 60)
				assertEquals(1.0/3.0, sensor.getAvgVehiclesPerSecond(time), 0.04);
		}
	}

	@Test
	void testGetAvgVehiclesPerSecondDuringBucketCollection() {
		//test if average is working for constant flow
		LinkSensor sensor = new LinkSensor(null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second
			if (time % 3 == 0)
				sensor.handleEvent(new LinkEnterEvent(time, Id.createVehicleId("exampleCar"), null));
			//after enough buckets are collected the average should be 0,333 per second.
			if (time > 15)
				assertEquals(1.0/3.0, sensor.getAvgVehiclesPerSecond(time), 0.04);
		}
	}

	@Test
	void testGetAvgVehiclesPerSecondWithNoTrafficForTwoBucket() {
		//test if average is working for constant flow
		LinkSensor sensor = new LinkSensor(null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second, but generate two empty buckets (150-165 and 165-180)
			if (time % 3 == 0 && !(time >= 120 && time < 150))
				sensor.handleEvent(new LinkEnterEvent(time, Id.createVehicleId("exampleCar"), null));
			//after generating two empty bucket the avg should be 0,1666 for two buket collection times
			if (time > 150 && time < 195)
				assertEquals((1.0/3.0)/(4.0/2.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after a bucket with vehicles is colleted now the avg should be 0,25 since there is still an empty bucket
			if (time > 195 && time < 210)
				assertEquals((1.0/3.0)/(4.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after four buckets are collected again (150+60)the average should be 0,333 per second.
			if (time > 210)
				assertEquals((1.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
		}
	}

	@Test
	void testGetAvgVehiclesPerSecondWithNoTrafficForTwoBucketWhileHavingNotEnoughBuckets() {
		//test if average is working for constant flow
		LinkSensor sensor = new LinkSensor(null);
		sensor.registerAverageVehiclesPerSecondToMonitor(60, 15);
		for (int time = 0; time <= 3600; time++) {
			//generate a vehicle every third second, but generate two empty buckets (150-165 and 165-180)
			if (time % 3 == 0 && !(time >= 15 && time < 45)) {
				sensor.handleEvent(new LinkEnterEvent(time, Id.createVehicleId("exampleCar"), null));
			}
			//after generating two empty bucket the avg should be 0,1111 for two bucket collection times since there's only one full bucket (second 0 to 15) of total three
			if (time > 45 && time < 60)
				assertEquals((1.0/3.0)/(3.0/1.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after collection another full bucket it should be 0,1666 in avg.
			if (time > 60 && time < 90)
				assertEquals((1.0/3.0)/(4.0/2.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after the first empty bucket is removed and avg should be 0,25 since there is still one empty bucket
			if (time > 90 && time < 105)
				assertEquals((1.0/3.0)/(4.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
			//after four buckets are collected again (45+60)the average should be 0,333 per second.
			if (time > 105)
				assertEquals((1.0/3.0), sensor.getAvgVehiclesPerSecond(time), 0.02);
		}
	}

	@Test
	void testClassicBehaviour() {
		LinkSensor sensor = new LinkSensor(null);
		sensor.registerAverageVehiclesPerSecondToMonitor();
		for (int time = 0; time <= 3600; time++) {
			if (time > 30 && time != 45 && !(time > 100 && time < 129)) {
				sensor.handleEvent(new LinkEnterEvent(time, Id.createVehicleId("exampleCar"), null));
			}
			if (time <= 30)
				assertEquals(0.0, sensor.getAvgVehiclesPerSecond(time), 0.02);
			if (time == 32)
				assertEquals(1.0, sensor.getAvgVehiclesPerSecond(time), 0.02);
			if (time > 50)
				assertTrue(sensor.getAvgVehiclesPerSecond(time) < 1.0);
			if (time == 1000)
				assertTrue(sensor.getAvgVehiclesPerSecond(time)> 0.96);
		}
	}
	
	
	/*
	 * ----------------- 
	 * The following two tests are moved from former DgSensorTest.
	 * It can be that they test behavior that is already captured by the other tests
	 * above. Have not checked this. Theresa, aug'18.
	 * ----------------- 
	 */
	
	/**
	 * Creates a Network with one 600 m link fs 6 -> fs_tt 100 s and flow_cap = 1 veh/s
	 * 1 ---------- 2
	 */
	private Scenario createScenario(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n1 = nf.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		net.addNode(n1);
		Node n2 = nf.createNode(Id.create(2, Node.class), new Coord((double) 500, (double) 0));
		net.addNode(n2);
		Link l = nf.createLink(Id.create(1, Link.class), n1, n2);
		net.addLink(l);
		l.setLength(600.0);
		l.setCapacity(3600);
		l.setFreespeed(6.0);
		return sc;
	}


	@Test
	void testSensorNumberOfCarsMonitoring(){
		Scenario sc = this.createScenario();
		Link link = sc.getNetwork().getLinks().get(Id.create(1, Link.class));
		LinkSensor sensor = new LinkSensor(link);
		int numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(0, numberOfCars);
		
		Id<Person> agId2 = Id.createPersonId(2);
		Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
		
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, vehId1, link.getId());
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(1, numberOfCars);
		//expect NullPointerException as feature is still switched of
		NullPointerException e = null;
		try {
			sensor.getNumberOfCarsInDistance(600.0, 0.0);
		}
		catch (NullPointerException ex){
			e = ex;
		}
		Assertions.assertNotNull(e);

		LinkEnterEvent enterEvent2 = new LinkEnterEvent(10.0, vehId2, link.getId());
		sensor.handleEvent(enterEvent2);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(2, numberOfCars);
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(100.0, vehId1, link.getId());
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(1, numberOfCars);
		
		VehicleLeavesTrafficEvent link2WaitEvent = new VehicleLeavesTrafficEvent(110.0, agId2, link.getId(), vehId2, TransportMode.car, 1.0);
		sensor.handleEvent(link2WaitEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(0, numberOfCars);
		
		PersonEntersVehicleEvent enterVehEvent = new PersonEntersVehicleEvent(120., agId2, vehId2);
		sensor.handleEvent(enterVehEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(1, numberOfCars);

		leaveEvent = new LinkLeaveEvent(120.0, vehId2, link.getId());
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(0, numberOfCars);
	}

	@Test
	void testSensorDistanceMonitoring(){
		Scenario sc = this.createScenario();
		Link link = sc.getNetwork().getLinks().get(Id.create(1, Link.class));
		LinkSensor sensor = new LinkSensor(link);
		sensor.registerDistanceToMonitor(100.0);
		int numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(0, numberOfCars);
		int numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 0.0);
		Assertions.assertEquals(0, numberOfCarsInDistance);
		
		Id<Person> agId2 = Id.createPersonId(2);
		Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
		
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, vehId1, link.getId());
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(1, numberOfCars);

		enterEvent = new LinkEnterEvent(1.0, vehId2, link.getId());
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assertions.assertEquals(2, numberOfCars);

		//expect NullPointerException as feature is not switched on for distance 500.0 m
		NullPointerException e = null;
		try {
			sensor.getNumberOfCarsInDistance(500.0, 0.0);
		}
		catch (NullPointerException ex){
			e = ex;
		}
		Assertions.assertNotNull(e);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 10.0);
		Assertions.assertEquals(0, numberOfCarsInDistance);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 83.0);
		Assertions.assertEquals(0, numberOfCarsInDistance);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 84.0);
		Assertions.assertEquals(1, numberOfCarsInDistance);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 85.0);
		Assertions.assertEquals(2, numberOfCarsInDistance);
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(100.0, vehId1, link.getId());
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 100.0);
		Assertions.assertEquals(1, numberOfCarsInDistance);
		
		VehicleLeavesTrafficEvent link2WaitEvent = new VehicleLeavesTrafficEvent(101.0, agId2, link.getId(), vehId2, TransportMode.car, 1.0);
		sensor.handleEvent(link2WaitEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 101.0);
		Assertions.assertEquals(0, numberOfCarsInDistance);
		
		PersonEntersVehicleEvent enterVehEvent = new PersonEntersVehicleEvent(120., agId2, vehId2);
		sensor.handleEvent(enterVehEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assertions.assertEquals(1, numberOfCarsInDistance);

		leaveEvent = new LinkLeaveEvent(120.0, vehId2, link.getId());
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assertions.assertEquals(0, numberOfCarsInDistance);
		
	}
	
}
