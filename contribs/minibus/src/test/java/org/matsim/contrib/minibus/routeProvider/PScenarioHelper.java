/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.routeProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.*;
import org.matsim.contrib.minibus.replanning.CreateNewPlan;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.schedule.CreatePStops;
import org.matsim.contrib.minibus.schedule.CreateStopsForAllCarLinks;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Creates an car grid network with one pt line.
 *  
 * @author aneumann
 *
 */
public class PScenarioHelper {
	
	public static Operator createTestCooperative(String testOutPutDir){
		Scenario sC = PScenarioHelper.createTestNetwork();
		
		PConfigGroup pC = new PConfigGroup();
		Operator coop = new BasicOperator(Id.create(pC.getPIdentifier() + 1, Operator.class), pC, new PFranchise(pC.getUseFranchise(), pC.getGridSize()));
		TransitSchedule sched = CreatePStops.createPStops(sC.getNetwork(), pC);
		RandomStopProvider randomStopProvider = new RandomStopProvider(pC, sC.getPopulation(), sched, null);
		
		PRouteProvider rP = new ComplexCircleScheduleProvider(sched, sC.getNetwork(), randomStopProvider, pC.getPlanningSpeedFactor(), pC.getVehicleMaximumVelocity(), pC.getDriverRestTime(), pC.getMode());
		
		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("900");
		parameter.add("0.0");
		TimeProvider tP = new TimeProvider(pC, testOutPutDir);
		CreateNewPlan strat = new CreateNewPlan(parameter);
		strat.setTimeProvider(tP);
		coop.init(rP, strat, 0, 0.0);
		
		return coop;
	}
	
