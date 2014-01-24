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

package playground.droeder.southAfrica.scenarios.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;

/**
 * @author droeder
 *
 */
class CreateTestScenario {
	
	
	private static final String DIR = "E:/VSP/svn/droeder/southAfrica/test/input/";
	
	public static void main(String[] args) {
		new CreateTestScenario().run();
	}

	private int numAgents = 2000;
	
	private CreateTestScenario(){
		
	}
	
	private void run(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		createNetwork(sc);
		new NetworkWriter(sc.getNetwork()).write(DIR + "network.xml");
		
		createScheduleAndVehicles(sc);
		new TransitScheduleWriter(sc.getTransitSchedule()).writeFile(DIR + "schedule.xml");
		new VehicleWriterV1(((ScenarioImpl) sc).getVehicles()).writeFile(DIR + "vehicles.xml");
		
		createPopulation(sc);
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(DIR + "plans.xml");
	}

	// ################## create network
	
	/*	    B1
	 * 	   /  \
	 *    /	   \
	 *   A--B2--C
	 *    \    /
	 *      B3
	 */		
	private void createNetwork(Scenario sc) {
		createNodes(sc);
		createLinks(sc);
	}
	
	private void createNodes(Scenario sc){
		Network net = sc.getNetwork();
		NetworkFactory factory = net.getFactory();
		
		Node n;
		n = factory.createNode(sc.createId("A"), sc.createCoord(1000, 2000));
		net.addNode(n);
		
		n = factory.createNode(sc.createId("C"), sc.createCoord(11000, 2000));
		net.addNode(n);
		
		n = factory.createNode(sc.createId("B1"), sc.createCoord(6000, 4000));
		net.addNode(n);
		
		n = factory.createNode(sc.createId("B2"), sc.createCoord(6000, 2000));
		net.addNode(n);
		
		n = factory.createNode(sc.createId("B3"), sc.createCoord(6000, 1000));
		net.addNode(n);
	}
	
	private void createLinks(Scenario sc){
		Network net = sc.getNetwork();
		NetworkFactory factory = net.getFactory();
		
		@SuppressWarnings("serial")
		Set<String> train = new HashSet<String>(){{
			add("train");
		}};
		
		@SuppressWarnings("serial")
		Set<String> busCarTaxi= new HashSet<String>(){{
			add("bus");
			add("car");
			add("taxi");
		}};
		
		
		Link l = createLink(net, factory, "A-B1", busCarTaxi);
		net.addLink(l);
		net.addLink(createReverse(l, factory));
		
		l = createLink(net, factory, "B1-C", busCarTaxi);
		net.addLink(l);
		net.addLink(createReverse(l, factory));
		
		l = createLink(net, factory, "A-B3", busCarTaxi);
		net.addLink(l);
		net.addLink(createReverse(l, factory));
		
		l = createLink(net, factory, "B3-C", busCarTaxi);
		net.addLink(l);
		net.addLink(createReverse(l, factory));
		
		l = createLink(net, factory, "A-B2", train);
		net.addLink(l);
		net.addLink(createReverse(l, factory));
		
		l = createLink(net, factory, "B2-C", train);
		net.addLink(l);
		net.addLink(createReverse(l, factory));
		
		// create LoopLinks for Home/Work-Locations
		l = createLink(net, factory, "A-A", busCarTaxi);
		l.setCapacity(9999);
		l.setFreespeed(10000);
		l.setLength(10000);
		net.addLink(l);
		
		l = createLink(net, factory, "C-C", busCarTaxi);
		l.setCapacity(9999);
		l.setFreespeed(10000);
		l.setLength(10000);
		net.addLink(l);
		
	}

