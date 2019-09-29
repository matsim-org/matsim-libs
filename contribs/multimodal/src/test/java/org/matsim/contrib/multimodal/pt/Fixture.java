/* *********************************************************************** *
 * project: org.matsim.*
 * Fixture.java
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

package org.matsim.contrib.multimodal.pt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 * Network:
 * <pre>
 *
 * (n) node
 * [s] stop facilities
 *  l  link
 *  A  stop name
 *
 *           A        B        C
 *      0   [0]  1   [1]  2   [2]   3
 * (0)------(1)======(2)======(3)------(4)
 *           |                 |
 *           -------------------
 *                    4
 * </pre>
 * Coordinates: 4km between two stops along x-axis.
 *
 * Transit Lines:
 * <ul>
 * <li>blue line: regular line every 20 minutes from A to C with stop on all facilities</li>
 * </ul>
 *
 * @author cdobler
 */
/*package*/ class Fixture {

	/*package*/ final Scenario scenario;
	/*package*/ private final Config config;
	/*package*/ private final Network network;
	/*package*/ private final TransitScheduleFactory builder;
	/*package*/ private final TransitSchedule schedule;
	/*package*/ private TransitLine blueLine = null;
	private final Node[] nodes = new Node[5];
	private final Link[] links = new Link[5];
	private final TransitStopFacility[] stopFacilities = new TransitStopFacility[3];

	public Fixture() {
		this.config = ConfigUtils.createConfig();	
		this.config.transit().setUseTransit(true);

		this.scenario = ScenarioUtils.createScenario(config);
		this.network = this.scenario.getNetwork();
		this.schedule = this.scenario.getTransitSchedule();
		this.builder = this.schedule.getFactory();
	}

	void init() {
		buildNetwork();
		buildStops();
		buildVehicles();
		buildBlueLine();
//		buildPopulation();
	}
	
	void buildNetwork() {
		this.nodes[0] = this.network.getFactory().createNode(Id.create("0", Node.class), new Coord((double) 0, (double) 5000));
		this.nodes[1] = this.network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 4000, (double) 5000));
		this.nodes[2] = this.network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 8000, (double) 5000));
		this.nodes[3] = this.network.getFactory().createNode(Id.create("3", Node.class), new Coord((double) 12000, (double) 5000));
		this.nodes[4] = this.network.getFactory().createNode(Id.create("4", Node.class), new Coord((double) 16000, (double) 5000));
		for (int i = 0; i < 5; i++) {
			this.network.addNode(this.nodes[i]);
		}
		this.links[0] = this.network.getFactory().createLink(Id.create("0", Link.class), this.nodes[0], this.nodes[1]);
		this.links[1] = this.network.getFactory().createLink(Id.create("1", Link.class), this.nodes[1], this.nodes[2]);
		this.links[2] = this.network.getFactory().createLink(Id.create("2", Link.class), this.nodes[2], this.nodes[3]);
		this.links[3] = this.network.getFactory().createLink(Id.create("3", Link.class), this.nodes[3], this.nodes[4]);
		this.links[4] = this.network.getFactory().createLink(Id.create("4", Link.class), this.nodes[1], this.nodes[3]);
		
		this.links[0].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk + "," + TransportMode.car));
		this.links[1].setAllowedModes(CollectionUtils.stringToSet(TransportMode.car));
		this.links[2].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk + "," + TransportMode.car));
		this.links[3].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk));
		this.links[4].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk + "," + TransportMode.car));
		
		for (int i = 0; i < 5; i++) {
			this.links[i].setLength(5000.0);
			this.links[i].setFreespeed(20.0);
			this.links[i].setCapacity(2000.0);
			this.links[i].setNumberOfLanes(1.0);
			this.network.addLink(this.links[i]);
		}
	}

	void buildStops() {
		this.stopFacilities[0] = this.builder.createTransitStopFacility(Id.create( "0", TransitStopFacility.class), new Coord((double) 4000, (double) 5002), true);
		this.stopFacilities[1] = this.builder.createTransitStopFacility(Id.create( "1", TransitStopFacility.class), new Coord((double) 8000, (double) 4998), true);
		this.stopFacilities[2] = this.builder.createTransitStopFacility(Id.create( "2", TransitStopFacility.class), new Coord((double) 12000, (double) 5002), true);
		this.stopFacilities[0].setName("A");
		this.stopFacilities[1].setName("B");
		this.stopFacilities[2].setName("C");
		this.stopFacilities[0].setLinkId(this.links[0].getId());
		this.stopFacilities[1].setLinkId(this.links[1].getId());
		this.stopFacilities[2].setLinkId(this.links[2].getId());

		for (TransitStopFacility stopFacility : this.stopFacilities) {
			this.schedule.addStopFacility(stopFacility);
		}
	}

	void buildVehicles() {
		Vehicles vehicles = scenario.getTransitVehicles();
        VehiclesFactory vb = vehicles.getFactory();
        VehicleType vehicleType = vb.createVehicleType(Id.create("transitVehicleType", VehicleType.class));
//        VehicleCapacity capacity = vb.createVehicleCapacity();
		vehicleType.getCapacity().setSeats(101);
		vehicleType.getCapacity().setStandingRoom(0);
//        vehicleType.setCapacity(capacity);
        vehicles.addVehicleType(vehicleType);
        vehicles.addVehicle( vb.createVehicle(Id.create("veh1", Vehicle.class), vehicleType));
        vehicles.addVehicle( vb.createVehicle(Id.create("veh2", Vehicle.class), vehicleType));
        vehicles.addVehicle( vb.createVehicle(Id.create("veh3", Vehicle.class), vehicleType));
        vehicles.addVehicle( vb.createVehicle(Id.create("veh4", Vehicle.class), vehicleType));
        vehicles.addVehicle( vb.createVehicle(Id.create("veh5", Vehicle.class), vehicleType));
        vehicles.addVehicle( vb.createVehicle(Id.create("veh6", Vehicle.class), vehicleType));
	}
	
	void buildBlueLine() {
		this.blueLine = this.builder.createTransitLine(Id.create("blue", TransitLine.class));
		this.schedule.addTransitLine(this.blueLine);
		{ // route from left to right
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.links[0].getId(), this.links[2].getId());
			List<Id<Link>> routeLinks = new ArrayList<>();
			Collections.addAll(routeLinks, this.links[1].getId());
			netRoute.setLinkIds(this.links[0].getId(), routeLinks, this.links[2].getId());
			List<TransitRouteStop> stops = new ArrayList<>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStop(this.stopFacilities[0], Time.UNDEFINED_TIME, 0.0);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[1], Time.UNDEFINED_TIME, 7.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[2], 12.0 * 60, 16.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(Id.create("blue A > C", TransitRoute.class), netRoute, stops, "train");
			this.blueLine.addRoute(route);

			Departure dep;
			dep = this.builder.createDeparture(Id.create("b>10", Departure.class), 8.0*3600 +  6.0*60);
			dep.setVehicleId(Id.create("veh1", Vehicle.class));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(Id.create("b>11", Departure.class), 8.0*3600 + 26.0*60);
			dep.setVehicleId(Id.create("veh2", Vehicle.class));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(Id.create("b>12", Departure.class), 8.0*3600 + 46.0*60);
			dep.setVehicleId(Id.create("veh3", Vehicle.class));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(Id.create("b>13", Departure.class), 9.0*3600 +  6.0*60);
			dep.setVehicleId(Id.create("veh4", Vehicle.class));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(Id.create("b>14", Departure.class), 9.0*3600 + 26.0*60);
			dep.setVehicleId(Id.create("veh5", Vehicle.class));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(Id.create("b>15", Departure.class), 9.0*3600 + 46.0*60);
			dep.setVehicleId(Id.create("veh6", Vehicle.class));
			route.addDeparture(dep);
		}
	}
	
	/*package*/ Person createPersonAndAdd(Scenario scenario, String id, String mode) {
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(id, Person.class));

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("0", Link.class));
		((Activity) from).setCoord(this.nodes[0].getCoord());
		Leg leg = scenario.getPopulation().getFactory().createLeg(mode);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("3", Link.class));
		((Activity) to).setCoord(this.nodes[4].getCoord());
		
		from.setEndTime(8*3600);
		leg.setDepartureTime(8*3600);
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);
		
		person.addPlan(plan);
		
		this.scenario.getPopulation().addPerson(person);
		return person;
	}
}
