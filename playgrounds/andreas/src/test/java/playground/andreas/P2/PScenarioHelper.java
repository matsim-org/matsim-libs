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

package playground.andreas.P2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.BasicCooperative;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.pbox.PFranchise;
import playground.andreas.P2.replanning.CreateNewPlan;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.PStrategy;
import playground.andreas.P2.replanning.TimeProvider;
import playground.andreas.P2.routeProvider.ComplexCircleScheduleProvider;
import playground.andreas.P2.routeProvider.PRouteProvider;
import playground.andreas.P2.routeProvider.RandomStopProvider;
import playground.andreas.P2.schedule.CreatePStops;
import playground.andreas.P2.schedule.CreateStopsForAllCarLinks;

/**
 * Creates an car grid network with one pt line.
 *  
 * @author aneumann
 *
 */
public class PScenarioHelper {
	
	public static Cooperative createTestCooperative(String testOutPutDir){
		Scenario sC = PScenarioHelper.createTestNetwork();
		
		PConfigGroup pC = new PConfigGroup();
		Cooperative coop = new BasicCooperative(new IdImpl(pC.getPIdentifier() + 1), pC, new PFranchise(pC.getUseFranchise(), pC.getGridSize()));
		TransitSchedule sched = CreatePStops.createPStops(sC.getNetwork(), pC);
		RandomStopProvider randomStopProvider = new RandomStopProvider(pC, sC.getPopulation(), sched, null);
		
		PRouteProvider rP = new ComplexCircleScheduleProvider(sched, sC.getNetwork(), randomStopProvider, 0, pC.getPlanningSpeedFactor(), pC.getVehicleMaximumVelocity(), pC.getMode());
		
		ArrayList<String> parameter = new ArrayList<String>();
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
		
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		network = scenario.getNetwork();
		schedule = ((ScenarioImpl) scenario).getTransitSchedule();
		NetworkFactory nf = network.getFactory();

		// creates nodes
		Node n1 = nf.createNode(new IdImpl("1"), scenario.createCoord(1050, 1050));
		Node n2 = nf.createNode(new IdImpl("2"), scenario.createCoord(2050, 2950));
		Node n3 = nf.createNode(new IdImpl("3"), scenario.createCoord(3950, 1050));
		
		Node n11 = nf.createNode(new IdImpl("11"), scenario.createCoord(1000, 1000));
		Node n12 = nf.createNode(new IdImpl("12"), scenario.createCoord(1000, 2000));
		Node n13 = nf.createNode(new IdImpl("13"), scenario.createCoord(1000, 3000));
		Node n14 = nf.createNode(new IdImpl("14"), scenario.createCoord(1000, 4000));
		
		Node n21 = nf.createNode(new IdImpl("21"), scenario.createCoord(2000, 1000));
		Node n22 = nf.createNode(new IdImpl("22"), scenario.createCoord(2000, 2000));
		Node n23 = nf.createNode(new IdImpl("23"), scenario.createCoord(2000, 3000));
		Node n24 = nf.createNode(new IdImpl("24"), scenario.createCoord(2000, 4000));
		
		Node n31 = nf.createNode(new IdImpl("31"), scenario.createCoord(3000, 1000));
		Node n32 = nf.createNode(new IdImpl("32"), scenario.createCoord(3000, 2000));
		Node n33 = nf.createNode(new IdImpl("33"), scenario.createCoord(3000, 3000));
		Node n34 = nf.createNode(new IdImpl("34"), scenario.createCoord(3000, 4000));
		
		Node n41 = nf.createNode(new IdImpl("41"), scenario.createCoord(4000, 1000));
		Node n42 = nf.createNode(new IdImpl("42"), scenario.createCoord(4000, 2000));
		Node n43 = nf.createNode(new IdImpl("43"), scenario.createCoord(4000, 3000));
		Node n44 = nf.createNode(new IdImpl("44"), scenario.createCoord(4000, 4000));
		
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
		
		network.addLink(nf.createLink(new IdImpl("A"), n14, n14));
		network.addLink(nf.createLink(new IdImpl("B"), n44, n44));
		network.addLink(nf.createLink(new IdImpl("C"), n11, n11));
		network.addLink(nf.createLink(new IdImpl("D"), n41, n41));
		
		network.addLink(nf.createLink(new IdImpl("11"), n1, n1));
		network.addLink(nf.createLink(new IdImpl("12"), n1, n2));
		network.addLink(nf.createLink(new IdImpl("21"), n2, n1));
		network.addLink(nf.createLink(new IdImpl("23"), n2, n3));
		network.addLink(nf.createLink(new IdImpl("32"), n3, n2));
		network.addLink(nf.createLink(new IdImpl("33"), n3, n3));
		
		network.addLink(nf.createLink(new IdImpl("1112"), n11, n12));
		network.addLink(nf.createLink(new IdImpl("1211"), n12, n11));
		network.addLink(nf.createLink(new IdImpl("1213"), n12, n13));
		network.addLink(nf.createLink(new IdImpl("1312"), n13, n12));
		network.addLink(nf.createLink(new IdImpl("1314"), n13, n14));
		network.addLink(nf.createLink(new IdImpl("1413"), n14, n13));
		
		network.addLink(nf.createLink(new IdImpl("1121"), n11, n21));
		network.addLink(nf.createLink(new IdImpl("2111"), n21, n11));
		network.addLink(nf.createLink(new IdImpl("1222"), n12, n22));
		network.addLink(nf.createLink(new IdImpl("2212"), n22, n12));
		network.addLink(nf.createLink(new IdImpl("1323"), n13, n23));
		network.addLink(nf.createLink(new IdImpl("2313"), n23, n13));
		
		network.addLink(nf.createLink(new IdImpl("1424"), n14, n24));
		network.addLink(nf.createLink(new IdImpl("2414"), n24, n14));
		network.addLink(nf.createLink(new IdImpl("2122"), n21, n22));
		network.addLink(nf.createLink(new IdImpl("2221"), n22, n21));
		network.addLink(nf.createLink(new IdImpl("2223"), n22, n23));
		network.addLink(nf.createLink(new IdImpl("2322"), n23, n22));
		
		network.addLink(nf.createLink(new IdImpl("2324"), n23, n24));
		network.addLink(nf.createLink(new IdImpl("2423"), n24, n23));
		network.addLink(nf.createLink(new IdImpl("2131"), n21, n31));
		network.addLink(nf.createLink(new IdImpl("3121"), n31, n21));
		network.addLink(nf.createLink(new IdImpl("2232"), n22, n32));
		network.addLink(nf.createLink(new IdImpl("3222"), n32, n22));
		
		network.addLink(nf.createLink(new IdImpl("2333"), n23, n33));
		network.addLink(nf.createLink(new IdImpl("3323"), n33, n23));
		network.addLink(nf.createLink(new IdImpl("2434"), n24, n34));
		network.addLink(nf.createLink(new IdImpl("3424"), n34, n24));
		network.addLink(nf.createLink(new IdImpl("3132"), n31, n32));
		network.addLink(nf.createLink(new IdImpl("3231"), n32, n31));
		
		network.addLink(nf.createLink(new IdImpl("3233"), n32, n33));
		network.addLink(nf.createLink(new IdImpl("3332"), n33, n32));
		network.addLink(nf.createLink(new IdImpl("3334"), n33, n34));
		network.addLink(nf.createLink(new IdImpl("3433"), n34, n33));
		network.addLink(nf.createLink(new IdImpl("3141"), n31, n41));
		network.addLink(nf.createLink(new IdImpl("4131"), n41, n31));
		
		network.addLink(nf.createLink(new IdImpl("3242"), n32, n42));
		network.addLink(nf.createLink(new IdImpl("4232"), n42, n32));
		network.addLink(nf.createLink(new IdImpl("3343"), n33, n43));
		network.addLink(nf.createLink(new IdImpl("4333"), n43, n33));
		network.addLink(nf.createLink(new IdImpl("3444"), n34, n44));
		network.addLink(nf.createLink(new IdImpl("4434"), n44, n34));
		
		network.addLink(nf.createLink(new IdImpl("4142"), n41, n42));
		network.addLink(nf.createLink(new IdImpl("4241"), n42, n41));
		network.addLink(nf.createLink(new IdImpl("4243"), n42, n43));
		network.addLink(nf.createLink(new IdImpl("4342"), n43, n42));
		network.addLink(nf.createLink(new IdImpl("4344"), n43, n44));
		network.addLink(nf.createLink(new IdImpl("4443"), n44, n43));
		
		
		Set<String> modes = new TreeSet<String>();
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
		
		Set<Id> ids = new TreeSet<Id>(); 
		ids.add(new IdImpl("A")); ids.add(new IdImpl("B")); ids.add(new IdImpl("C")); ids.add(new IdImpl("D"));
		
		for (Id id : ids) {
			Link link = network.getLinks().get(id);
			link.setLength(100.0);
			link.setCapacity(9999.0);
			link.setFreespeed(100.0);
		}
		
		modes = new TreeSet<String>();
		modes.add("train");
		
		ids = new TreeSet<Id>();
		ids.add(new IdImpl("11")); ids.add(new IdImpl("12")); ids.add(new IdImpl("21"));
		ids.add(new IdImpl("23")); ids.add(new IdImpl("32")); ids.add(new IdImpl("33"));
		
		for (Id id : ids) {
			Link link = network.getLinks().get(id);
			link.setLength(100.0);
			link.setFreespeed(12.0);
			link.setAllowedModes(modes);
		}
		
		network.getLinks().get(new IdImpl("11")).setFreespeed(100.0);
		network.getLinks().get(new IdImpl("33")).setFreespeed(100.0);
		
		network.getLinks().get(new IdImpl("12")).setLength(2200.0);
		network.getLinks().get(new IdImpl("21")).setLength(2200.0);
		
		network.getLinks().get(new IdImpl("23")).setLength(2700.0);
		network.getLinks().get(new IdImpl("32")).setLength(2700.0);
		
		// network done
		
		// create transit schedule
		
		TransitScheduleFactory sf = schedule.getFactory();

		TransitStopFacility s1 = sf.createTransitStopFacility(new IdImpl("1"), scenario.createCoord(1000, 1000), false);
		TransitStopFacility s2a = sf.createTransitStopFacility(new IdImpl("2a"), scenario.createCoord(2050, 2950), false);
		TransitStopFacility s2b = sf.createTransitStopFacility(new IdImpl("2b"), scenario.createCoord(2050, 2950), false);
		TransitStopFacility s3 = sf.createTransitStopFacility(new IdImpl("3"), scenario.createCoord(3950, 1050), false);
				
		s1.setLinkId(new IdImpl("11"));
		s2a.setLinkId(new IdImpl("12"));
		s2b.setLinkId(new IdImpl("32"));
		s3.setLinkId(new IdImpl("33"));
		
		schedule.addStopFacility(s1);
		schedule.addStopFacility(s2a);
		schedule.addStopFacility(s2b);
		schedule.addStopFacility(s3);

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		stops.add(sf.createTransitRouteStop(s1, 0, 0));
		stops.add(sf.createTransitRouteStop(s2a, 185, 200));
		stops.add(sf.createTransitRouteStop(s3, 425, 440));
		stops.add(sf.createTransitRouteStop(s2b, 665, 680));
		stops.add(sf.createTransitRouteStop(s1, 865, 865));

		
		line = sf.createTransitLine(new IdImpl("Blue Line"));
		NetworkRoute netRoute = new LinkNetworkRouteImpl(new IdImpl("11"), new IdImpl("11"));
		List<Id> linkIds = new ArrayList<Id>();
		Collections.addAll(linkIds, new IdImpl("12"), new IdImpl("23"), new IdImpl("33"), new IdImpl("32"), new IdImpl("21"));
		netRoute.setLinkIds(new IdImpl("11"), linkIds,  new IdImpl("11"));
		route = sf.createTransitRoute( new IdImpl("1to3to1"), netRoute, stops, "train");

		line.addRoute(route);
		schedule.addTransitLine(line);
		
		// create departures - 5 min headways
		double startTime = 5.0 * 3600.0;
		Departure dep;
		
		for (int i = 1; i < 50; i += 2) {
			dep = sf.createDeparture(new IdImpl(i), startTime);
			dep.setVehicleId(new IdImpl("tr_1"));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;
			
			dep = sf.createDeparture(new IdImpl(i+1), startTime);
			dep.setVehicleId(new IdImpl("tr_2"));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;			
		}
		
		startTime = 5.0 * 3600.0 + 5.0 * 60.0;
		for (int i = 101; i < 150; i += 2) {
			dep = sf.createDeparture(new IdImpl(i), startTime);
			dep.setVehicleId(new IdImpl("tr_3"));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;
			
			dep = sf.createDeparture(new IdImpl(i+1), startTime);
			dep.setVehicleId(new IdImpl("tr_4"));
			route.addDeparture(dep);
			startTime += 10.0 * 60.0;			
		}
		return scenario;
	}
	