	public static Scenario createTestNetwork() {
		
		final Scenario scenario;
		final Network network;
		final TransitSchedule schedule;
		final TransitLine line;
		final TransitRoute route;		
		
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		network = scenario.getNetwork();
		schedule = scenario.getTransitSchedule();
		NetworkFactory nf = network.getFactory();

		// creates nodes
		Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 1050, (double) 1050));
		Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 2050, (double) 2950));
		Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 3950, (double) 1050));

		Node n11 = nf.createNode(Id.create("11", Node.class), new Coord((double) 1000, (double) 1000));
		Node n12 = nf.createNode(Id.create("12", Node.class), new Coord((double) 1000, (double) 2000));
		Node n13 = nf.createNode(Id.create("13", Node.class), new Coord((double) 1000, (double) 3000));
		Node n14 = nf.createNode(Id.create("14", Node.class), new Coord((double) 1000, (double) 4000));

		Node n21 = nf.createNode(Id.create("21", Node.class), new Coord((double) 2000, (double) 1000));
		Node n22 = nf.createNode(Id.create("22", Node.class), new Coord((double) 2000, (double) 2000));
		Node n23 = nf.createNode(Id.create("23", Node.class), new Coord((double) 2000, (double) 3000));
		Node n24 = nf.createNode(Id.create("24", Node.class), new Coord((double) 2000, (double) 4000));

		Node n31 = nf.createNode(Id.create("31", Node.class), new Coord((double) 3000, (double) 1000));
		Node n32 = nf.createNode(Id.create("32", Node.class), new Coord((double) 3000, (double) 2000));
		Node n33 = nf.createNode(Id.create("33", Node.class), new Coord((double) 3000, (double) 3000));
		Node n34 = nf.createNode(Id.create("34", Node.class), new Coord((double) 3000, (double) 4000));

		Node n41 = nf.createNode(Id.create("41", Node.class), new Coord((double) 4000, (double) 1000));
		Node n42 = nf.createNode(Id.create("42", Node.class), new Coord((double) 4000, (double) 2000));
		Node n43 = nf.createNode(Id.create("43", Node.class), new Coord((double) 4000, (double) 3000));
		Node n44 = nf.createNode(Id.create("44", Node.class), new Coord((double) 4000, (double) 4000));
		
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		
		network.addNode(n11);
		network.addNode(n12);
		network.addNode(n13);
		network.addNode(n14);
		
		network.addNode(n21);
		network.addNode(n22);
		network.addNode(n23);
		network.addNode(n24);
		
		network.addNode(n31);
		network.addNode(n32);
		network.addNode(n33);
		network.addNode(n34);
		
		network.addNode(n41);
		network.addNode(n42);
		network.addNode(n43);
		network.addNode(n44);
		
		// create links
		
		network.addLink(nf.createLink(Id.create("A", Link.class), n14, n14));
		network.addLink(nf.createLink(Id.create("B", Link.class), n44, n44));
		network.addLink(nf.createLink(Id.create("C", Link.class), n11, n11));
		network.addLink(nf.createLink(Id.create("D", Link.class), n41, n41));
		
		network.addLink(nf.createLink(Id.create("11", Link.class), n1, n1));
		network.addLink(nf.createLink(Id.create("12", Link.class), n1, n2));
		network.addLink(nf.createLink(Id.create("21", Link.class), n2, n1));
		network.addLink(nf.createLink(Id.create("23", Link.class), n2, n3));
		network.addLink(nf.createLink(Id.create("32", Link.class), n3, n2));
		network.addLink(nf.createLink(Id.create("33", Link.class), n3, n3));
		
		network.addLink(nf.createLink(Id.create("1112", Link.class), n11, n12));
		network.addLink(nf.createLink(Id.create("1211", Link.class), n12, n11));
		network.addLink(nf.createLink(Id.create("1213", Link.class), n12, n13));
		network.addLink(nf.createLink(Id.create("1312", Link.class), n13, n12));
		network.addLink(nf.createLink(Id.create("1314", Link.class), n13, n14));
		network.addLink(nf.createLink(Id.create("1413", Link.class), n14, n13));
		
		network.addLink(nf.createLink(Id.create("1121", Link.class), n11, n21));
		network.addLink(nf.createLink(Id.create("2111", Link.class), n21, n11));
		network.addLink(nf.createLink(Id.create("1222", Link.class), n12, n22));
		network.addLink(nf.createLink(Id.create("2212", Link.class), n22, n12));
		network.addLink(nf.createLink(Id.create("1323", Link.class), n13, n23));
		network.addLink(nf.createLink(Id.create("2313", Link.class), n23, n13));
		
		network.addLink(nf.createLink(Id.create("1424", Link.class), n14, n24));
		network.addLink(nf.createLink(Id.create("2414", Link.class), n24, n14));
		network.addLink(nf.createLink(Id.create("2122", Link.class), n21, n22));
		network.addLink(nf.createLink(Id.create("2221", Link.class), n22, n21));
		network.addLink(nf.createLink(Id.create("2223", Link.class), n22, n23));
		network.addLink(nf.createLink(Id.create("2322", Link.class), n23, n22));
		
		network.addLink(nf.createLink(Id.create("2324", Link.class), n23, n24));
		network.addLink(nf.createLink(Id.create("2423", Link.class), n24, n23));
		network.addLink(nf.createLink(Id.create("2131", Link.class), n21, n31));
		network.addLink(nf.createLink(Id.create("3121", Link.class), n31, n21));
		network.addLink(nf.createLink(Id.create("2232", Link.class), n22, n32));
		network.addLink(nf.createLink(Id.create("3222", Link.class), n32, n22));
		
		network.addLink(nf.createLink(Id.create("2333", Link.class), n23, n33));
		network.addLink(nf.createLink(Id.create("3323", Link.class), n33, n23));
		network.addLink(nf.createLink(Id.create("2434", Link.class), n24, n34));
		network.addLink(nf.createLink(Id.create("3424", Link.class), n34, n24));
		network.addLink(nf.createLink(Id.create("3132", Link.class), n31, n32));
		network.addLink(nf.createLink(Id.create("3231", Link.class), n32, n31));
		
		network.addLink(nf.createLink(Id.create("3233", Link.class), n32, n33));
		network.addLink(nf.createLink(Id.create("3332", Link.class), n33, n32));
		network.addLink(nf.createLink(Id.create("3334", Link.class), n33, n34));
		network.addLink(nf.createLink(Id.create("3433", Link.class), n34, n33));
		network.addLink(nf.createLink(Id.create("3141", Link.class), n31, n41));
		network.addLink(nf.createLink(Id.create("4131", Link.class), n41, n31));
		
		network.addLink(nf.createLink(Id.create("3242", Link.class), n32, n42));
		network.addLink(nf.createLink(Id.create("4232", Link.class), n42, n32));
		network.addLink(nf.createLink(Id.create("3343", Link.class), n33, n43));
		network.addLink(nf.createLink(Id.create("4333", Link.class), n43, n33));
		network.addLink(nf.createLink(Id.create("3444", Link.class), n34, n44));
		network.addLink(nf.createLink(Id.create("4434", Link.class), n44, n34));
		
		network.addLink(nf.createLink(Id.create("4142", Link.class), n41, n42));
		network.addLink(nf.createLink(Id.create("4241", Link.class), n42, n41));
		network.addLink(nf.createLink(Id.create("4243", Link.class), n42, n43));
		network.addLink(nf.createLink(Id.create("4342", Link.class), n43, n42));
		network.addLink(nf.createLink(Id.create("4344", Link.class), n43, n44));
		network.addLink(nf.createLink(Id.create("4443", Link.class), n44, n43));
		
		
		Set<String> modes = new TreeSet<>();
		modes.add(TransportMode.car);
		Random rnd = new Random(4713) ;
		for (Link link : network.getLinks().values()) {
			link.setLength(1200.0 + rnd.nextDouble() );
			// (adding small random noise to the link length so that there is no arbitrariness any more for routers.  kai, oct'13)
			
			link.setCapacity(2000.0);
			link.setFreespeed(7.0);
			link.setAllowedModes(modes);
			link.setNumberOfLanes(1.0);
		}
		
		Set<Id<Link>> ids = new TreeSet<>();
		ids.add(Id.create("A", Link.class)); ids.add(Id.create("B", Link.class)); ids.add(Id.create("C", Link.class)); ids.add(Id.create("D", Link.class));
		
		for (Id<Link> id : ids) {
			Link link = network.getLinks().get(id);
			link.setLength(100.0);
			link.setCapacity(9999.0);
			link.setFreespeed(100.0);
		}
		
		modes = new TreeSet<>();
		modes.add("train");
		
		ids = new TreeSet<>();
		ids.add(Id.create("11", Link.class)); ids.add(Id.create("12", Link.class)); ids.add(Id.create("21", Link.class));
		ids.add(Id.create("23", Link.class)); ids.add(Id.create("32", Link.class)); ids.add(Id.create("33", Link.class));
		
		for (Id<Link> id : ids) {
			Link link = network.getLinks().get(id);
			link.setLength(100.0);
			link.setFreespeed(12.0);
			link.setAllowedModes(modes);
		}
		
		network.getLinks().get(Id.create("11", Link.class)).setFreespeed(100.0);
		network.getLinks().get(Id.create("33", Link.class)).setFreespeed(100.0);
		
		network.getLinks().get(Id.create("12", Link.class)).setLength(2200.0);
		network.getLinks().get(Id.create("21", Link.class)).setLength(2200.0);
		
		network.getLinks().get(Id.create("23", Link.class)).setLength(2700.0);
		network.getLinks().get(Id.create("32", Link.class)).setLength(2700.0);
		
		// network done
		
		// create transit schedule
		
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility s1 = sf.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 1000, (double) 1000), false);
		TransitStopFacility s2a = sf.createTransitStopFacility(Id.create("2a", TransitStopFacility.class), new Coord((double) 2050, (double) 2950), false);
		TransitStopFacility s2b = sf.createTransitStopFacility(Id.create("2b", TransitStopFacility.class), new Coord((double) 2050, (double) 2950), false);
		TransitStopFacility s3 = sf.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 3950, (double) 1050), false);
				
		s1.setLinkId(Id.create("11", Link.class));
		s2a.setLinkId(Id.create("12", Link.class));
		s2b.setLinkId(Id.create("32", Link.class));
		s3.setLinkId(Id.create("33", Link.class));
		
		schedule.addStopFacility(s1);
		schedule.addStopFacility(s2a);
		schedule.addStopFacility(s2b);
		schedule.addStopFacility(s3);

		List<TransitRouteStop> stops = new ArrayList<>();
		stops.add(sf.createTransitRouteStop(s1, 0, 0));
		stops.add(sf.createTransitRouteStop(s2a, 185, 200));
		stops.add(sf.createTransitRouteStop(s3, 425, 440));
		stops.add(sf.createTransitRouteStop(s2b, 665, 680));
		stops.add(sf.createTransitRouteStop(s1, 865, 865));

		
		line = sf.createTransitLine(Id.create("Blue Line", TransitLine.class));
		NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(Id.create("11", Link.class), Id.create("11", Link.class));
		List<Id<Link>> linkIds = new ArrayList<>();
		Collections.addAll(linkIds, Id.create("12", Link.class), Id.create("23", Link.class), Id.create("33", Link.class), Id.create("32", Link.class), Id.create("21", Link.class));
		netRoute.setLinkIds(Id.create("11", Link.class), linkIds,  Id.create("11", Link.class));
		route = sf.createTransitRoute( Id.create("1to3to1", TransitRoute.class), netRoute, stops, "train");

		line.addRoute(route);
		schedule.addTransitLine(line);
		
		// create departures - 5 min headways
		double startTime = 5.0 * 3600.0;
		Departure dep;
		
		for (int i = 1; i < 50; i += 2) {
			dep = sf.createDeparture(Id.create(i, Departure.class), startTime);
			dep.setVehicleId(Id.create("tr_1", Vehicle.class));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;
			
			dep = sf.createDeparture(Id.create(i+1, Departure.class), startTime);
			dep.setVehicleId(Id.create("tr_2", Vehicle.class));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;			
		}
		
		startTime = 5.0 * 3600.0 + 5.0 * 60.0;
		for (int i = 101; i < 150; i += 2) {
			dep = sf.createDeparture(Id.create(i, Departure.class), startTime);
			dep.setVehicleId(Id.create("tr_3", Vehicle.class));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;
			
			dep = sf.createDeparture(Id.create(i+1, Departure.class), startTime);
			dep.setVehicleId(Id.create("tr_4", Vehicle.class));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;			
		}
		return scenario;
	}
	
	public static Operator createCoop2111to2333(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getDriverRestTime(), conf.getMode());
	
		Operator coop = new BasicOperator(Id.create(conf.getPIdentifier() + 1, Operator.class), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2111to2333(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Operator createCoop2333to2111(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getDriverRestTime(), conf.getMode());
	
		Operator coop = new BasicOperator(Id.create(conf.getPIdentifier() + 1, Operator.class), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2333to2111(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Operator createCoop2414to3444(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getDriverRestTime(), conf.getMode());
		Operator coop = new BasicOperator(Id.create(conf.getPIdentifier() + 1, Operator.class), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2414to3444(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Operator createCoop2111to1314to4443(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getDriverRestTime(), conf.getMode());
		Operator coop = new BasicOperator(Id.create(conf.getPIdentifier() + 1, Operator.class), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2111to1314to4443(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Operator createCoopRouteVShaped(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getDriverRestTime(), conf.getMode());
	
		Operator coop = new BasicOperator(Id.create(conf.getPIdentifier() + 1, Operator.class), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new RouteVShaped(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}

}

class Route2111to2333 implements PStrategy{
	
	private final String pId;
	private final TransitSchedule sched;

	public Route2111to2333(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Operator cooperative) {
		Id<PPlan> routeId = Id.create(cooperative.getCurrentIteration(), PPlan.class);
		
		PPlan newPlan = new PPlan(routeId, this.getStrategyName(), PConstants.founderPlanId);
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(Id.create(pId + "2111", TransitStopFacility.class));
		TransitStopFacility endStop = sched.getFacilities().get(Id.create(pId + "2333", TransitStopFacility.class));
		ArrayList<TransitStopFacility> stops = new ArrayList<>();
		stops.add(startStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLineFromOperatorPlan(cooperative.getId(), newPlan));
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return "Route2111to2333";
	}
}

class Route2333to2111 implements PStrategy{
	
	private final String pId;
	private final TransitSchedule sched;

	public Route2333to2111(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Operator cooperative) {
		Id<PPlan> routeId = Id.create(cooperative.getCurrentIteration(), PPlan.class);
		
		PPlan newPlan = new PPlan(routeId, this.getStrategyName(), PConstants.founderPlanId);
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(Id.create(pId + "2333", TransitStopFacility.class));
		TransitStopFacility endStop = sched.getFacilities().get(Id.create(pId + "2111", TransitStopFacility.class));
		ArrayList<TransitStopFacility> stops = new ArrayList<>();
		stops.add(startStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLineFromOperatorPlan(cooperative.getId(), newPlan));
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return "Route2333to2111";
	}
}

class Route2414to3444 implements PStrategy{
	
	private final String pId;
	private final TransitSchedule sched;

	public Route2414to3444(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Operator cooperative) {
		Id<PPlan> routeId = Id.create(cooperative.getCurrentIteration(), PPlan.class);
		PPlan newPlan = new PPlan(routeId, this.getStrategyName(), PConstants.founderPlanId);
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(Id.create(pId + "2414", TransitStopFacility.class));
		TransitStopFacility endStop = sched.getFacilities().get(Id.create(pId + "3444", TransitStopFacility.class));
		ArrayList<TransitStopFacility> stops = new ArrayList<>();
		stops.add(startStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLineFromOperatorPlan(cooperative.getId(), newPlan));
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return "Route2414to3444";
	}
}


class Route2111to1314to4443 implements PStrategy{
	
	private final String pId;
	private final TransitSchedule sched;

	public Route2111to1314to4443(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Operator cooperative) {
		Id<PPlan> routeId = Id.create(cooperative.getCurrentIteration(), PPlan.class);
		PPlan newPlan = new PPlan(routeId, this.getStrategyName(), PConstants.founderPlanId);
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(Id.create(pId + "2111", TransitStopFacility.class));
		TransitStopFacility middleStop = sched.getFacilities().get(Id.create(pId + "1314", TransitStopFacility.class));
		TransitStopFacility endStop = sched.getFacilities().get(Id.create(pId + "4443", TransitStopFacility.class));
		ArrayList<TransitStopFacility> stops = new ArrayList<>();
		stops.add(startStop);
		stops.add(middleStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLineFromOperatorPlan(cooperative.getId(), newPlan));
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return "Route2111to1314to4443";
	}
}

class RouteVShaped implements PStrategy{
	
	private final String pId;
	private final TransitSchedule sched;
	public RouteVShaped(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}
	@Override
	public PPlan run(Operator cooperative) {
		Id<PPlan> routeId = Id.create(cooperative.getCurrentIteration(), PPlan.class);
		
		PPlan newPlan = new PPlan(routeId, this.getStrategyName(), PConstants.founderPlanId);
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(Id.create(pId + "2111", TransitStopFacility.class));
		TransitStopFacility mostDistantStop = sched.getFacilities().get(Id.create(pId + "3141", TransitStopFacility.class));
		TransitStopFacility endStop = sched.getFacilities().get(Id.create(pId + "3222", TransitStopFacility.class));
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		stopsToBeServed.add(startStop);
		stopsToBeServed.add(mostDistantStop);
		stopsToBeServed.add(endStop);
		stopsToBeServed.add(mostDistantStop);
		newPlan.setStopsToBeServed(stopsToBeServed);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLineFromOperatorPlan(cooperative.getId(), newPlan));
		return newPlan;
	}
	@Override
	public String getStrategyName() {
		return "RouteVShaped2111to3141to3222";
	}
}