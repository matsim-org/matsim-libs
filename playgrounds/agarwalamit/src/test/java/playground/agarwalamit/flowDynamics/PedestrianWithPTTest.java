/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.flowDynamics;

import java.util.*;
import com.google.inject.CreationException;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

/**
 * @author amit copy of {@link CarPassingBusTest}
 *
 * Simulating walk trip (named as="pedestrian" and has characteristics same as car) and public transit trips together.
 * Most likely, using name as "walk" will not work (if walk is a network mode too) because,
 * walk (or transit_walk) mode is used in {@link org.matsim.pt.router.TransitRouterConfig} for teleportation between
 * first activity (home,work,...) and "pt interaction" activity.
 */

@RunWith(Parameterized.class)
public class PedestrianWithPTTest {

	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final boolean isUsingControler ;

	/*
	 * a switch to see the problem locally but let it pass on travis.
	 */
	private final boolean letTheTestFail = false;

	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils();

	private final String walkModeName;

	public PedestrianWithPTTest (boolean isUsingControler, String walkModeName) {
		this.isUsingControler = isUsingControler;
		this.walkModeName = walkModeName;
	}

	@Parameterized.Parameters(name = "{index}: isUsingControler == {0}")
	public static Collection<Object[]> parameterObjects () {
		Object [][] runMethod = new Object [][] {
				{true, "walk"},
				{false, "walk"},
				{true, "ped"},
				{false, "ped"}
		};
		return Arrays.asList(runMethod);
	}

	@Test
	public void runTest() {
		prepareConfig();
		createNetwork();
		createTransitSchedule();
		createVehicles();
		createPopulation();
		
		LinkEnterLeaveTimeEventHandler lelteh = new LinkEnterLeaveTimeEventHandler();
		
		if (isUsingControler && this.walkModeName.equals(TransportMode.walk) & ! letTheTestFail) {
			try {
				runSim(lelteh);
				Assert.assertTrue("Expected CreationException does not occur.", false);
			} catch (CreationException e) {
				Assert.assertTrue("Catched CreationException.", true);
			}
		} else {
			runSim(lelteh);

			Id<Vehicle> busId = Id.createVehicleId("bus_1");
			Id<Vehicle> carId = Id.createVehicleId(walkModeName+"User");

			Id<Link> linkId = Id.createLinkId("1011");
			//	first make sure pedestrian enter after bus
			double busEnterTime = lelteh.vehicleEnterLeaveTimes.get(busId).get(linkId).getFirst();
			double carEnterTime = lelteh.vehicleEnterLeaveTimes.get(carId).get(linkId).getFirst();

			Assert.assertEquals("Bus should enter before ."+walkModeName, busEnterTime < carEnterTime, true);

			// now check pedestrian left before bus

			double busLeaveTime = lelteh.vehicleEnterLeaveTimes.get(busId).get(linkId).getSecond();
			double carLeaveTime = lelteh.vehicleEnterLeaveTimes.get(carId).get(linkId).getSecond();

			Assert.assertEquals("Walk should leave before bus.", busLeaveTime > carLeaveTime, true);

			// now check for travel times
			double busTravelTime = busLeaveTime - busEnterTime; // should be = 500/5+1 = 101
			double carTravelTime = carLeaveTime - carEnterTime; // should be = 500/10+1 = 51

			Assert.assertEquals("Wrong bus travel time", busTravelTime, 101, MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong "+walkModeName+" travel time", carTravelTime, 51, MatsimTestUtils.EPSILON);
		}
	}

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.transit().setUseTransit(true);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue );
		config.qsim().setEndTime(24.0*3600);
		config.qsim().setMainModes(Arrays.asList("car",walkModeName)); // car is still necessary for PT simulation
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		config.travelTimeCalculator().setAnalyzedModes(StringUtils.join(new String[] { walkModeName}, ","));
		config.travelTimeCalculator().setFilterModes(true);
		config.plansCalcRoute().setNetworkModes(Arrays.asList(walkModeName));

