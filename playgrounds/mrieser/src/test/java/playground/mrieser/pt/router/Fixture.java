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

package playground.mrieser.pt.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

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

	/*package*/ final ScenarioImpl scenario;
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
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
		this.config.scenario().setUseTransit(true);
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
		this.nodes[0]  = this.network.getFactory().createNode(this.scenario.createId("0"),  this.scenario.createCoord(   0, 5000));
		this.nodes[1]  = this.network.getFactory().createNode(this.scenario.createId("1"),  this.scenario.createCoord( 4000, 5000));
		this.nodes[2]  = this.network.getFactory().createNode(this.scenario.createId("2"),  this.scenario.createCoord( 8000, 5000));
		this.nodes[3]  = this.network.getFactory().createNode(this.scenario.createId("3"),  this.scenario.createCoord(12000, 5000));
		this.nodes[4]  = this.network.getFactory().createNode(this.scenario.createId("4"),  this.scenario.createCoord(16000, 5000));
		this.nodes[5]  = this.network.getFactory().createNode(this.scenario.createId("5"),  this.scenario.createCoord(20000, 5000));
		this.nodes[6]  = this.network.getFactory().createNode(this.scenario.createId("6"),  this.scenario.createCoord(24000, 5000));
		this.nodes[7]  = this.network.getFactory().createNode(this.scenario.createId("7"),  this.scenario.createCoord(28000, 5000));
		this.nodes[8]  = this.network.getFactory().createNode(this.scenario.createId("8"),  this.scenario.createCoord(32000, 5000));
		this.nodes[9]  = this.network.getFactory().createNode(this.scenario.createId("9"),  this.scenario.createCoord(36000, 5000));
		this.nodes[10] = this.network.getFactory().createNode(this.scenario.createId("10"), this.scenario.createCoord(40000, 5000));
		this.nodes[11] = this.network.getFactory().createNode(this.scenario.createId("11"), this.scenario.createCoord(12000, 5000));
		this.nodes[12] = this.network.getFactory().createNode(this.scenario.createId("12"), this.scenario.createCoord(16000, 10000));
		this.nodes[13] = this.network.getFactory().createNode(this.scenario.createId("13"), this.scenario.createCoord(24000, 10000));
		this.nodes[14] = this.network.getFactory().createNode(this.scenario.createId("14"), this.scenario.createCoord(28000, 5000));
		this.nodes[15] = this.network.getFactory().createNode(this.scenario.createId("15"), this.scenario.createCoord(24000, 0));
		this.nodes[16] = this.network.getFactory().createNode(this.scenario.createId("16"), this.scenario.createCoord(16000, 0));
		for (int i = 0; i < 17; i++) {
			this.network.addNode(this.nodes[i]);
		}
		this.links[0]  = this.network.getFactory().createLink(this.scenario.createId( "0"), this.nodes[ 0].getId(), this.nodes[ 1].getId());
		this.links[1]  = this.network.getFactory().createLink(this.scenario.createId( "1"), this.nodes[ 1].getId(), this.nodes[ 2].getId());
		this.links[2]  = this.network.getFactory().createLink(this.scenario.createId( "2"), this.nodes[ 2].getId(), this.nodes[ 3].getId());
		this.links[3]  = this.network.getFactory().createLink(this.scenario.createId( "3"), this.nodes[ 3].getId(), this.nodes[ 4].getId());
		this.links[4]  = this.network.getFactory().createLink(this.scenario.createId( "4"), this.nodes[ 4].getId(), this.nodes[ 5].getId());
		this.links[5]  = this.network.getFactory().createLink(this.scenario.createId( "5"), this.nodes[ 5].getId(), this.nodes[ 6].getId());
		this.links[6]  = this.network.getFactory().createLink(this.scenario.createId( "6"), this.nodes[ 6].getId(), this.nodes[ 7].getId());
		this.links[7]  = this.network.getFactory().createLink(this.scenario.createId( "7"), this.nodes[ 7].getId(), this.nodes[ 8].getId());
		this.links[8]  = this.network.getFactory().createLink(this.scenario.createId( "8"), this.nodes[ 8].getId(), this.nodes[ 9].getId());
		this.links[9]  = this.network.getFactory().createLink(this.scenario.createId( "9"), this.nodes[10].getId(), this.nodes[ 9].getId());
		this.links[10] = this.network.getFactory().createLink(this.scenario.createId("10"), this.nodes[ 9].getId(), this.nodes[ 8].getId());
		this.links[11] = this.network.getFactory().createLink(this.scenario.createId("11"), this.nodes[ 8].getId(), this.nodes[ 7].getId());
		this.links[12] = this.network.getFactory().createLink(this.scenario.createId("12"), this.nodes[ 7].getId(), this.nodes[ 6].getId());
		this.links[13] = this.network.getFactory().createLink(this.scenario.createId("13"), this.nodes[ 6].getId(), this.nodes[ 5].getId());
		this.links[14] = this.network.getFactory().createLink(this.scenario.createId("14"), this.nodes[ 5].getId(), this.nodes[ 4].getId());
		this.links[15] = this.network.getFactory().createLink(this.scenario.createId("15"), this.nodes[ 4].getId(), this.nodes[ 3].getId());
		this.links[16] = this.network.getFactory().createLink(this.scenario.createId("16"), this.nodes[ 3].getId(), this.nodes[ 2].getId());
		this.links[17] = this.network.getFactory().createLink(this.scenario.createId("17"), this.nodes[ 2].getId(), this.nodes[ 1].getId());
		for (int i = 0; i < 18; i++) {
			this.links[i].setLength(5000.0);
			this.links[i].setFreespeed(44.44);
			this.links[i].setCapacity(2000.0);
			this.links[i].setNumberOfLanes(1.0);
			this.network.addLink(this.links[i]);
		}
		this.links[18] = null;
		this.links[19] = null;
		this.links[20] = this.network.getFactory().createLink(this.scenario.createId("20"), this.nodes[11].getId(), this.nodes[12].getId());
		this.links[21] = this.network.getFactory().createLink(this.scenario.createId("21"), this.nodes[12].getId(), this.nodes[13].getId());
		this.links[22] = this.network.getFactory().createLink(this.scenario.createId("22"), this.nodes[13].getId(), this.nodes[14].getId());
		this.links[23] = this.network.getFactory().createLink(this.scenario.createId("23"), this.nodes[14].getId(), this.nodes[15].getId());
		this.links[24] = this.network.getFactory().createLink(this.scenario.createId("24"), this.nodes[15].getId(), this.nodes[16].getId());
		this.links[25] = this.network.getFactory().createLink(this.scenario.createId("25"), this.nodes[16].getId(), this.nodes[11].getId());
		for (int i = 20; i < 26; i++) {
			this.links[i].setLength(10000.0);
			this.links[i].setFreespeed(20.0);
			this.links[i].setCapacity(2000.0);
			this.links[i].setNumberOfLanes(1.0);
			this.network.addLink(this.links[i]);
		}
	}

	protected void buildStops() {
		this.stopFacilities[ 0] = this.builder.createTransitStopFacility(this.scenario.createId( "0"), this.scenario.createCoord( 4000,  5002), true);
		this.stopFacilities[ 1] = this.builder.createTransitStopFacility(this.scenario.createId( "1"), this.scenario.createCoord( 4000,  4998), true);
		this.stopFacilities[ 2] = this.builder.createTransitStopFacility(this.scenario.createId( "2"), this.scenario.createCoord( 8000,  5002), true);
		this.stopFacilities[ 3] = this.builder.createTransitStopFacility(this.scenario.createId( "3"), this.scenario.createCoord( 8000,  4998), true);
		this.stopFacilities[ 4] = this.builder.createTransitStopFacility(this.scenario.createId( "4"), this.scenario.createCoord(12000,  5002), true);
		this.stopFacilities[ 5] = this.builder.createTransitStopFacility(this.scenario.createId( "5"), this.scenario.createCoord(12000,  4998), true);
		this.stopFacilities[ 6] = this.builder.createTransitStopFacility(this.scenario.createId( "6"), this.scenario.createCoord(16000,  5002), true);
		this.stopFacilities[ 7] = this.builder.createTransitStopFacility(this.scenario.createId( "7"), this.scenario.createCoord(16000,  4998), true);
		this.stopFacilities[ 8] = this.builder.createTransitStopFacility(this.scenario.createId( "8"), this.scenario.createCoord(20000,  5002), true);
		this.stopFacilities[ 9] = this.builder.createTransitStopFacility(this.scenario.createId( "9"), this.scenario.createCoord(20000,  4998), true);
		this.stopFacilities[10] = this.builder.createTransitStopFacility(this.scenario.createId("10"), this.scenario.createCoord(24000,  5002), true);
		this.stopFacilities[11] = this.builder.createTransitStopFacility(this.scenario.createId("11"), this.scenario.createCoord(24000,  4998), true);
		this.stopFacilities[12] = this.builder.createTransitStopFacility(this.scenario.createId("12"), this.scenario.createCoord(28000,  5002), true);
		this.stopFacilities[13] = this.builder.createTransitStopFacility(this.scenario.createId("13"), this.scenario.createCoord(28000,  4998), true);
		this.stopFacilities[14] = this.builder.createTransitStopFacility(this.scenario.createId("14"), this.scenario.createCoord(32000,  5002), true);
		this.stopFacilities[15] = this.builder.createTransitStopFacility(this.scenario.createId("15"), this.scenario.createCoord(32000,  4998), true);
		this.stopFacilities[16] = this.builder.createTransitStopFacility(this.scenario.createId("16"), this.scenario.createCoord(36000,  5002), true);
		this.stopFacilities[17] = this.builder.createTransitStopFacility(this.scenario.createId("17"), this.scenario.createCoord(36000,  4998), true);
		this.stopFacilities[18] = this.builder.createTransitStopFacility(this.scenario.createId("18"), this.scenario.createCoord(12000,  5000), true);
		this.stopFacilities[19] = this.builder.createTransitStopFacility(this.scenario.createId("19"), this.scenario.createCoord(16000, 10000), true);
		this.stopFacilities[20] = this.builder.createTransitStopFacility(this.scenario.createId("20"), this.scenario.createCoord(24000, 10000), true);
		this.stopFacilities[21] = this.builder.createTransitStopFacility(this.scenario.createId("21"), this.scenario.createCoord(28000,  5000), true);
		this.stopFacilities[22] = this.builder.createTransitStopFacility(this.scenario.createId("22"), this.scenario.createCoord(24000,     0), true);
		this.stopFacilities[23] = this.builder.createTransitStopFacility(this.scenario.createId("23"), this.scenario.createCoord(16000,     0), true);
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
		this.stopFacilities[ 0].setLink(this.links[ 0]);
		this.stopFacilities[ 1].setLink(this.links[17]);
		this.stopFacilities[ 2].setLink(this.links[ 1]);
		this.stopFacilities[ 3].setLink(this.links[16]);
		this.stopFacilities[ 4].setLink(this.links[ 2]);
		this.stopFacilities[ 5].setLink(this.links[15]);
		this.stopFacilities[ 6].setLink(this.links[ 3]);
		this.stopFacilities[ 7].setLink(this.links[14]);
		this.stopFacilities[ 8].setLink(this.links[ 4]);
		this.stopFacilities[ 9].setLink(this.links[13]);
		this.stopFacilities[10].setLink(this.links[ 5]);
		this.stopFacilities[11].setLink(this.links[12]);
		this.stopFacilities[12].setLink(this.links[ 6]);
		this.stopFacilities[13].setLink(this.links[11]);
		this.stopFacilities[14].setLink(this.links[ 7]);
		this.stopFacilities[15].setLink(this.links[10]);
		this.stopFacilities[16].setLink(this.links[ 8]);
		this.stopFacilities[17].setLink(this.links[ 9]);
		this.stopFacilities[18].setLink(this.links[25]);
		this.stopFacilities[19].setLink(this.links[20]);
		this.stopFacilities[20].setLink(this.links[21]);
		this.stopFacilities[21].setLink(this.links[22]);
		this.stopFacilities[22].setLink(this.links[23]);
		this.stopFacilities[23].setLink(this.links[24]);
		for (TransitStopFacility stopFacility : this.stopFacilities) {
			this.schedule.addStopFacility(stopFacility);
		}
	}

	protected void buildRedLine() {
		this.redLine = this.builder.createTransitLine(this.scenario.createId("red"));
		this.schedule.addTransitLine(this.redLine);
		{ // route from left to right
			NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(this.links[2], this.links[6]);
			List<Link> routeLinks = new ArrayList<Link>();
			Collections.addAll(routeLinks, this.links[3], this.links[4], this.links[5]);
			netRoute.setLinks(this.links[2], routeLinks, this.links[6]);
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			stops.add(this.builder.createTransitRouteStop(this.stopFacilities[4], Time.UNDEFINED_TIME, 0.0));
			stops.add(this.builder.createTransitRouteStop(this.stopFacilities[12], Time.UNDEFINED_TIME, 9.0*60));
			TransitRoute route = this.builder.createTransitRoute(this.scenario.createId("red C > G"), netRoute, stops, TransportMode.train);
			this.redLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r>01"), 6.0*3600));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r>02"), 7.0*3600));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r>03"), 8.0*3600));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r>04"), 9.0*3600));
		}
		{ // route from right to left
			NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(this.links[11], this.links[15]);
			List<Link> routeLinks = new ArrayList<Link>();
			Collections.addAll(routeLinks, this.links[12], this.links[13], this.links[14]);
			netRoute.setLinks(this.links[11], routeLinks, this.links[15]);
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			stops.add(this.builder.createTransitRouteStop(this.stopFacilities[13], Time.UNDEFINED_TIME, 0.0));
			stops.add(this.builder.createTransitRouteStop(this.stopFacilities[5], Time.UNDEFINED_TIME, 9.0*60));
			TransitRoute route = this.builder.createTransitRoute(this.scenario.createId("red G > C"), netRoute, stops, TransportMode.train);
			this.redLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r<01"), 6.0*3600 + 10.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r<02"), 7.0*3600 + 10.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r<03"), 8.0*3600 + 10.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("r<04"), 9.0*3600 + 10.0*60));
		}
	}

	protected void buildBlueLine() {
		this.blueLine = this.builder.createTransitLine(this.scenario.createId("blue"));
		this.schedule.addTransitLine(this.blueLine);
		{ // route from left to right
			NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(this.links[0], this.links[8]);
			List<Link> routeLinks = new ArrayList<Link>();
			Collections.addAll(routeLinks, this.links[1], this.links[2], this.links[3], this.links[4], this.links[5], this.links[6], this.links[7]);
			netRoute.setLinks(this.links[0], routeLinks, this.links[8]);
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 0], Time.UNDEFINED_TIME, 0.0);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 2], Time.UNDEFINED_TIME, 7.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 4], 12.0 * 60, 16.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 6], Time.UNDEFINED_TIME, 23.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 8], Time.UNDEFINED_TIME, 30.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[10], Time.UNDEFINED_TIME, 37.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[12], 42.0 * 60, 46.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[14], Time.UNDEFINED_TIME, 53.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[16], 58.0 * 60, Time.UNDEFINED_TIME);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(this.scenario.createId("blue A > I"), netRoute, stops, TransportMode.train);
			this.blueLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>01"), 5.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>02"), 5.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>03"), 5.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>04"), 6.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>05"), 6.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>06"), 6.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>07"), 7.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>08"), 7.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>09"), 7.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>10"), 8.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>11"), 8.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>12"), 8.0*3600 + 46.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>13"), 9.0*3600 +  6.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>14"), 9.0*3600 + 26.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b>15"), 9.0*3600 + 46.0*60));
		}
		{ // route from right to left
			NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(this.links[9], this.links[17]);
			List<Link> routeLinks = new ArrayList<Link>();
			Collections.addAll(routeLinks, this.links[10], this.links[11], this.links[12], this.links[13], this.links[14], this.links[15], this.links[16]);
			netRoute.setLinks(this.links[9], routeLinks, this.links[17]);
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStop(this.stopFacilities[17], Time.UNDEFINED_TIME, 0.0);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[15], Time.UNDEFINED_TIME, 7.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[13], 12.0 * 60, 16.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[11], Time.UNDEFINED_TIME, 23.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 9], Time.UNDEFINED_TIME, 30.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 7], Time.UNDEFINED_TIME, 37.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 5], 42.0 * 60, 46.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[3], Time.UNDEFINED_TIME, 53.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[ 1], 58.0 * 60, Time.UNDEFINED_TIME);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(this.scenario.createId("blue I > A"), netRoute, stops, TransportMode.train);
			this.blueLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<01"), 5.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<02"), 5.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<03"), 5.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<04"), 6.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<05"), 6.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<06"), 6.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<07"), 7.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<08"), 7.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<09"), 7.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<10"), 8.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<11"), 8.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<12"), 8.0*3600 + 56.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<13"), 9.0*3600 + 16.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<14"), 9.0*3600 + 36.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("b<15"), 9.0*3600 + 56.0*60));
		}
	}

	protected void buildGreenLine() {
		this.greenLine = this.builder.createTransitLine(this.scenario.createId("green"));
		this.schedule.addTransitLine(this.greenLine);
		{ // route in circle in clockwise
			NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(this.links[25], this.links[25]);
			List<Link> routeLinks = new ArrayList<Link>();
			Collections.addAll(routeLinks, this.links[20], this.links[21], this.links[22], this.links[23], this.links[24]);
			netRoute.setLinks(this.links[25], routeLinks, this.links[25]);
			List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			TransitRouteStop stop;
			stop = this.builder.createTransitRouteStop(this.stopFacilities[18], Time.UNDEFINED_TIME, 0.0);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[19], Time.UNDEFINED_TIME, 10.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[20], Time.UNDEFINED_TIME, 20.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[21], Time.UNDEFINED_TIME, 30.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[22], Time.UNDEFINED_TIME, 40.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[23], Time.UNDEFINED_TIME, 50.0*60);
			stop.setAwaitDepartureTime(true);
			stops.add(stop);
			stop = this.builder.createTransitRouteStop(this.stopFacilities[18], 59.0*60, Time.UNDEFINED_TIME);
			stops.add(stop);
			TransitRoute route = this.builder.createTransitRoute(this.scenario.createId("green clockwise"), netRoute, stops, TransportMode.train);
			this.greenLine.addRoute(route);

			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>01"), 5.0*3600 + 01.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>02"), 5.0*3600 + 11.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>03"), 5.0*3600 + 21.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>04"), 5.0*3600 + 31.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>05"), 5.0*3600 + 41.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>06"), 5.0*3600 + 51.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>07"), 6.0*3600 +  1.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>08"), 6.0*3600 + 11.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>09"), 6.0*3600 + 21.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>10"), 6.0*3600 + 31.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>11"), 6.0*3600 + 41.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>12"), 6.0*3600 + 51.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>13"), 7.0*3600 +  1.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>14"), 7.0*3600 + 11.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>15"), 7.0*3600 + 21.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>16"), 7.0*3600 + 31.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>17"), 7.0*3600 + 41.0*60));
			route.addDeparture(this.builder.createDeparture(this.scenario.createId("g>18"), 7.0*3600 + 51.0*60));
		}
	}
}
