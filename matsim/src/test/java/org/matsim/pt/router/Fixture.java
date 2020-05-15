/* *********************************************************************** *
 * project: org.matsim.*
 * Fixture.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Network:
 * <pre>
 *
 * (n) node                              K(12)-------------(13)L
 * [s] stop facilities                   / [19]    21     [20]\
 *  l  link                             /                      \
 *  A  stop name                     20/                        \22
 *                                    /                          \
 *           A        B        C [18]/    D       E        F      \[21]G      H       J
 *      0   [0]  1   [2]  2   [4](11) 3  [6]  4  [8]  5  [10]  6 (14)[12] 7  [14] 8  [16]
 * (0)------(1)======(2)======(3)=======(4)======(5)======(6)======(7)======(8)======(9)------(10)
 *          [1]  17  [3]  16  [5]  \ 15  [7] 14  [9]  13 [11]  12 /[13] 11 [15] 10  [17]  9
 *                                  \                            /
 *                                 25\                          /
 *                                    \                        /23
 *                                     \[23]       24    [22] /
 *                                     N(16)--------------(15)M
 *
 * </pre>
 * Coordinates: 4km between two stops along x-axis, 5km along y-axis. Thus: A is at 4000/5000,
 * C at 12000/5000, N at 16000/0, L at 24000/10000, J at 36000/5000.
 *
 * Transit Lines:
 * <ul>
 * <li>red line: express line once an hour from C to G and back without stop</li>
 * <li>blue line: regular line every 20 minutes from A to J with stop on all facilities</li>
 * <li>green line: circular line every 10 minutes C-K-L-G-M-N-C</li>
 * </ul>
 *
 * @author mrieser
 */
