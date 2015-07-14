/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.pt.router;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener.VehicleLoad;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.vividsolutions.jts.util.Assert;

/**
 * 
 * Actually this is more an integration-test. But, from my point of view this is the 
 * best (and easiest) way to test the outcome of the routing.
 * 
 * @author droeder
 *
 */
public class WagonSimRouterNetworkTravelDistutilityAndTravelTimeTest extends MatsimTestCase{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(WagonSimRouterNetworkTravelDistutilityAndTravelTimeTest.class);

	public WagonSimRouterNetworkTravelDistutilityAndTravelTimeTest() {
	}

	@Test
	public void test() {
		// prepare the scenario
		Scenario sc = createScenario();
		createPerson(sc);
		createPersonObjectAttributes(sc);
		createNetwork(sc);
		createSchedule(sc);
		createVehicles(sc);
		ObjectAttributes locomotiveAttributes = createVehicleLinkSpeedAttributes(sc);
		VehicleLoad vehLoad = new VehicleLoad(sc.getTransitSchedule());
		Config config = sc.getConfig(); 
		PreparedTransitSchedule prepSchedule = new PreparedTransitSchedule(sc.getTransitSchedule());
		TransitRouterConfig routerConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		WagonSimRouterNetworkTravelDistutilityAndTravelTime tt = 
				new WagonSimRouterNetworkTravelDistutilityAndTravelTime(
											routerConfig, 
											prepSchedule, 
											vehLoad, 
											locomotiveAttributes, 
											sc.getPopulation().getPersonAttributes());
		TransitRouterNetwork network = WagonSimRouterFactoryImpl.createRouterNetwork(sc.getTransitSchedule(), 0);
		TransitRouter router = new TransitRouterImpl(routerConfig, prepSchedule, network, tt, tt);
		Person p = sc.getPopulation().getPersons().get(Id.create("1", Person.class));
		
		// calc the first best route without any congestion
		List<Leg> legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 0, p);
		Assert.equals(210., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 110, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 111, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 410, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 411, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 710, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		// we missed the last departure, thus, the (same) departure of the next day is chosen (independent from congestion)
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 711, p);
		Assert.equals(85899., legs.get(1).getTravelTime());
		
