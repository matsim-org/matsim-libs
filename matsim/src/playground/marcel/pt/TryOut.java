/* *********************************************************************** *
 * project: org.matsim.*
 * TryOut.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel.pt;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.algorithms.EventWriterXML;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.world.World;

import playground.marcel.pt.events.ArrivalAtFacilityEvent;
import playground.marcel.pt.events.DepartureAtFacilityEvent;
import playground.marcel.pt.implementations.VehicleImpl;
import playground.marcel.pt.interfaces.DriverAgent;
import playground.marcel.pt.interfaces.Vehicle;
import playground.marcel.pt.tryout.BusDriver;
import playground.marcel.pt.utils.FacilityVisitors;

public class TryOut {


	private void simulatePtVehicle() {
		// create network
		final NetworkLayer network = createNetwork();
		Link link1 = network.getLink("1");
		Link link2 = network.getLink("2");
		Link link3 = network.getLink("3");
		Link link4 = network.getLink("4");
		Link link5 = network.getLink("5");
		Link link6 = network.getLink("6");
		Link link7 = network.getLink("7");
		Link link8 = network.getLink("8");

		// create needed facilities
		final Facilities facilities = createFacilities();
		Facility stop1 = facilities.getFacility(new IdImpl("stop1"));
		Facility stop2 = facilities.getFacility(new IdImpl("stop2"));
		Facility stop3 = facilities.getFacility(new IdImpl("stop3"));
		Facility stop4 = facilities.getFacility(new IdImpl("stop4"));
		Facility stop5 = facilities.getFacility(new IdImpl("stop5"));
		Facility stop6 = facilities.getFacility(new IdImpl("stop6"));

		// do some forbidden magic
		final World world = new World();
		world.setNetworkLayer(network);
		world.setFacilityLayer(facilities);
		world.complete();

		// test that the magic worked
		if (stop1.getLink() == null) {
			throw new RuntimeException("facilities should have a link by now.");
		}

		// create 1 Person Population
		final Population population = createPopulation(network, facilities);
		final Person person1 = population.getPerson(new IdImpl("1"));
		final Plan plan = person1.getPlans().get(0);

		// prepare events handling
		final Events events = new Events();
		final EventWriterXML eWriter = new EventWriterXML("events.xml");
		events.addHandler(eWriter);
		final FacilityVisitors fv = new FacilityVisitors();
		events.addHandler(fv);

		// create Bus with 1 schedule
		ArrayList<Facility> busStops = new ArrayList<Facility>(6);
		busStops.add(stop1);
		busStops.add(stop2);
		busStops.add(stop3);
		busStops.add(stop4);
		busStops.add(stop5);
		busStops.add(stop6);
		ArrayList<Link> busRoute = new ArrayList<Link>(6);
		busRoute.add(link1);
		busRoute.add(link2);
		busRoute.add(link3);
		busRoute.add(link4);
		busRoute.add(link5);
		busRoute.add(link6);

		DriverAgent busDriver = new BusDriver(busStops, busRoute, 7.25 * 3600);
		Vehicle bus = new VehicleImpl(5, events);
		bus.setDriver(busDriver);
		((BusDriver) busDriver).setVehicle(bus); // not happy with that, bus must know driver and driver must know bus...

		// run/simulate the things

		// - initialize
		Act act = (Act) plan.getActsLegs().get(0);
		fv.handleEvent(new ActStartEvent(0.0, person1, link2, act));

		busDriver.enterLink(link1);

		// - person departs at home
		act = (Act) plan.getActsLegs().get(0);
		fv.handleEvent(new ActEndEvent(7.0*3600, person1, link2, act));

		// - bus departs at stop1
		new DepartureAtFacilityEvent(7.25*3600, bus, stop1);

		// - person waits at stop2
		act = (Act) plan.getActsLegs().get(2);
		fv.handleEvent(new ActStartEvent(7.20*3600, person1, link3, act));

		// - bus crosses node 3
		busDriver.leaveLink(link1);
		new LinkLeaveEvent(7.27*3600, null/*bus*/, link1, null/*leg*/);
		new LinkEnterEvent(7.27*3600, null/*bus*/, link3, null/*leg*/);
		busDriver.enterLink(link3);

		// - bus arrives at stop2
		new ArrivalAtFacilityEvent(7.30*3600, bus, stop2);

		// - passengers in/out
		act = (Act) plan.getActsLegs().get(2);
		fv.handleEvent(new ActEndEvent(7.31*3600, person1, link3, act));


		// - bus departs at stop2
		new DepartureAtFacilityEvent(7.25*3600, bus, stop2);

		// - bus arrives/departs at stop3
		new ArrivalAtFacilityEvent(7.30*3600, bus, stop3);
		new DepartureAtFacilityEvent(7.30*3600+30, bus, stop3);

		// - bus arrives/departs at stop4
		new ArrivalAtFacilityEvent(7.40*3600, bus, stop4);
		new DepartureAtFacilityEvent(7.40*3600+30, bus, stop4);

		// - bus arrives/departs at stop5, person getting out
		new ArrivalAtFacilityEvent(7.50*3600, bus, stop5);

		act = (Act) plan.getActsLegs().get(4);
		fv.handleEvent(new ActStartEvent(7.51*3600, person1, link6, act));

		new DepartureAtFacilityEvent(7.52*3600, bus, stop5);

		// - person starts walking
		act = (Act) plan.getActsLegs().get(4);
		fv.handleEvent(new ActEndEvent(7.51*3600, person1, link6, act));

		// - bus arrives at stop6
		new ArrivalAtFacilityEvent(7.60*3600, bus, stop6);

		// - person arrives at work
		act = (Act) plan.getActsLegs().get(6);
		fv.handleEvent(new ActStartEvent(7.60*3600, person1, link7, act));

		// finish things
		eWriter.closeFile();

	}

	/**
	 * Creates a simple test network:
	 *
	 * <pre>
	 * (1)                                             (8)
	 *   \                                             /
	 *    1                                           7
	 *     \                                         /
	 *     (3)---3---(4)---4---(5)---5---(6)---6---(7)
	 *     /                                         \
	 *    2                                           8
	 *   /                                             \
	 * (2)                                             (9)
	 * </pre>
	 *
	 * All links have length 1000.0, free speed 10.0, capacity 3600.0 veh/h, and 1 lane.
	 *
	 * @return a test network
	 */
	private NetworkLayer createNetwork() {
		final String filename = "../thesis-data/examples/tryout/network.xml";
		NetworkLayer network = new NetworkLayer();

//		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 1000));
//		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(0, 0));
//		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(500, 500));
//		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(1500, 500));
//		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(2500, 500));
//		Node node6 = network.createNode(new IdImpl("6"), new CoordImpl(3500, 500));
//		Node node7 = network.createNode(new IdImpl("7"), new CoordImpl(4500, 500));
//		Node node8 = network.createNode(new IdImpl("8"), new CoordImpl(5000, 1000));
//		Node node9 = network.createNode(new IdImpl("9"), new CoordImpl(5000, 0));
//		network.createLink(new IdImpl("1"), node1, node3, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("2"), node2, node3, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("3"), node3, node4, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("4"), node4, node5, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("5"), node5, node6, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("6"), node6, node7, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("7"), node7, node8, 1000, 10.0, 3600.0, 1);
//		network.createLink(new IdImpl("8"), node7, node9, 1000, 10.0, 3600.0, 1);
//
//		new NetworkWriter(network, filename).write();

		new MatsimNetworkReader(network).readFile(filename);
		
		return network;
	}

	private Facilities createFacilities() {
		final String filename = "../thesis-data/examples/tryout/facilities.xml";
		final Facilities facilities = new Facilities();
		
//		final Facility home = facilities.createFacility(new IdImpl("home"), new CoordImpl(0, 900));
//		home.createActivity("home");
//		final Facility work = facilities.createFacility(new IdImpl("work"), new CoordImpl(0, 900));
//		work.createActivity("work");
//		final Facility stop1 = facilities.createFacility(new IdImpl("stop1"), new CoordImpl(0, 900));
//		stop1.createActivity("transitInteraction");
//		final Facility stop2 = facilities.createFacility(new IdImpl("stop2"), new CoordImpl(0, 900));
//		stop2.createActivity("transitInteraction");
//		final Facility stop3 = facilities.createFacility(new IdImpl("stop3"), new CoordImpl(0, 900));
//		stop3.createActivity("transitInteraction");
//		final Facility stop4 = facilities.createFacility(new IdImpl("stop4"), new CoordImpl(0, 900));
//		stop4.createActivity("transitInteraction");
//		final Facility stop5 = facilities.createFacility(new IdImpl("stop5"), new CoordImpl(0, 900));
//		stop5.createActivity("transitInteraction");
//		final Facility stop6 = facilities.createFacility(new IdImpl("stop6"), new CoordImpl(0, 900));
//		stop6.createActivity("transitInteraction");
//		
//		new FacilitiesWriter(facilities, filename).write();
		
		new MatsimFacilitiesReader(facilities).readFile(filename);

		return facilities;
	}

	private Population createPopulation(final NetworkLayer network, final Facilities facilities) {
		final String filename = "../thesis-data/examples/tryout/population.xml";
		final Population population = new Population(Population.NO_STREAMING);

		final Person person = new PersonImpl(new IdImpl("1"));
		population.addPerson(person);
		final Plan plan = person.createPlan(true);
		final Act homeAct = plan.createAct("home", facilities.getFacility(new IdImpl("home")));
		homeAct.setEndTime(7.0*3600);
		final Leg walk1 = plan.createLeg(BasicLeg.Mode.walk);
		final Act changeMode1 = plan.createAct("transitIteraction", facilities.getFacility(new IdImpl("stop2")));
		changeMode1.setDuration(0.0);
		final Leg bus = plan.createLeg(BasicLeg.Mode.pt);
		final Act changeMode2 = plan.createAct("transitIteraction", facilities.getFacility(new IdImpl("stop5")));
		changeMode2.setDuration(0.0);
		final Leg walk2 = plan.createLeg(BasicLeg.Mode.walk);
		final Act workAct = plan.createAct("work", facilities.getFacility(new IdImpl("work")));
		workAct.setDuration(8.0*3600);
		
		Gbl.createConfig(null); // required for plans.outputSample
		new PopulationWriter(population, filename, "v4").write();

		return population;
	}

	public static void main(final String[] args) {
		final TryOut tryOut = new TryOut();

		tryOut.simulatePtVehicle();
	}

}