	public static Cooperative createCoop2111to2333(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, 10, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getMode());
	
		Cooperative coop = new BasicCooperative(new IdImpl(conf.getPIdentifier() + 1), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2111to2333(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Cooperative createCoop2333to2111(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, 10, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getMode());
	
		Cooperative coop = new BasicCooperative(new IdImpl(conf.getPIdentifier() + 1), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2333to2111(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Cooperative createCoop2414to3444(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, 10, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getMode());
		Cooperative coop = new BasicCooperative(new IdImpl(conf.getPIdentifier() + 1), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2414to3444(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}
	
	public static Cooperative createCoop2111to1314to4443(){
		
		Scenario sc= PScenarioHelper.createTestNetwork();
		
		PConfigGroup conf = new PConfigGroup();
		TransitSchedule sched = CreateStopsForAllCarLinks.createStopsForAllCarLinks(sc.getNetwork(), conf);
		RandomStopProvider randomStopProvider = new RandomStopProvider(conf, sc.getPopulation(), sched, null);
		ComplexCircleScheduleProvider prov = new ComplexCircleScheduleProvider(sched, sc.getNetwork(), randomStopProvider, 10, conf.getVehicleMaximumVelocity(), conf.getPlanningSpeedFactor(), conf.getMode());
		Cooperative coop = new BasicCooperative(new IdImpl(conf.getPIdentifier() + 1), conf, new PFranchise(conf.getUseFranchise(), conf.getGridSize()));
		coop.init(prov, new Route2111to1314to4443(sched, conf.getPIdentifier()), 0, 0.0);
		
		return coop;
	}

}

class Route2111to2333 implements PStrategy{
	
	private String pId;
	private TransitSchedule sched;

	public Route2111to2333(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		Id routeId = new IdImpl(cooperative.getCurrentIteration());
		
		PPlan newPlan = new PPlan(routeId, this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(new IdImpl(pId + "2111"));
		TransitStopFacility endStop = sched.getFacilities().get(new IdImpl(pId + "2333"));
		ArrayList<TransitStopFacility> stops = new ArrayList<TransitStopFacility>();
		stops.add(startStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		return newPlan;
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PStrategy#getName()
	 */
	@Override
	public String getName() {
		return "Route2111to2333";
	}
}

class Route2333to2111 implements PStrategy{
	
	private String pId;
	private TransitSchedule sched;

	public Route2333to2111(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		Id routeId = new IdImpl(cooperative.getCurrentIteration());
		
		PPlan newPlan = new PPlan(routeId, this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(new IdImpl(pId + "2333"));
		TransitStopFacility endStop = sched.getFacilities().get(new IdImpl(pId + "2111"));
		ArrayList<TransitStopFacility> stops = new ArrayList<TransitStopFacility>();
		stops.add(startStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		return newPlan;
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PStrategy#getName()
	 */
	@Override
	public String getName() {
		return "Route2333to2111";
	}
}

class Route2414to3444 implements PStrategy{
	
	private String pId;
	private TransitSchedule sched;

	public Route2414to3444(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		Id routeId = new IdImpl(cooperative.getCurrentIteration());
		PPlan newPlan = new PPlan(routeId, this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(new IdImpl(pId + "2414"));
		TransitStopFacility endStop = sched.getFacilities().get(new IdImpl(pId + "3444"));
		ArrayList<TransitStopFacility> stops = new ArrayList<TransitStopFacility>();
		stops.add(startStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		return newPlan;
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PStrategy#getName()
	 */
	@Override
	public String getName() {
		return "Route2414to3444";
	}
}


class Route2111to1314to4443 implements PStrategy{
	
	private String pId;
	private TransitSchedule sched;

	public Route2111to1314to4443(TransitSchedule sched, String pId){
		this.sched = sched;
		this.pId = pId;
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		Id routeId = new IdImpl(cooperative.getCurrentIteration());
		PPlan newPlan = new PPlan(routeId, this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(8.0 * 3600.0);
		newPlan.setEndTime(16.0 * 3600.0);
		TransitStopFacility startStop = sched.getFacilities().get(new IdImpl(pId + "2111"));
		TransitStopFacility middleStop = sched.getFacilities().get(new IdImpl(pId + "1314"));
		TransitStopFacility endStop = sched.getFacilities().get(new IdImpl(pId + "4443"));
		ArrayList<TransitStopFacility> stops = new ArrayList<TransitStopFacility>();
		stops.add(startStop);
		stops.add(middleStop);
		stops.add(endStop);
		newPlan.setStopsToBeServed(stops);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		return newPlan;
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PStrategy#getName()
	 */
	@Override
	public String getName() {
		return "Route2111to1314to4443";
	}
}