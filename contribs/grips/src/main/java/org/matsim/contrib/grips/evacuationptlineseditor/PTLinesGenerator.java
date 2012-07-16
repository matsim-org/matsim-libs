/* *********************************************************************** *
 * project: org.matsim.*
 * PTLinesGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.grips.evacuationptlineseditor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.evacuationptlineseditor.EvacuationPTLinesEditor.BusStop;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class PTLinesGenerator {

	private final Scenario sc;
	private TransitSchedule schedule = null;
	private final Map<Id, BusStop> busStops;
	private final Dijkstra dijkstra;
	private final TransitScheduleFactoryImpl fac;
	private TransitStopFacility safeFacility;

	public PTLinesGenerator(Scenario sc, Map<Id, BusStop> busStops) {
		this.sc = sc;
		this.busStops = busStops;
		FreeSpeedTravelTimeCalculator fs = new FreeSpeedTravelTimeCalculator();
		TravelDisutility cost = new TravelCostCalculatorFactoryImpl().createTravelDisutility(fs,this.sc.getConfig().planCalcScore() );
		Network network = sc.getNetwork();
		this.dijkstra = new Dijkstra(network, cost, fs);
		this.fac = new TransitScheduleFactoryImpl();
	}

	public TransitSchedule getTransitSchedule() {
		if (this.schedule == null) {
			generate();
		}
		
		
		return this.schedule ;
	}

	private void generate() {

		
		
		
		this.schedule = this.fac.createTransitSchedule();
		
		
		Id id = this.sc.getNetwork().getLinks().get(new IdImpl("el1")).getId();
		Coord c = this.sc.getNetwork().getLinks().get(id).getCoord();
		this.safeFacility = this.fac.createTransitStopFacility(id,c,false);
		this.safeFacility.setLinkId(id);
		
		this.schedule.addStopFacility(this.safeFacility);
		
		for (BusStop stop : this.busStops.values()) {
			createTransitLine(stop);
		}
		
	}

	private void createTransitLine(BusStop stop) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		Id id = stop.id;
		Link link = this.sc.getNetwork().getLinks().get(id);
		Coord c = link.getCoord();
		TransitStopFacility facility = this.fac.createTransitStopFacility(id,c,false);
		facility.setLinkId(id);
		this.schedule.addStopFacility(facility);
		TransitRouteStop rs = this.fac.createTransitRouteStop(facility, 0, 0); //what does arrival and departure delay mean? 
		stops.add(rs);
		TransitRouteStop safeStop = this.fac.createTransitRouteStop(this.safeFacility, 0, 0);
		stops.add(safeStop);
		
		Node start = link.getToNode(); 
		Node end = this.sc.getNetwork().getLinks().get(this.safeFacility.getLinkId()).getFromNode();
		
		Path p = this.dijkstra.calcLeastCostPath(start, end, 0, null, null);
		List<Link> links = p.links;
		
		List<Id> linkIds = new ArrayList<Id>();
		Set<String> modes  = new HashSet<String>();
		modes.add("bus");
		
		for (Link l : links) {
			((LinkImpl)l).setAllowedModes(modes);
			linkIds.add(l.getId());
		}
		
		TransitLine line = this.fac.createTransitLine(id);
		NetworkRoute route = new LinkNetworkRouteImpl(id,this.safeFacility.getLinkId());
		route.setLinkIds(id, linkIds, this.safeFacility.getLinkId());
		TransitRoute tr = this.fac.createTransitRoute(id, route, stops, "bus");
		
		//Vehicles
		Vehicles vehicles = ((ScenarioImpl)this.sc).getVehicles();
		VehiclesFactory vf = vehicles.getFactory();
		VehicleType vt = vf.createVehicleType(id);
		VehicleCapacity vc = vf.createVehicleCapacity();
		vc.setSeats((Integer)stop.capSpinnerValue + 1);
		vc.setStandingRoom(0);
		vt.setCapacity(vc);
		vehicles.getVehicleTypes().put(vt.getId(), vt);
		
		
		int hours = Integer.parseInt(stop.hh);
		int min = Integer.parseInt(stop.mm);
		double depTime = hours * 3600 + min * 60;
		
		//departures
		for (int i = 0; i < (Integer)stop.numDepSpinnerValue; i++) {
			Vehicle veh = vf.createVehicle(new IdImpl(id.toString() + "_veh_" +i), vt);
			vehicles.getVehicles().put(veh.getId(), veh);
			Departure dep = this.fac.createDeparture(new IdImpl(id.toString() + "_dep_" +i), depTime);
			dep.setVehicleId(veh.getId());
			tr.addDeparture(dep);
		}
		line.addRoute(tr);
		this.schedule.addTransitLine(line);
		
	}

}