/*package*/ class Fixture {

	/*package*/ final MutableScenario scenario;
	/*package*/ final Config config;
	/*package*/ final Network network;
	/*package*/ final TransitScheduleFactory builder;
	/*package*/ final TransitSchedule schedule;
	/*package*/ TransitLine redLine = null;
	/*package*/ TransitLine blueLine = null;
	/*package*/ TransitLine greenLine = null;
	private final Node[] nodes = new Node[17];
	private final Link[] links = new Link[26];
	private final TransitStopFacility[] stopFacilities = new TransitStopFacility[24];

	public Fixture() {
		this.config = ConfigUtils.createConfig();
		this.config.transit().setUseTransit(true);
		
		ScenarioBuilder scBuilder = new ScenarioBuilder(config) ;
		this.scenario = (MutableScenario) scBuilder.build() ;

		this.network = this.scenario.getNetwork();
		this.schedule = this.scenario.getTransitSchedule();
		this.builder = this.schedule.getFactory();
	}

	protected void init() {
		buildNetwork();
		buildStops();
		buildRedLine();
		buildBlueLine();
		buildGreenLine();
	}

	protected void buildNetwork() {
		this.nodes[0]  = this.network.getFactory().createNode(Id.create("0", Node.class), new Coord((double) 0, (double) 5000));
		this.nodes[1]  = this.network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 4000, (double) 5000));
		this.nodes[2]  = this.network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 8000, (double) 5000));
		this.nodes[3]  = this.network.getFactory().createNode(Id.create("3", Node.class), new Coord((double) 12000, (double) 5000));
		this.nodes[4]  = this.network.getFactory().createNode(Id.create("4", Node.class), new Coord((double) 16000, (double) 5000));
		this.nodes[5]  = this.network.getFactory().createNode(Id.create("5", Node.class), new Coord((double) 20000, (double) 5000));
		this.nodes[6]  = this.network.getFactory().createNode(Id.create("6", Node.class), new Coord((double) 24000, (double) 5000));
		this.nodes[7]  = this.network.getFactory().createNode(Id.create("7", Node.class), new Coord((double) 28000, (double) 5000));
		this.nodes[8]  = this.network.getFactory().createNode(Id.create("8", Node.class), new Coord((double) 32000, (double) 5000));
		this.nodes[9]  = this.network.getFactory().createNode(Id.create("9", Node.class), new Coord((double) 36000, (double) 5000));
		this.nodes[10] = this.network.getFactory().createNode(Id.create("10", Node.class), new Coord((double) 40000, (double) 5000));
		this.nodes[11] = this.network.getFactory().createNode(Id.create("11", Node.class), new Coord((double) 12000, (double) 5000));
		this.nodes[12] = this.network.getFactory().createNode(Id.create("12", Node.class), new Coord((double) 16000, (double) 10000));
		this.nodes[13] = this.network.getFactory().createNode(Id.create("13", Node.class), new Coord((double) 24000, (double) 10000));
		this.nodes[14] = this.network.getFactory().createNode(Id.create("14", Node.class), new Coord((double) 28000, (double) 5000));
		this.nodes[15] = this.network.getFactory().createNode(Id.create("15", Node.class), new Coord((double) 24000, (double) 0));
		this.nodes[16] = this.network.getFactory().createNode(Id.create("16", Node.class), new Coord((double) 16000, (double) 0));
		for (int i = 0; i < 17; i++) {
			this.network.addNode(this.nodes[i]);
		}
		this.links[0]  = this.network.getFactory().createLink(Id.create( "0", Link.class), this.nodes[ 0], this.nodes[ 1]);
		this.links[1]  = this.network.getFactory().createLink(Id.create( "1", Link.class), this.nodes[ 1], this.nodes[ 2]);
		this.links[2]  = this.network.getFactory().createLink(Id.create( "2", Link.class), this.nodes[ 2], this.nodes[ 3]);
		this.links[3]  = this.network.getFactory().createLink(Id.create( "3", Link.class), this.nodes[ 3], this.nodes[ 4]);
		this.links[4]  = this.network.getFactory().createLink(Id.create( "4", Link.class), this.nodes[ 4], this.nodes[ 5]);
		this.links[5]  = this.network.getFactory().createLink(Id.create( "5", Link.class), this.nodes[ 5], this.nodes[ 6]);
		this.links[6]  = this.network.getFactory().createLink(Id.create( "6", Link.class), this.nodes[ 6], this.nodes[ 7]);
		this.links[7]  = this.network.getFactory().createLink(Id.create( "7", Link.class), this.nodes[ 7], this.nodes[ 8]);
		this.links[8]  = this.network.getFactory().createLink(Id.create( "8", Link.class), this.nodes[ 8], this.nodes[ 9]);
		this.links[9]  = this.network.getFactory().createLink(Id.create( "9", Link.class), this.nodes[10], this.nodes[ 9]);
		this.links[10] = this.network.getFactory().createLink(Id.create("10", Link.class), this.nodes[ 9], this.nodes[ 8]);
		this.links[11] = this.network.getFactory().createLink(Id.create("11", Link.class), this.nodes[ 8], this.nodes[ 7]);
		this.links[12] = this.network.getFactory().createLink(Id.create("12", Link.class), this.nodes[ 7], this.nodes[ 6]);
		this.links[13] = this.network.getFactory().createLink(Id.create("13", Link.class), this.nodes[ 6], this.nodes[ 5]);
		this.links[14] = this.network.getFactory().createLink(Id.create("14", Link.class), this.nodes[ 5], this.nodes[ 4]);
		this.links[15] = this.network.getFactory().createLink(Id.create("15", Link.class), this.nodes[ 4], this.nodes[ 3]);
		this.links[16] = this.network.getFactory().createLink(Id.create("16", Link.class), this.nodes[ 3], this.nodes[ 2]);
		this.links[17] = this.network.getFactory().createLink(Id.create("17", Link.class), this.nodes[ 2], this.nodes[ 1]);
		for (int i = 0; i < 18; i++) {
			this.links[i].setLength(5000.0);
			this.links[i].setFreespeed(44.44);
			this.links[i].setCapacity(2000.0);
			this.links[i].setNumberOfLanes(1.0);
			this.network.addLink(this.links[i]);
		}
		this.links[18] = null;
		this.links[19] = null;
		this.links[20] = this.network.getFactory().createLink(Id.create("20", Link.class), this.nodes[11], this.nodes[12]);
		this.links[21] = this.network.getFactory().createLink(Id.create("21", Link.class), this.nodes[12], this.nodes[13]);
		this.links[22] = this.network.getFactory().createLink(Id.create("22", Link.class), this.nodes[13], this.nodes[14]);
		this.links[23] = this.network.getFactory().createLink(Id.create("23", Link.class), this.nodes[14], this.nodes[15]);
		this.links[24] = this.network.getFactory().createLink(Id.create("24", Link.class), this.nodes[15], this.nodes[16]);
		this.links[25] = this.network.getFactory().createLink(Id.create("25", Link.class), this.nodes[16], this.nodes[11]);
		for (int i = 20; i < 26; i++) {
			this.links[i].setLength(10000.0);
			this.links[i].setFreespeed(20.0);
			this.links[i].setCapacity(2000.0);
			this.links[i].setNumberOfLanes(1.0);
			this.network.addLink(this.links[i]);
		}
	}

	protected void buildStops() {
		this.stopFacilities[ 0] = this.builder.createTransitStopFacility(Id.create( "0", TransitStopFacility.class), new Coord((double) 4000, (double) 5002), true);
		this.stopFacilities[ 1] = this.builder.createTransitStopFacility(Id.create( "1", TransitStopFacility.class), new Coord((double) 4000, (double) 4998), true);
		this.stopFacilities[ 2] = this.builder.createTransitStopFacility(Id.create( "2", TransitStopFacility.class), new Coord((double) 8000, (double) 5002), true);
		this.stopFacilities[ 3] = this.builder.createTransitStopFacility(Id.create( "3", TransitStopFacility.class), new Coord((double) 8000, (double) 4998), true);
		this.stopFacilities[ 4] = this.builder.createTransitStopFacility(Id.create( "4", TransitStopFacility.class), new Coord((double) 12000, (double) 5002), true);
		this.stopFacilities[ 5] = this.builder.createTransitStopFacility(Id.create( "5", TransitStopFacility.class), new Coord((double) 12000, (double) 4998), true);
		this.stopFacilities[ 6] = this.builder.createTransitStopFacility(Id.create( "6", TransitStopFacility.class), new Coord((double) 16000, (double) 5002), true);
		this.stopFacilities[ 7] = this.builder.createTransitStopFacility(Id.create( "7", TransitStopFacility.class), new Coord((double) 16000, (double) 4998), true);
		this.stopFacilities[ 8] = this.builder.createTransitStopFacility(Id.create( "8", TransitStopFacility.class), new Coord((double) 20000, (double) 5002), true);
		this.stopFacilities[ 9] = this.builder.createTransitStopFacility(Id.create( "9", TransitStopFacility.class), new Coord((double) 20000, (double) 4998), true);
		this.stopFacilities[10] = this.builder.createTransitStopFacility(Id.create("10", TransitStopFacility.class), new Coord((double) 24000, (double) 5002), true);
		this.stopFacilities[11] = this.builder.createTransitStopFacility(Id.create("11", TransitStopFacility.class), new Coord((double) 24000, (double) 4998), true);
		this.stopFacilities[12] = this.builder.createTransitStopFacility(Id.create("12", TransitStopFacility.class), new Coord((double) 28000, (double) 5002), true);
		this.stopFacilities[13] = this.builder.createTransitStopFacility(Id.create("13", TransitStopFacility.class), new Coord((double) 28000, (double) 4998), true);
		this.stopFacilities[14] = this.builder.createTransitStopFacility(Id.create("14", TransitStopFacility.class), new Coord((double) 32000, (double) 5002), true);
		this.stopFacilities[15] = this.builder.createTransitStopFacility(Id.create("15", TransitStopFacility.class), new Coord((double) 32000, (double) 4998), true);
		this.stopFacilities[16] = this.builder.createTransitStopFacility(Id.create("16", TransitStopFacility.class), new Coord((double) 36000, (double) 5002), true);
		this.stopFacilities[17] = this.builder.createTransitStopFacility(Id.create("17", TransitStopFacility.class), new Coord((double) 36000, (double) 4998), true);
		this.stopFacilities[18] = this.builder.createTransitStopFacility(Id.create("18", TransitStopFacility.class), new Coord((double) 12000, (double) 5000), true);
		this.stopFacilities[19] = this.builder.createTransitStopFacility(Id.create("19", TransitStopFacility.class), new Coord((double) 16000, (double) 10000), true);
		this.stopFacilities[20] = this.builder.createTransitStopFacility(Id.create("20", TransitStopFacility.class), new Coord((double) 24000, (double) 10000), true);
		this.stopFacilities[21] = this.builder.createTransitStopFacility(Id.create("21", TransitStopFacility.class), new Coord((double) 28000, (double) 5000), true);
		this.stopFacilities[22] = this.builder.createTransitStopFacility(Id.create("22", TransitStopFacility.class), new Coord((double) 24000, (double) 0), true);
		this.stopFacilities[23] = this.builder.createTransitStopFacility(Id.create("23", TransitStopFacility.class), new Coord((double) 16000, (double) 0), true);
		this.stopFacilities[ 0].setName("A");
		this.stopFacilities[ 1].setName("A");
		this.stopFacilities[ 2].setName("B");
		this.stopFacilities[ 3].setName("B");
		this.stopFacilities[ 4].setName("C");
		this.stopFacilities[ 5].setName("C");
		this.stopFacilities[ 6].setName("D");
		this.stopFacilities[ 7].setName("D");
		this.stopFacilities[ 8].setName("E");
		this.stopFacilities[ 9].setName("E");
		this.stopFacilities[10].setName("F");
		this.stopFacilities[11].setName("F");
		this.stopFacilities[12].setName("G");
		this.stopFacilities[13].setName("G");
		this.stopFacilities[14].setName("H");
		this.stopFacilities[15].setName("H");
		this.stopFacilities[16].setName("I");
		this.stopFacilities[17].setName("I");
		this.stopFacilities[18].setName("C");
		this.stopFacilities[19].setName("K");
		this.stopFacilities[20].setName("L");
		this.stopFacilities[21].setName("G");
		this.stopFacilities[22].setName("M");
		this.stopFacilities[23].setName("N");
		this.stopFacilities[ 0].setLinkId(this.links[ 0].getId());
		this.stopFacilities[ 1].setLinkId(this.links[17].getId());
		this.stopFacilities[ 2].setLinkId(this.links[ 1].getId());
		this.stopFacilities[ 3].setLinkId(this.links[16].getId());
		this.stopFacilities[ 4].setLinkId(this.links[ 2].getId());
		this.stopFacilities[ 5].setLinkId(this.links[15].getId());
		this.stopFacilities[ 6].setLinkId(this.links[ 3].getId());
		this.stopFacilities[ 7].setLinkId(this.links[14].getId());
		this.stopFacilities[ 8].setLinkId(this.links[ 4].getId());
		this.stopFacilities[ 9].setLinkId(this.links[13].getId());
		this.stopFacilities[10].setLinkId(this.links[ 5].getId());
		this.stopFacilities[11].setLinkId(this.links[12].getId());
		this.stopFacilities[12].setLinkId(this.links[ 6].getId());
		this.stopFacilities[13].setLinkId(this.links[11].getId());
		this.stopFacilities[14].setLinkId(this.links[ 7].getId());
		this.stopFacilities[15].setLinkId(this.links[10].getId());
		this.stopFacilities[16].setLinkId(this.links[ 8].getId());
		this.stopFacilities[17].setLinkId(this.links[ 9].getId());
		this.stopFacilities[18].setLinkId(this.links[25].getId());
		this.stopFacilities[19].setLinkId(this.links[20].getId());
		this.stopFacilities[20].setLinkId(this.links[21].getId());
		this.stopFacilities[21].setLinkId(this.links[22].getId());
		this.stopFacilities[22].setLinkId(this.links[23].getId());
		this.stopFacilities[23].setLinkId(this.links[24].getId());
		for (TransitStopFacility stopFacility : this.stopFacilities) {
			this.schedule.addStopFacility(stopFacility);
		}
	}

	protected void buildRedLine() {
		this.redLine = this.builder.createTransitLine(Id.create("red", TransitLine.class));
		this.schedule.addTransitLine(this.redLine);
		{ // route from left to right
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.links[2].getId(), this.links[6].getId());
			List<Id<Link>> routeLinks = new ArrayList<>();
			Collections.addAll(routeLinks, this.links[3].getId(), this.links[4].getId(), this.links[5].getId());
			netRoute.setLinkIds(this.links[2].getId(), routeLinks, this.links[6].getId());
			List<TransitRouteStop> stops = new ArrayList<>();
			stops.add(this.builder.createTransitRouteStopBuilder(this.stopFacilities[4]).departureOffset(0.0).build());
			stops.add(this.builder.createTransitRouteStopBuilder(this.stopFacilities[12]).departureOffset(9.0*60).build());
			TransitRoute route = this.builder.createTransitRoute(Id.create("red C > G", TransitRoute.class), netRoute, stops, "train");
			this.redLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(Id.create("r>01", Departure.class), 6.0*3600));
			route.addDeparture(this.builder.createDeparture(Id.create("r>02", Departure.class), 7.0*3600));
			route.addDeparture(this.builder.createDeparture(Id.create("r>03", Departure.class), 8.0*3600));
			route.addDeparture(this.builder.createDeparture(Id.create("r>04", Departure.class), 9.0*3600));
		}
		{ // route from right to left
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.links[11].getId(), this.links[15].getId());
			List<Id<Link>> routeLinks = new ArrayList<>();
			Collections.addAll(routeLinks, this.links[12].getId(), this.links[13].getId(), this.links[14].getId());
			netRoute.setLinkIds(this.links[11].getId(), routeLinks, this.links[15].getId());
			List<TransitRouteStop> stops = new ArrayList<>();
			stops.add(this.builder.createTransitRouteStopBuilder(this.stopFacilities[13]).departureOffset(0.0).build());
			stops.add(this.builder.createTransitRouteStopBuilder(this.stopFacilities[5]).departureOffset(9.0*60).build());
			TransitRoute route = this.builder.createTransitRoute(Id.create("red G > C", TransitRoute.class), netRoute, stops, "train");
			this.redLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(Id.create("r<01", Departure.class), 6.0*3600 + 10.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("r<02", Departure.class), 7.0*3600 + 10.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("r<03", Departure.class), 8.0*3600 + 10.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("r<04", Departure.class), 9.0*3600 + 10.0*60));
		}
	}

	protected void buildBlueLine() {
		this.blueLine = this.builder.createTransitLine(Id.create("blue", TransitLine.class));
		this.schedule.addTransitLine(this.blueLine);
		{ // route from left to right
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.links[0].getId(), this.links[8].getId());
			List<Id<Link>> routeLinks = new ArrayList<>();
			Collections.addAll(routeLinks, this.links[1].getId(), this.links[2].getId(), this.links[3].getId(), this.links[4].getId(), this.links[5].getId(), this.links[6].getId(), this.links[7].getId());
			netRoute.setLinkIds(this.links[0].getId(), routeLinks, this.links[8].getId());
			List<TransitRouteStop> stops = new ArrayList<>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 0]).departureOffset(0.0).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 2]).departureOffset(7.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 4], 12.0 * 60, 16.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 6]).departureOffset(23.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 8]).departureOffset(30.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[10]).departureOffset(37.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[12], 42.0 * 60, 46.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[14]).departureOffset(53.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[16]).arrivalOffset(58.0 * 60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(Id.create("blue A > I", TransitRoute.class), netRoute, stops, "train");
			this.blueLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(Id.create("b>01", Departure.class), 5.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>02", Departure.class), 5.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>03", Departure.class), 5.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>04", Departure.class), 6.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>05", Departure.class), 6.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>06", Departure.class), 6.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>07", Departure.class), 7.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>08", Departure.class), 7.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>09", Departure.class), 7.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>10", Departure.class), 8.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>11", Departure.class), 8.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>12", Departure.class), 8.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>13", Departure.class), 9.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>14", Departure.class), 9.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b>15", Departure.class), 9.0*3600 + 46.0*60));
		}
		{ // route from right to left
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.links[9].getId(), this.links[17].getId());
			List<Id<Link>> routeLinks = new ArrayList<>();
			Collections.addAll(routeLinks, this.links[10].getId(), this.links[11].getId(), this.links[12].getId(), this.links[13].getId(), this.links[14].getId(), this.links[15].getId(), this.links[16].getId());
			netRoute.setLinkIds(this.links[9].getId(), routeLinks, this.links[17].getId());
			List<TransitRouteStop> stops = new ArrayList<>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[17]).departureOffset(0.0).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[15]).departureOffset(7.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[13], 12.0 * 60, 16.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[11]).departureOffset(23.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 9]).departureOffset(30.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 7]).departureOffset(37.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 5], 42.0 * 60, 46.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[3]).departureOffset(53.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[ 1]).arrivalOffset(58.0 * 60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(Id.create("blue I > A", TransitRoute.class), netRoute, stops, "train");
			this.blueLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(Id.create("b<01", Departure.class), 5.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<02", Departure.class), 5.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<03", Departure.class), 5.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<04", Departure.class), 6.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<05", Departure.class), 6.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<06", Departure.class), 6.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<07", Departure.class), 7.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<08", Departure.class), 7.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<09", Departure.class), 7.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<10", Departure.class), 8.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<11", Departure.class), 8.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<12", Departure.class), 8.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<13", Departure.class), 9.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<14", Departure.class), 9.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("b<15", Departure.class), 9.0*3600 + 56.0*60));
		}
	}

	protected void buildGreenLine() {
		this.greenLine = this.builder.createTransitLine(Id.create("green", TransitLine.class));
		this.schedule.addTransitLine(this.greenLine);
		{ // route in circle in clockwise
			NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.links[25].getId(), this.links[25].getId());
			List<Id<Link>> routeLinks = new ArrayList<>();
			Collections.addAll(routeLinks, this.links[20].getId(), this.links[21].getId(), this.links[22].getId(), this.links[23].getId(), this.links[24].getId());
			netRoute.setLinkIds(this.links[25].getId(), routeLinks, this.links[25].getId());
			List<TransitRouteStop> stops = new ArrayList<>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[18]).departureOffset(0.0).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[19]).departureOffset(10.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[20]).departureOffset(20.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[21]).departureOffset(30.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[22]).departureOffset(40.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[23]).departureOffset(50.0*60).build();
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStopBuilder(this.stopFacilities[18]).arrivalOffset(59.0*60).build();
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(Id.create("green clockwise", TransitRoute.class), netRoute, stops, "train");
			this.greenLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(Id.create("g>01", Departure.class), 5.0*3600 + 01.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>02", Departure.class), 5.0*3600 + 11.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>03", Departure.class), 5.0*3600 + 21.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>04", Departure.class), 5.0*3600 + 31.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>05", Departure.class), 5.0*3600 + 41.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>06", Departure.class), 5.0*3600 + 51.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>07", Departure.class), 6.0*3600 +  1.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>08", Departure.class), 6.0*3600 + 11.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>09", Departure.class), 6.0*3600 + 21.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>10", Departure.class), 6.0*3600 + 31.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>11", Departure.class), 6.0*3600 + 41.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>12", Departure.class), 6.0*3600 + 51.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>13", Departure.class), 7.0*3600 +  1.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>14", Departure.class), 7.0*3600 + 11.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>15", Departure.class), 7.0*3600 + 21.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>16", Departure.class), 7.0*3600 + 31.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>17", Departure.class), 7.0*3600 + 41.0*60));
			route.addDeparture(this.builder.createDeparture(Id.create("g>18", Departure.class), 7.0*3600 + 51.0*60));
		}
	}
}