		// only the first departure is overcrowded
		vehLoad.entering(WagonSimVehicleLoadListener.getIdentifier("v1", "f1", 0), 50000, 990);
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 0, p);
		Assert.equals(510., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 110, p);
		Assert.equals(400., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 111, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 410, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 411, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 710, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		// we missed the last departure, thus, the (same) departure of the next day is chosen (independent from congestion)
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 711, p);
		Assert.equals(86199., legs.get(1).getTravelTime());
		
		// first and second departure is overcrowded
		vehLoad.entering(WagonSimVehicleLoadListener.getIdentifier("v2", "f1", 0), 50000, 990);
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 0, p);
		Assert.equals(810., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 110, p);
		Assert.equals(700., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 111, p);
		Assert.equals(699., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 410, p);
		Assert.equals(400., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 411, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 710, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		// we missed the last departure, thus, the (same) departure of the next day is chosen (independent from congestion)
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 711, p);
		Assert.equals(86499., legs.get(1).getTravelTime());
		
		// all departures are overcrowded (disutility for all connections). Therefore,
		// we get the same results as they are returned without any congestion.
		vehLoad.entering(WagonSimVehicleLoadListener.getIdentifier("v3", "f1", 0), 50000, 990);
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 0, p);
		Assert.equals(210., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 110, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 111, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 410, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 411, p);
		Assert.equals(399., legs.get(1).getTravelTime());
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 710, p);
		Assert.equals(100., legs.get(1).getTravelTime());
		// we missed the last departure, thus, the (same) departure of the next day is chosen (independent from congestion)
		legs = router.calcRoute(new CoordImpl(0,0), new CoordImpl(100, 100), 711, p);
		Assert.equals(85899., legs.get(1).getTravelTime());
	}
	
	/**
	 * @return
	 */
	private Scenario createScenario() {
		Config config = super.loadConfig(null);
		config.controler().setMobsim(QSimConfigGroup.GROUP_NAME);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.qsim().setEndTime(1000);
		ActivityParams home = new ActivityParams("h");
		home.setTypicalDuration(99);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("w");
		work.setTypicalDuration(99);
		config.planCalcScore().addActivityParams(work);
		// something very small
		config.plansCalcRoute().setTeleportedModeSpeed("walk", 0.00000000001);
		Scenario sc = ScenarioUtils.createScenario(config);
		sc.getConfig().transit().setUseTransit(true);
		return sc;
	}
	
	/**
	 * @param sc 
	 * @return
	 */
	private void createPerson(Scenario sc) {
		PopulationFactory factory =  sc.getPopulation().getFactory();
		Plan plan = factory.createPlan();
		Activity home = factory.createActivityFromCoord("h", sc.createCoord(0, 0));
		home.setEndTime(0);
		plan.addActivity(home);
		Leg l = factory.createLeg(TransportMode.pt);
		Person p = factory.createPerson(Id.create(1, Person.class));
		plan.addLeg(l);
		plan.addActivity(factory.createActivityFromCoord("w", sc.createCoord(100, 100)));
		p.addPlan(plan);
		sc.getPopulation().addPerson(p);
	}
	
	private void createPersonObjectAttributes(Scenario sc){
		ObjectAttributes oa = sc.getPopulation().getPersonAttributes();
		for(Id p: sc.getPopulation().getPersons().keySet()){
			oa.putAttribute(p.toString(), WagonSimConstants.WAGON_GROSS_WEIGHT, 20000.);
			oa.putAttribute(p.toString(), WagonSimConstants.WAGON_LENGTH, 20.);
		}
	}
	
	/**
	 * @param sc
	 */
	private void createNetwork(Scenario sc) {
		NetworkFactory factory =  sc.getNetwork().getFactory();
		Node one, two, three;
		one = factory.createNode(Id.create("n1", Node.class), sc.createCoord(-100, -100));
		two = factory.createNode(Id.create("n2", Node.class), sc.createCoord(0, 0));
		three = factory.createNode(Id.create("n3", Node.class), sc.createCoord(100, 100));
		sc.getNetwork().addNode(one);
		sc.getNetwork().addNode(two);
		sc.getNetwork().addNode(three);
		sc.getNetwork().addLink(factory.createLink(Id.create("l1", Link.class), one, two));
		sc.getNetwork().addLink(factory.createLink(Id.create("l2", Link.class), two, three));
		sc.getConfig().transit().setUseTransit(true);
	}
	
	/**
	 * @param sc
	 */
	private void createSchedule(Scenario sc) {
		TransitSchedule sched = sc.getTransitSchedule();
		TransitScheduleFactory schedFactory = sched.getFactory();
		TransitLine line = schedFactory.createTransitLine(Id.create("wagonLine", TransitLine.class));
		
		TransitStopFacility f1 = schedFactory.createTransitStopFacility(Id.create("f1", TransitStopFacility.class), sc.createCoord(0, 0), false);
		f1.setLinkId(Id.create("l1", Link.class));
		TransitStopFacility f2 = schedFactory.createTransitStopFacility(Id.create("f2", TransitStopFacility.class), sc.createCoord(100, 100), false);
		f2.setLinkId(Id.create("l2", Link.class));
		sched.addStopFacility(f1);
		sched.addStopFacility(f2);
		
		TransitRouteStop s1 = schedFactory.createTransitRouteStop(f1, 0, 10);
		s1.setAwaitDepartureTime(true);
		TransitRouteStop s2 = schedFactory.createTransitRouteStop(f2, 110, 120);
		s2.setAwaitDepartureTime(true);
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(s1);
		stops.add(s2);
		
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(Id.create("l1", Link.class), new ArrayList<Id<Link>>(), Id.create("l2", Link.class)); 
		TransitRoute route = schedFactory.createTransitRoute(Id.create("wagonRoute", TransitRoute.class), networkRoute, stops, TransportMode.pt);
		Departure d = schedFactory.createDeparture(Id.create("d1", Departure.class), 100);
		d.setVehicleId(Id.create("v1", Vehicle.class));
		route.addDeparture(d);
		
		d = schedFactory.createDeparture(Id.create("d2", Departure.class), 400);
		d.setVehicleId(Id.create("v2", Vehicle.class));
		route.addDeparture(d);
		
		d = schedFactory.createDeparture(Id.create("d3", Departure.class), 700);
		d.setVehicleId(Id.create("v3", Vehicle.class));
		route.addDeparture(d);
		
		line.addRoute(route);
		sched.addTransitLine(line);
	}

	/**
	 * @param sc
	 */
	@SuppressWarnings("deprecation")
	private void createVehicles(Scenario sc) {
		Vehicles veh = ((ScenarioImpl) sc).getTransitVehicles();
		VehiclesFactory factory = veh.getFactory();
		
		VehicleCapacity vc = factory.createVehicleCapacity();
		vc.setSeats(100);
		vc.setStandingRoom(0);
		
		VehicleType vt1 = factory.createVehicleType(Id.create("vt1", VehicleType.class));
		vt1.setAccessTime(10);
		vt1.setCapacity(vc);
		
		veh.addVehicleType(vt1);
		
		Vehicle v = factory.createVehicle(Id.create("v1", Vehicle.class), vt1);
		veh.addVehicle( v);
		
		v = factory.createVehicle(Id.create("v2", Vehicle.class), vt1);
		veh.addVehicle( v);
		
		v = factory.createVehicle(Id.create("v3", Vehicle.class), vt1);
		veh.addVehicle( v);
	}



	/**
	 * @param sc
	 * @return
	 */
	private ObjectAttributes createVehicleLinkSpeedAttributes(Scenario sc) {
		ObjectAttributes oa = new ObjectAttributes();
		for(Id<Vehicle> v: ((ScenarioImpl)sc).getTransitVehicles().getVehicles().keySet()){
			for(Id<Link> l: sc.getNetwork().getLinks().keySet()){
				oa.putAttribute(v.toString(), l.toString(), 10000.);
			}
			oa.putAttribute(v.toString(), WagonSimConstants.TRAIN_MAX_LENGTH, 1000.);
			oa.putAttribute(v.toString(), WagonSimConstants.TRAIN_MAX_WEIGHT, 80000.);
		}
		return oa;
	}



	

}

