/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingRouterDemo.java
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

package playground.wrashid.parkingSearch.withinday;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.vehicles.Vehicle;

public class ParkingRouterDemo {

	private static final Logger log = Logger.getLogger(ParkingRouterDemo.class);
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		calcMultiNodeRoute(scenario);
	}
	
	private static void calcMultiNodeRoute(Scenario scenario) {
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TransitTravelDisutility travelDisutility = new TravelDisutilityImpl(travelTime);
		
		ParkingRouter parkingRouter = new ParkingRouter(scenario.getNetwork(), travelTime, travelDisutility, 3);
		
		NetworkRoute route = new LinkNetworkRouteImpl(Id.create("l0", Link.class), Id.create("l4", Link.class));
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		routeLinkIds.add(Id.create("l1", Link.class));
		routeLinkIds.add(Id.create("l2", Link.class));
		routeLinkIds.add(Id.create("l3", Link.class));
		route.setLinkIds(Id.create("l0", Link.class), routeLinkIds, Id.create("l4", Link.class));
		
		Link startLink = scenario.getNetwork().getLinks().get(Id.create("l5", Link.class));
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		CustomDataManager dataManager = null;
		
		parkingRouter.adaptRoute(route, startLink, time, person, vehicle, dataManager);
		
		log.info(route.getStartLinkId());
		for(Id<Link> linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
	}
	
	private static void createNetwork(Scenario scenario) {
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();

		Node n0 = networkFactory.createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node n1 = networkFactory.createNode(Id.create("n1", Node.class), new Coord(1000.0, 0.0));
		Node n2 = networkFactory.createNode(Id.create("n2", Node.class), new Coord(2000.0, 0.0));
		Node n3 = networkFactory.createNode(Id.create("n3", Node.class), new Coord(3000.0, 0.0));
		Node n4 = networkFactory.createNode(Id.create("n4", Node.class), new Coord(4000.0, 0.0));
		Node n5 = networkFactory.createNode(Id.create("n5", Node.class), new Coord(5000.0, 0.0));
		double y1 = -1000.0;
		Node n6 = networkFactory.createNode(Id.create("n6", Node.class), new Coord(2000.0, y1));
		double y = -2000.0;
		Node n7 = networkFactory.createNode(Id.create("n7", Node.class), new Coord(2000.0, y));
		
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		scenario.getNetwork().addNode(n6);
		scenario.getNetwork().addNode(n7);

		Link l0 = networkFactory.createLink(Id.create("l0", Link.class), n0, n1);
		l0.setLength(1000.0);
		l0.setFreespeed(10.0);
		
		Link l1 = networkFactory.createLink(Id.create("l1", Link.class), n1, n2);
		l1.setLength(1000.0);
		l1.setFreespeed(10.0);
		
		Link l2 = networkFactory.createLink(Id.create("l2", Link.class), n2, n3);
		l2.setLength(1000.0);
		l2.setFreespeed(10.0);
		
		Link l3 = networkFactory.createLink(Id.create("l3", Link.class), n3, n4);
		l3.setLength(1000.0);
		l3.setFreespeed(10.0);

		Link l4 = networkFactory.createLink(Id.create("l4", Link.class), n4, n5);
		l4.setLength(1000.0);
		l4.setFreespeed(10.0);

		Link l5 = networkFactory.createLink(Id.create("l5", Link.class), n7, n6);
		l5.setLength(1000.0);
		l5.setFreespeed(10.0);
		
		Link l6 = networkFactory.createLink(Id.create("l6", Link.class), n6, n1);
		l6.setLength(1415.0);
		l6.setFreespeed(10.0);

		Link l7 = networkFactory.createLink(Id.create("l7", Link.class), n6, n2);
		l7.setLength(1000.0);
		l7.setFreespeed(10.0);

		Link l8 = networkFactory.createLink(Id.create("l8", Link.class), n6, n3);
		l8.setLength(1415.0);
		l8.setFreespeed(10.0);

		Link l9 = networkFactory.createLink(Id.create("l9", Link.class), n6, n4);
		l9.setLength(2237.0);
		l9.setFreespeed(10.0);

		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
		scenario.getNetwork().addLink(l5);
		scenario.getNetwork().addLink(l6);
		scenario.getNetwork().addLink(l7);
		scenario.getNetwork().addLink(l8);
		scenario.getNetwork().addLink(l9);
	}
	
	private static class TravelDisutilityImpl implements TransitTravelDisutility, TravelDisutility {

		private final TravelTime travelTime;
		
		public TravelDisutilityImpl(TravelTime travelTime) {
			this.travelTime = travelTime;
		}
		
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle, CustomDataManager dataManager) {
			return getLinkTravelDisutility(link, time, person, vehicle);
		}

		@Override
		public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
			return 0.0;
		}

		@Override
		public double getTravelTime(Person person, Coord coord, Coord toCoord) {
			return 0.0;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return link.getLength() / link.getFreespeed();
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return this.travelTime.getLinkTravelTime(link, time, person, vehicle);
		}
		
	}
}