	private Link createLink(Network net, NetworkFactory factory, String id,	Set<String> mode) {
		Node from,to;
		from = net.getNodes().get(new IdImpl(id.split("-")[0]));
		to =  net.getNodes().get(new IdImpl(id.split("-")[1]));
		
		Link l = factory.createLink(new IdImpl(id), from, to);
		l.setAllowedModes(mode);
		l.setCapacity(4000);
		if(mode.contains("train")){
			l.setFreespeed(100/3.6);
		}else{
			l.setFreespeed(50/3.6);
		}
		l.setLength(CoordUtils.calcDistance(from.getCoord(), to.getCoord()));
		return l;
	}

	private Link createReverse(Link l, NetworkFactory factory) {
		Link ll = factory.createLink(
				new IdImpl(l.getToNode().getId().toString() + "-" + l.getFromNode().getId().toString()), 
				l.getToNode(), 
				l.getFromNode());
		ll.setAllowedModes(l.getAllowedModes());
		ll.setCapacity(l.getCapacity());
		ll.setFreespeed(l.getFreespeed());
		ll.setLength(l.getLength());
		
		return ll;
	}

	//############ createTransitScheduleAndVehicles
	private void createScheduleAndVehicles(Scenario sc){
		
		sc.getConfig().scenario().setUseVehicles(true);
		sc.getConfig().scenario().setUseTransit(true);
		
		createStops(sc);
		createBus(sc);
		createTrain(sc);
	}

	private void createStops(Scenario sc) {
		TransitSchedule sched = sc.getTransitSchedule();
		
		for(Link l: sc.getNetwork().getLinks().values()){
			sched.addStopFacility(createFacility(sc, l.getId().toString()));
		}
	}
	
	private TransitStopFacility createFacility(Scenario sc, String linkId){
		TransitScheduleFactory f = sc.getTransitSchedule().getFactory();
		Link l = sc.getNetwork().getLinks().get(sc.createId(linkId));
		
		TransitStopFacility fac = f.createTransitStopFacility(l.getId(), l.getToNode().getCoord(), false);
		fac.setLinkId(l.getId());
	
		return fac;
	}

