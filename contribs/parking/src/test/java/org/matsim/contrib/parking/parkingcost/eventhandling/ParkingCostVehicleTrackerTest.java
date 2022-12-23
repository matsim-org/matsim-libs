package org.matsim.contrib.parking.parkingcost.eventhandling;


import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingcost.config.ParkingCostConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.testcases.utils.EventsLogger;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser, jfbischoff (SBB)
 */
public class ParkingCostVehicleTrackerTest {

	@Test
	public void testParkingCostEvents() {
		Fixture f = new Fixture();

		f.events.addHandler(new EventsLogger());
		ParkingCostConfigGroup parkingCostConfigGroup = new ParkingCostConfigGroup();
		parkingCostConfigGroup.activityTypesWithoutParkingCost = "home";
		MainModeParkingCostVehicleTracker tracker = new MainModeParkingCostVehicleTracker(TransportMode.car, parkingCostConfigGroup);
		tracker.events = f.events;
		tracker.network = f.scenario.getNetwork();
		f.events.addHandler(tracker);
		EventsCollector collector = new EventsCollector();
		f.events.addHandler(collector);

		Id<Person> personId = Id.create(1, Person.class);
		Id<Vehicle> vehicleId = Id.create(2, Vehicle.class);
		Id<Link> linkHome = Id.create("L", Link.class);
		Id<Link> linkWork = Id.create("B", Link.class);
		Id<Link> linkShop = Id.create("T", Link.class);

		double hourlyParkingCostWork = 20; // this is the value of at_car in zone Bern
		double hourlyParkingCostShop = 3; // this is the value of at_car in zone Thun
		f.scenario.getNetwork().getLinks().get(linkWork).getAttributes().putAttribute("pc_car", hourlyParkingCostWork);
		f.scenario.getNetwork().getLinks().get(linkShop).getAttributes().putAttribute("pc_car", hourlyParkingCostShop);
		f.events.processEvent(new VehicleEntersTrafficEvent(7.00 * 3600, personId, linkHome, vehicleId, "car", 1.0));
		f.events.processEvent(new VehicleLeavesTrafficEvent(7.25 * 3600, personId, linkWork, vehicleId, "car", 1.0));
		f.events.processEvent(new ActivityStartEvent(7.25 * 3600, personId, linkWork, null, "work", null));
		Assert.assertEquals(3, collector.getEvents().size());

		f.events.processEvent(new VehicleEntersTrafficEvent(12.00 * 3600, personId, linkWork, vehicleId, "car", 1.0));
		Assert.assertEquals(5, collector.getEvents().size());

		Assert.assertEquals(PersonMoneyEvent.class, collector.getEvents().get(3).getClass());
		PersonMoneyEvent parkingEvent1 = (PersonMoneyEvent) collector.getEvents().get(3);
		Assert.assertEquals(personId, parkingEvent1.getPersonId());
		Assert.assertEquals(12.00 * 3600, parkingEvent1.getTime(), 1e-8);
		Assert.assertEquals((12 - 7.25) * hourlyParkingCostWork, -parkingEvent1.getAmount(), 1e-8);

		f.events.processEvent(new VehicleLeavesTrafficEvent(12.25 * 3600, personId, linkShop, vehicleId, "car", 1.0));
		f.events.processEvent(new ActivityStartEvent(12.25 * 3600, personId, linkShop, null, "shop", null));
		Assert.assertEquals(7, collector.getEvents().size());

		f.events.processEvent(new VehicleEntersTrafficEvent(13.00 * 3600, personId, linkShop, vehicleId, "car", 1.0));
		Assert.assertEquals(9, collector.getEvents().size());

		Assert.assertEquals(PersonMoneyEvent.class, collector.getEvents().get(7).getClass());
		PersonMoneyEvent parkingEvent2 = (PersonMoneyEvent) collector.getEvents().get(7);
		Assert.assertEquals(personId, parkingEvent2.getPersonId());
		Assert.assertEquals(13.00 * 3600, parkingEvent2.getTime(), 1e-8);
		Assert.assertEquals((13 - 12.25) * hourlyParkingCostShop, -parkingEvent2.getAmount(), 1e-8);

		f.events.processEvent(new VehicleLeavesTrafficEvent(13.25 * 3600, personId, linkHome, vehicleId, "car", 1.0));
		f.events.processEvent(new ActivityStartEvent(13.25 * 3600, personId, linkHome, null, "home", null));
		Assert.assertEquals(11, collector.getEvents().size());

		f.events.processEvent(new VehicleEntersTrafficEvent(15.00 * 3600, personId, linkShop, vehicleId, "car", 1.0));
		Assert.assertEquals(12, collector.getEvents().size()); // there should be no parking cost event at home

		f.events.processEvent(new VehicleLeavesTrafficEvent(15.25 * 3600, personId, linkWork, vehicleId, "car", 1.0));
		f.events.processEvent(new ActivityStartEvent(13.25 * 3600, personId, linkWork, null, "shop", null));
		Assert.assertEquals(14, collector.getEvents().size());

		f.events.processEvent(new VehicleEntersTrafficEvent(18.00 * 3600, personId, linkWork, vehicleId, "car", 1.0));
		Assert.assertEquals(16, collector.getEvents().size());

		Assert.assertEquals(PersonMoneyEvent.class, collector.getEvents().get(14).getClass());
		PersonMoneyEvent parkingEvent3 = (PersonMoneyEvent) collector.getEvents().get(14);
		Assert.assertEquals(personId, parkingEvent3.getPersonId());
		Assert.assertEquals(18.00 * 3600, parkingEvent3.getTime(), 1e-8);
		Assert.assertEquals((18 - 15.25) * hourlyParkingCostWork, -parkingEvent3.getAmount(), 1e-8);

		f.events.processEvent(new VehicleLeavesTrafficEvent(18.50 * 3600, personId, linkHome, vehicleId, "car", 1.0));
		f.events.processEvent(new ActivityStartEvent(18.00 * 3600, personId, linkHome, null, "shop", null));
		Assert.assertEquals(18, collector.getEvents().size());
	}

