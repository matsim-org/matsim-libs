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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 *
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

	/*package*/ final ScenarioImpl scenario;
	/*package*/ final Config config;
	/*package*/ final Network network;
	/*package*/ final TransitScheduleFactory builder;
	/*package*/ final TransitSchedule schedule;
	/*package*/ TransitLine blueLine = null;
	private final Node[] nodes = new Node[5];
	private final Link[] links = new Link[4];
	private final TransitStopFacility[] stopFacilities = new TransitStopFacility[3];
	/*package*/ final Person[] persons = new Person[1]; 

	public Fixture() {
		Config config = ConfigUtils.createConfig();	
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		this.config = this.scenario.getConfig();
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		this.network = this.scenario.getNetwork();
		this.schedule = this.scenario.getTransitSchedule();
		this.builder = this.schedule.getFactory();
	}

	protected void init() {
		buildNetwork();
		buildStops();
		buildVehicles();
		buildBlueLine();
		buildPopulation();
	}
	
	protected void buildNetwork() {
		this.nodes[0] = this.network.getFactory().createNode(this.scenario.createId("0"),  this.scenario.createCoord(    0, 5000));
		this.nodes[1] = this.network.getFactory().createNode(this.scenario.createId("1"),  this.scenario.createCoord( 4000, 5000));
		this.nodes[2] = this.network.getFactory().createNode(this.scenario.createId("2"),  this.scenario.createCoord( 8000, 5000));
		this.nodes[3] = this.network.getFactory().createNode(this.scenario.createId("3"),  this.scenario.createCoord(12000, 5000));
		this.nodes[4] = this.network.getFactory().createNode(this.scenario.createId("4"),  this.scenario.createCoord(16000, 5000));
		for (int i = 0; i < 5; i++) {
			this.network.addNode(this.nodes[i]);
		}
		this.links[0] = this.network.getFactory().createLink(this.scenario.createId( "0"), this.nodes[0], this.nodes[1]);
		this.links[1] = this.network.getFactory().createLink(this.scenario.createId( "1"), this.nodes[1], this.nodes[2]);
		this.links[2] = this.network.getFactory().createLink(this.scenario.createId( "2"), this.nodes[2], this.nodes[3]);
		this.links[3] = this.network.getFactory().createLink(this.scenario.createId( "3"), this.nodes[3], this.nodes[4]);
		
		this.links[0].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk + "," + TransportMode.car));
		this.links[1].setAllowedModes(CollectionUtils.stringToSet(TransportMode.car));
		this.links[2].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk + "," + TransportMode.car));
		this.links[3].setAllowedModes(CollectionUtils.stringToSet(TransportMode.walk + "," + TransportMode.transit_walk));
		
		for (int i = 0; i < 4; i++) {
			this.links[i].setLength(5000.0);
			this.links[i].setFreespeed(20.0);
			this.links[i].setCapacity(2000.0);
			this.links[i].setNumberOfLanes(1.0);
			this.network.addLink(this.links[i]);
		}
	}

	protected void buildStops() {
		this.stopFacilities[0] = this.builder.createTransitStopFacility(this.scenario.createId( "0"), this.scenario.createCoord( 4000,  5002), true);
		this.stopFacilities[1] = this.builder.createTransitStopFacility(this.scenario.createId( "1"), this.scenario.createCoord( 8000,  4998), true);
		this.stopFacilities[2] = this.builder.createTransitStopFacility(this.scenario.createId( "2"), this.scenario.createCoord(12000,  5002), true);
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

	protected void buildVehicles() {
		Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory vb = vehicles.getFactory();
        VehicleType vehicleType = vb.createVehicleType(scenario.createId("transitVehicleType"));
        VehicleCapacity capacity = vb.createVehicleCapacity();
        capacity.setSeats(Integer.valueOf(101));
        capacity.setStandingRoom(Integer.valueOf(0));
        vehicleType.setCapacity(capacity);
        vehicles.addVehicleType(vehicleType);
        vehicles.addVehicle( vb.createVehicle(this.scenario.createId("veh1"), vehicleType));
        vehicles.addVehicle( vb.createVehicle(this.scenario.createId("veh2"), vehicleType));
        vehicles.addVehicle( vb.createVehicle(this.scenario.createId("veh3"), vehicleType));
        vehicles.addVehicle( vb.createVehicle(this.scenario.createId("veh4"), vehicleType));
        vehicles.addVehicle( vb.createVehicle(this.scenario.createId("veh5"), vehicleType));
        vehicles.addVehicle( vb.createVehicle(this.scenario.createId("veh6"), vehicleType));
	}
	
	protected void buildBlueLine() {
		this.blueLine = this.builder.createTransitLine(this.scenario.createId("blue"));
		this.schedule.addTransitLine(this.blueLine);
		{ // route from left to right
			NetworkRoute netRoute = new LinkNetworkRouteImpl(this.links[0].getId(), this.links[2].getId());
			List<Id> routeLinks = new ArrayList<Id>();
			Collections.addAll(routeLinks, this.links[1].getId());
			netRoute.setLinkIds(this.links[0].getId(), routeLinks, this.links[2].getId());
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
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
			TransitRoute route = this.builder.createTransitRoute(this.scenario.createId("blue A > C"), netRoute, stops, "train");
			this.blueLine.addRoute(route);

			Departure dep;
			dep = this.builder.createDeparture(this.scenario.createId("b>10"), 8.0*3600 +  6.0*60);
			dep.setVehicleId(this.scenario.createId("veh1"));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(this.scenario.createId("b>11"), 8.0*3600 + 26.0*60);
			dep.setVehicleId(this.scenario.createId("veh2"));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(this.scenario.createId("b>12"), 8.0*3600 + 46.0*60);
			dep.setVehicleId(this.scenario.createId("veh3"));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(this.scenario.createId("b>13"), 9.0*3600 +  6.0*60);
			dep.setVehicleId(this.scenario.createId("veh4"));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(this.scenario.createId("b>14"), 9.0*3600 + 26.0*60);
			dep.setVehicleId(this.scenario.createId("veh5"));
			route.addDeparture(dep);
			dep = this.builder.createDeparture(this.scenario.createId("b>15"), 9.0*3600 + 46.0*60);
			dep.setVehicleId(this.scenario.createId("veh6"));
			route.addDeparture(dep);
		}
	}
	
	protected void buildPopulation() {
		persons[0] = createPerson(scenario, "0", "pt");
		scenario.getPopulation().addPerson(persons[0]);
	}
	
	private Person createPerson(Scenario scenario, String id, String mode) {
		PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(scenario.createId(id));
		
		person.setAge(50);
		person.setSex("m");

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", scenario.createId("0"));
		((ActivityImpl) from).setCoord(this.nodes[0].getCoord());
		Leg leg = scenario.getPopulation().getFactory().createLeg(mode);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", scenario.createId("3"));
		((ActivityImpl) to).setCoord(this.nodes[4].getCoord());
		
		from.setEndTime(8*3600);
		leg.setDepartureTime(8*3600);
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);
		
		person.addPlan(plan);
		
		return person;
	}

}