	private void createBus(Scenario sc){
		TransitScheduleFactory f = sc.getTransitSchedule().getFactory();
		String mode = "bus";
		//  create vehicleType
		VehicleType vType = ((ScenarioImpl) sc).getVehicles().getFactory().createVehicleType(sc.createId(mode));
		((ScenarioImpl) sc).getVehicles().getVehicleTypes().put(vType.getId(), vType);
		vType.setLength(15);
		VehicleCapacity cap = new VehicleCapacityImpl();
		cap.setSeats(51);
		cap.setStandingRoom(0);
		vType.setCapacity(cap);
		//create Line and Route
		TransitLine l = f.createTransitLine(sc.createId(mode));
		NetworkRoute route = new LinkNetworkRouteImpl(sc.createId("A-A"), sc.createId("A-A"));
		
		@SuppressWarnings("serial")
		List<Id> linkIds = new ArrayList<Id>(){{
			add(new IdImpl("A-B1"));
			add(new IdImpl("B1-C"));
			add(new IdImpl("C-C"));
			add(new IdImpl("C-B1"));
			add(new IdImpl("B1-A"));
		}};
		route.setLinkIds(route.getStartLinkId(), linkIds, route.getEndLinkId());
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		double delay = 0;
		Link link;
		TransitRouteStop stop;
		
		//add first stop
		link = sc.getNetwork().getLinks().get(route.getStartLinkId());
		stop = f.createTransitRouteStop(sc.getTransitSchedule().getFacilities().get(link.getId()), delay, delay+=60);
		stop.setAwaitDepartureTime(true);
		stops.add(stop);
		
		//add other stops
		for(Id linkId: route.getLinkIds()){
			link = sc.getNetwork().getLinks().get(linkId);
			stop = f.createTransitRouteStop(sc.getTransitSchedule().getFacilities().get(link.getId()),	delay+=(link.getLength()/link.getFreespeed()*1.2), delay+=60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
		}
		//add last stop
		link = sc.getNetwork().getLinks().get(route.getEndLinkId());
		stop = f.createTransitRouteStop(sc.getTransitSchedule().getFacilities().get(link.getId()), delay+=(link.getLength()/link.getFreespeed()*1.2), delay+=60);
		stop.setAwaitDepartureTime(true);
		stops.add(stop);
		
		TransitRoute r = f.createTransitRoute(new IdImpl(mode + "1"), route, stops, mode);
		
		Departure d;
		int depCnt = 0;
		int vehCnt = 0;
		double umlaufDuration = stops.get(stops.size() - 1).getArrivalOffset() * 1.2;
		
		LinkedList<Tuple<Vehicle, Double>> vehicles = new LinkedList<Tuple<Vehicle,Double>>();
		
		//create umlaeufe
		Vehicle v;
		for(int i = (6*3600); i < (18.5 * 3600 + 1); i = i + (3600/6)){
			if(vehicles.isEmpty()){
				//currently we have no vehicle. create a new on, add to vehicles-container
				v = new VehicleImpl(new IdImpl(mode + vehCnt++), vType);
				((ScenarioImpl) sc).getVehicles().addVehicle( v);
			}else{
				//check, if the first vehicle of the queue should have finished its route. Poll it, if so
				if(vehicles.peekFirst().getSecond() <= i){
					v = vehicles.pollFirst().getFirst();
				}
				// otherwise create a new one and add to conatiner
				else{
					
					v = new VehicleImpl(new IdImpl(mode + vehCnt++), vType);
					((ScenarioImpl) sc).getVehicles().addVehicle( v);
				}
			}
			
			d = f.createDeparture(new IdImpl(depCnt++), i);
			d.setVehicleId(v.getId());
			r.addDeparture(d);
			// add the vehicle to the queue
			vehicles.add(new Tuple<Vehicle, Double>(v, i + umlaufDuration));
		}
		l.addRoute(r);
		sc.getTransitSchedule().addTransitLine(l);
	}
	
	private void createTrain(Scenario sc){
		TransitScheduleFactory f = sc.getTransitSchedule().getFactory();
		String mode = "train";
		//  create vehicleType
		VehicleType vType = ((ScenarioImpl) sc).getVehicles().getFactory().createVehicleType(sc.createId(mode));
		((ScenarioImpl) sc).getVehicles().getVehicleTypes().put(vType.getId(), vType);
		vType.setLength(45);
		VehicleCapacity cap = new VehicleCapacityImpl();
		cap.setSeats(150);
		cap.setStandingRoom(0);
		vType.setCapacity(cap);
		//create Line and Route
		TransitLine l = f.createTransitLine(sc.createId(mode));
		NetworkRoute route = new LinkNetworkRouteImpl(sc.createId("B2-A"), sc.createId("B2-A"));
		
		@SuppressWarnings("serial")
		List<Id> linkIds = new ArrayList<Id>(){{
			add(new IdImpl("A-B2"));
			add(new IdImpl("B2-C"));
			add(new IdImpl("C-B2"));
		}};
		route.setLinkIds(route.getStartLinkId(), linkIds, route.getEndLinkId());
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		double delay = 0;
		Link link;
		TransitRouteStop stop;
		
		//add first stop
		link = sc.getNetwork().getLinks().get(route.getStartLinkId());
		stop = f.createTransitRouteStop(sc.getTransitSchedule().getFacilities().get(link.getId()), delay, delay+=60);
		stop.setAwaitDepartureTime(true);
		stops.add(stop);
		
		//add other stops
		for(Id linkId: route.getLinkIds()){
			link = sc.getNetwork().getLinks().get(linkId);
			stop = f.createTransitRouteStop(sc.getTransitSchedule().getFacilities().get(link.getId()),	delay+=(link.getLength()/link.getFreespeed()*1.2), delay+=60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
		}
		//add last stop
		link = sc.getNetwork().getLinks().get(route.getEndLinkId());
		stop = f.createTransitRouteStop(sc.getTransitSchedule().getFacilities().get(link.getId()), delay+=(link.getLength()/link.getFreespeed()*1.2), delay+=60);
		stop.setAwaitDepartureTime(true);
		stops.add(stop);
		
		TransitRoute r = f.createTransitRoute(new IdImpl(mode + "1"), route, stops, mode);
		
		Departure d;
		int depCnt = 0;
		int vehCnt = 0;
		double umlaufDuration = stops.get(stops.size() - 1).getArrivalOffset() * 1.2;
		
		LinkedList<Tuple<Vehicle, Double>> vehicles = new LinkedList<Tuple<Vehicle,Double>>();
		
		//create umlaeufe
		Vehicle v;
		for(int i = (6*3600); i < (18.5 * 3600 + 1); i = i + (3600/6)){
			if(vehicles.isEmpty()){
				//currently we have no vehicle. create a new on, add to vehicles-container
				v = new VehicleImpl(new IdImpl(mode + vehCnt++), vType);
				((ScenarioImpl) sc).getVehicles().addVehicle( v);
			}else{
				//check, if the first vehicle of the queue should have finished its route. Poll it, if so
				if(vehicles.peekFirst().getSecond() <= i){
					v = vehicles.pollFirst().getFirst();
				}
				// otherwise create a new one and add to conatiner
				else{
					
					v = new VehicleImpl(new IdImpl(mode + vehCnt++), vType);
					((ScenarioImpl) sc).getVehicles().addVehicle( v);
				}
			}
			
			d = f.createDeparture(new IdImpl(depCnt++), i);
			d.setVehicleId(v.getId());
			r.addDeparture(d);
			// add the vehicle to the queue
			vehicles.add(new Tuple<Vehicle, Double>(v, i + umlaufDuration));
		}
		l.addRoute(r);
		sc.getTransitSchedule().addTransitLine(l);
		
	}
	
	//############ createPopulation
	
	private void createPopulation(Scenario sc) {
		Population p = sc.getPopulation();
		PopulationFactory pFac = p.getFactory();
		
		String mode;
		Double rnd;
		Double end1 = 8*3600.;
		Double end2 = 16*3600.;
		Activity h1, w, h2;
		Leg l;
		for(int i = 0; i < this.numAgents ; i++){
			rnd = MatsimRandom.getRandom().nextDouble();
			if(rnd < 0.2){
				mode = "bus";
			}
			else if(rnd < 0.4){
				mode = "taxi";
			}
			else if(rnd < 0.6){
				mode = "car";
			} else{
				mode = "train";
			}
			l = pFac.createLeg(mode);
			
			//TODO problems if loopLinks are used... maybe because of QLinkImpl line 306ff
			h1 = pFac.createActivityFromLinkId("h", sc.createId("B3-A"));
			((ActivityImpl) h1).setCoord(sc.getNetwork().getLinks().get(h1.getLinkId()).getToNode().getCoord());
			h1.setEndTime(end1);
			w = pFac.createActivityFromLinkId("w", sc.createId("B3-C"));
			((ActivityImpl) w).setCoord(sc.getNetwork().getLinks().get(w.getLinkId()).getToNode().getCoord());
			w.setEndTime(end2);
			h2 = pFac.createActivityFromLinkId("h", sc.createId("B3-A"));
			((ActivityImpl) h2).setCoord(sc.getNetwork().getLinks().get(h2.getLinkId()).getToNode().getCoord());
			
			Plan plan = pFac.createPlan();
			plan.addActivity(h1);
			plan.addLeg(l);
			plan.addActivity(w);
			plan.addLeg(l);
			plan.addActivity(h2);
			Person person = pFac.createPerson(sc.createId(String.valueOf(i) + "_" + mode));
			person.addPlan(plan);
			p.addPerson(person);
			end1 += (2.*3600/numAgents);
			end2 += (2.*3600/numAgents);
		}
		
		
	}
}