	/**
	 * Creates a simple test scenario matching the accesstime_zone.SHP test file.
	 */
	private static class Fixture {

		final Config config;
		final Scenario scenario;
		EventsManager events;

		public Fixture() {
			this.config = ConfigUtils.createConfig();
			prepareConfig();
			this.scenario = ScenarioUtils.createScenario(this.config);
			createNetwork();
			prepareEvents();
		}

		private void prepareConfig() {
			ParkingCostConfigGroup parkingConfig = ConfigUtils.addOrGetModule(this.config, ParkingCostConfigGroup.class);
			parkingConfig.modesWithParkingCosts = TransportMode.car;
		}

		private void createNetwork() {
			Network network = this.scenario.getNetwork();
			NetworkFactory nf = network.getFactory();

			Node nL1 = nf.createNode(Id.create("L1", Node.class), new Coord(545000, 150000));
			Node nL2 = nf.createNode(Id.create("L2", Node.class), new Coord(540000, 165000));
			Node nB1 = nf.createNode(Id.create("B1", Node.class), new Coord(595000, 205000));
			Node nB2 = nf.createNode(Id.create("B2", Node.class), new Coord(605000, 195000));
			Node nT1 = nf.createNode(Id.create("T1", Node.class), new Coord(610000, 180000));
			Node nT2 = nf.createNode(Id.create("T2", Node.class), new Coord(620000, 175000));

			network.addNode(nL1);
			network.addNode(nL2);
			network.addNode(nB1);
			network.addNode(nB2);
			network.addNode(nT1);
			network.addNode(nT2);

			Link lL = createLink(nf, "L", nL1, nL2, 500, 1000, 10);
			Link lLB = createLink(nf, "LB", nL2, nB1, 5000, 2000, 25);
			Link lB = createLink(nf, "B", nB1, nB2, 500, 1000, 10);
			Link lBT = createLink(nf, "BT", nB2, nT1, 5000, 2000, 25);
			Link lT = createLink(nf, "T", nT1, nT2, 500, 1000, 10);
			Link lTL = createLink(nf, "TL", nT2, nL1, 5000, 2000, 25);

			network.addLink(lL);
			network.addLink(lLB);
			network.addLink(lB);
			network.addLink(lBT);
			network.addLink(lT);
			network.addLink(lTL);
		}

		private Link createLink(NetworkFactory nf, String id, Node fromNode, Node toNode, double length, double capacity, double freespeed) {
			Link l = nf.createLink(Id.create(id, Link.class), fromNode, toNode);
			l.setLength(length);
			l.setCapacity(capacity);
			l.setFreespeed(freespeed);
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
			l.setNumberOfLanes(1);
			return l;
		}


		private void prepareEvents() {
			this.events = EventsUtils.createEventsManager(this.config);
		}

	}
}