		config.planCalcScore().getOrCreateModeParams(walkModeName).setConstant(0.);
		config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.pt).setBeelineDistanceFactor(1.3);
	}

	private void createNetwork() {
		Network network = this.scenario.getNetwork();
		network.setCapacityPeriod(3600.0);

		Node n01, n10, n11, n12, n13, n14;
		double x1 = -500;
		network.addNode(n01 = network.getFactory().createNode(Id.create("01", Node.class), new Coord(x1, -100)));
		network.addNode(n10 = network.getFactory().createNode(Id.create("10", Node.class), new Coord(0, 0)));
		network.addNode(n11 = network.getFactory().createNode(Id.create("11", Node.class), new Coord(500, 0)));
		network.addNode(n12 = network.getFactory().createNode(Id.create("12", Node.class), new Coord(1000, 0)));
		network.addNode(n13 = network.getFactory().createNode(Id.create("13", Node.class), new Coord(1500, 0)));
		network.addNode(n14 = network.getFactory().createNode(Id.create("14", Node.class), new Coord(2000, 50)));

		Link l;
		l = network.getFactory().createLink(Id.create("0110", Link.class), n01, n10); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1011", Link.class), n10, n11); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1112", Link.class), n11, n12); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1213", Link.class), n12, n13); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);
		l = network.getFactory().createLink(Id.create("1314", Link.class), n13, n14); l.setLength(500.0); l.setFreespeed(10.0);	l.setCapacity(4000.0); l.setNumberOfLanes(1); network.addLink(l);

	}

	private void createTransitSchedule() {
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory builder = schedule.getFactory();

		ArrayList<TransitRouteStop> stopListA = new ArrayList<>();

		// create stops
		TransitStopFacility stopFac;
		TransitRouteStop stop;

		stopFac = builder.createTransitStopFacility(Id.create("11", TransitStopFacility.class), new Coord(500, 0), false); stopFac.setLinkId(Id.create("1011", Link.class)); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 0, 10); stopListA.add(stop);
		stopFac = builder.createTransitStopFacility(Id.create("13", TransitStopFacility.class), new Coord(1500, 0), false); stopFac.setLinkId(Id.create("1213", Link.class)); schedule.addStopFacility(stopFac);
		stop = builder.createTransitRouteStop(stopFac, 50, 60); stopListA.add(stop);

		// transit line A		
		Link startLinkA = this.scenario.getNetwork().getLinks().get(Id.create("0110", Link.class));
		Link endLinkA = this.scenario.getNetwork().getLinks().get(Id.create("1314", Link.class));
		NetworkRoute networkRouteA = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, startLinkA.getId(), endLinkA.getId());

		ArrayList<Id<Link>> linkListA = new ArrayList<>();
		linkListA.add(Id.create("1011", Link.class)); 
		linkListA.add(Id.create("1112", Link.class)); 
		linkListA.add(Id.create("1213", Link.class));

		networkRouteA.setLinkIds(startLinkA.getId(), linkListA, endLinkA.getId());
		TransitRoute tRouteA = builder.createTransitRoute(Id.create("A", TransitRoute.class), networkRouteA, stopListA, "bus");
		TransitLine tLineA = builder.createTransitLine(Id.create("line A", TransitLine.class)); tLineA.addRoute(tRouteA); schedule.addTransitLine(tLineA);

		Departure dep = builder.createDeparture(Id.create("bus_1", Departure.class), 7*3600.0 );
		dep.setVehicleId(Id.create("bus_1", Vehicle.class));
		tRouteA.addDeparture(dep);
	}

	private void createVehicles() {
		{
			Vehicles vehicles = this.scenario.getTransitVehicles();
			VehiclesFactory vb = vehicles.getFactory();

			// bus like
			VehicleType busType = vb.createVehicleType(Id.create("bus", VehicleType.class));
			busType.setMaximumVelocity(5.0);
			busType.setPcuEquivalents(3.);
			VehicleCapacity capacity = vb.createVehicleCapacity();
			capacity.setSeats(Integer.valueOf(9999));
			capacity.setStandingRoom(Integer.valueOf(0));
			busType.setCapacity(capacity);
			vehicles.addVehicleType(busType);

			vehicles.addVehicle( vb.createVehicle(Id.create("bus_1", Vehicle.class), busType));
		}

		{
			Vehicles vehs = this.scenario.getVehicles();

			VehicleType carType = vehs.getFactory().createVehicleType(Id.create(walkModeName, VehicleType.class));
			carType.setMaximumVelocity(10.);
			carType.setPcuEquivalents(1.);
			vehs.addVehicleType(carType);

			vehs.addVehicle(vehs.getFactory().createVehicle(Id.create(walkModeName+"User", Vehicle.class), carType) );
		}
	}

	private void createPopulation() {
		Population population = this.scenario.getPopulation();
		PopulationFactory pb = population.getFactory();

		Person person = pb.createPerson(Id.create(walkModeName+"User", Person.class));
		Plan plan = pb.createPlan();

		Link startLinkA = this.scenario.getNetwork().getLinks().get(Id.create("0110", Link.class));
		Link endLinkA = this.scenario.getNetwork().getLinks().get(Id.create("1314", Link.class));

		Activity act1 = pb.createActivityFromLinkId("home", startLinkA.getId());
		act1.setEndTime(7*3600. + 49.);
		Leg leg = pb.createLeg(walkModeName);

		NetworkRoute networkRouteA = this.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, startLinkA.getId(), endLinkA.getId());

		ArrayList<Id<Link>> linkListA = new ArrayList<>();
		linkListA.add(Id.create("1011", Link.class)); 
		linkListA.add(Id.create("1112", Link.class)); 
		linkListA.add(Id.create("1213", Link.class));

		networkRouteA.setLinkIds(startLinkA.getId(), linkListA, endLinkA.getId());
		leg.setRoute(networkRouteA);

		Activity act2 = pb.createActivityFromLinkId("work", endLinkA.getId());

		population.addPerson(person);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);
	}

	private void runSim(LinkEnterLeaveTimeEventHandler eventHandler) {
		if (isUsingControler) {
			scenario.getConfig().controler().setOutputDirectory(helper.getOutputDirectory());
			scenario.getConfig().controler().setLastIteration(0);

			PlanCalcScoreConfigGroup.ActivityParams homeAct = new PlanCalcScoreConfigGroup.ActivityParams("home");
			PlanCalcScoreConfigGroup.ActivityParams workAct = new PlanCalcScoreConfigGroup.ActivityParams("work");
			homeAct.setTypicalDuration(12. * 3600.);
			workAct.setTypicalDuration(8. * 3600.);

			scenario.getConfig().planCalcScore().addActivityParams(homeAct);
			scenario.getConfig().planCalcScore().addActivityParams(workAct);

			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(eventHandler);
				}
			});
			controler.run();
		} else {
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(eventHandler);

			QSim qSim = QSimUtils.createDefaultQSim(this.scenario,events);
			qSim.run();
		}
	}
	
	private static class LinkEnterLeaveTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Tuple<Double, Double>>> vehicleEnterLeaveTimes = new HashMap<>();

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Tuple<Double, Double>> times = this.vehicleEnterLeaveTimes.get(event.getVehicleId());
			if (times == null) {
				times = new HashMap<>();
				times.put(event.getLinkId(), new Tuple<>(0., Double.NEGATIVE_INFINITY));
				this.vehicleEnterLeaveTimes.put(event.getVehicleId(), times);
			}
			
			Tuple<Double, Double> d = times.get(event.getLinkId());
			if (d == null) {
				d = new Tuple<>(event.getTime(), Double.NEGATIVE_INFINITY);
			}
			
			times.put(event.getLinkId(), d);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Tuple<Double, Double>> times = this.vehicleEnterLeaveTimes.get(event.getVehicleId());
			if (times == null) {
				times = new HashMap<>();
				times.put(event.getLinkId(), new Tuple<>(Double.NEGATIVE_INFINITY, 0.));
				this.vehicleEnterLeaveTimes.put(event.getVehicleId(), times);
			}
			
			Tuple<Double, Double> d = times.get(event.getLinkId());
			d = new Tuple<>(d.getFirst(), event.getTime());
			times.put(event.getLinkId(), d);
		}

		@Override
		public void reset(int iteration) {
		}
	}
}