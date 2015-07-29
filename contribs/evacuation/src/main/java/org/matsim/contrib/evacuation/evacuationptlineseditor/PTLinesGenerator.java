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

package org.matsim.contrib.evacuation.evacuationptlineseditor;

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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
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
	private final Map<Id<Link>, BusStop> busStops;
	private final Dijkstra dijkstra;
	private final TransitScheduleFactoryImpl fac;
	private Id safeId;

	public PTLinesGenerator(Scenario sc, Map<Id<Link>, BusStop> busStops) {
		this.sc = sc;
		this.busStops = busStops;
		FreeSpeedTravelTime fs = new FreeSpeedTravelTime();
		TravelDisutility cost = new TravelTimeAndDistanceBasedTravelDisutilityFactory().createTravelDisutility(fs, this.sc.getConfig().planCalcScore());
		Network network = sc.getNetwork();
		this.dijkstra = new Dijkstra(network, cost, fs);
		this.fac = new TransitScheduleFactoryImpl();
	}

	public TransitSchedule getTransitSchedule() {
		if (this.schedule == null) {
			generate();
		}

		return this.schedule;
	}

	private void generate() {

		this.schedule = this.fac.createTransitSchedule();

		Link safeLink = this.sc.getNetwork().getLinks().get(Id.create("el1", Link.class));

		TransitStopFacility safeFacility = this.fac.createTransitStopFacility(Id.create(safeLink.getId(), TransitStopFacility.class), safeLink.getToNode().getCoord(), false);
		safeFacility.setLinkId(safeLink.getId());
		this.schedule.addStopFacility(safeFacility);

		this.safeId = safeLink.getId();
		for (BusStop stop : this.busStops.values()) {
			createTransitLine(stop);
		}

	}

	private void createTransitLine(BusStop stop) {

		if ((stop.id == null) || (stop.hh.equals("")) || (stop.mm.equals("")) || (stop.hh.equals("--")) || (stop.mm.equals("--")))
			return;

		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		Id<Link> id = stop.id;
		Link link = this.sc.getNetwork().getLinks().get(id);
		Coord c = link.getCoord();
		TransitStopFacility facility = this.fac.createTransitStopFacility(Id.create(id, TransitStopFacility.class), c, false);
		facility.setLinkId(id);
		this.schedule.addStopFacility(facility);
		TransitRouteStop rs = this.fac.createTransitRouteStop(facility, 0, 0);
		
		stops.add(rs);

		Node start = link.getToNode();
		Node end = this.sc.getNetwork().getLinks().get(this.safeId).getFromNode();
		Path p = this.dijkstra.calcLeastCostPath(start, end, 0, null, null);
		List<Link> links = p.links.subList(0, p.links.size() - 1);

		Link safeLink = p.links.get(p.links.size() - 1);

		TransitStopFacility safeFacility = this.schedule.getFacilities().get(Id.create(safeLink.getId(), TransitStopFacility.class));

		if (safeFacility == null) {
			safeFacility = this.fac.createTransitStopFacility(Id.create(safeLink.getId(), TransitStopFacility.class), safeLink.getToNode().getCoord(), false);
			safeFacility.setLinkId(safeLink.getId());
			this.schedule.addStopFacility(safeFacility);
		}

		TransitRouteStop safeStop = this.fac.createTransitRouteStop(safeFacility, 0, 0);
		stops.add(safeStop);

		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Set<String> modes = new HashSet<String>();
		modes.add("bus");

		for (Link l : links) {
			Set<String> m = l.getAllowedModes();
			for (String s : m) {
				modes.add(s);
			}
			((LinkImpl) l).setAllowedModes(modes);
			linkIds.add(l.getId());
		}

		TransitLine line = this.fac.createTransitLine(Id.create(id, TransitLine.class));
		NetworkRoute route = new LinkNetworkRouteImpl(id, safeFacility.getLinkId());
		route.setLinkIds(id, linkIds, safeFacility.getLinkId());
		TransitRoute tr = this.fac.createTransitRoute(Id.create(id, TransitRoute.class), route, stops, "bus");

		// Vehicles
		Vehicles vehicles = ((ScenarioImpl) this.sc).getTransitVehicles();
		VehiclesFactory vf = vehicles.getFactory();
		VehicleType vt = vf.createVehicleType(Id.create(id, VehicleType.class));
		VehicleCapacity vc = vf.createVehicleCapacity();
		vc.setSeats((Integer) stop.capSpinnerValue + 1);
		vc.setStandingRoom(0);
		vt.setCapacity(vc);
		vehicles.addVehicleType(vt);

		int hours = Integer.parseInt(stop.hh);
		int min = Integer.parseInt(stop.mm);
		double depTime = hours * 3600 + min * 60;

		// departures
		for (int i = 0; i < (Integer) stop.numDepSpinnerValue; i++) {
			Vehicle veh = vf.createVehicle(Id.create(id.toString() + "_veh_" + i, Vehicle.class), vt);
			vehicles.addVehicle( veh);
			Departure dep = this.fac.createDeparture(Id.create(id.toString() + "_dep_" + i, Departure.class), depTime);
			dep.setVehicleId(veh.getId());
			tr.addDeparture(dep);
			depTime += 5 * 30;
		}
		line.addRoute(tr);
		this.schedule.addTransitLine(line);

	}

}
