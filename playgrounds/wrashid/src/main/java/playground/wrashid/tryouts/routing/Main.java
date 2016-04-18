/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.tryouts.routing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;


public class Main {

	public static void main(String[] args) {

		Fixture f = new Fixture();
		Network network = GeneralLib.readNetwork("H:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml");
		//Network network = GeneralLib.readNetwork("H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz");
		
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node node: network.getNodes().values()) {
			if (node.getCoord().getX() < minx) { minx = node.getCoord().getX(); }
			if (node.getCoord().getY() < miny) { miny = node.getCoord().getY(); }
			if (node.getCoord().getX() > maxx) { maxx = node.getCoord().getX(); }
			if (node.getCoord().getY() > maxy) { maxy = node.getCoord().getY(); }
		}
		
		double xFromAct=MatsimRandom.getRandom().nextDouble()*(-minx+maxx)-minx;
		double yFromAct=MatsimRandom.getRandom().nextDouble()*(-miny+maxy)-miny;
		
		double xToAct=MatsimRandom.getRandom().nextDouble()*(-minx+maxx)-minx;
		double yToAct=MatsimRandom.getRandom().nextDouble()*(-miny+maxy)-miny;
		
//		Config config = new Config();
//		config.addCoreModules();
//		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
//		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
//
//		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, timeCostCalc);
		
		RouteFactoryImpl routeFactory = ((PopulationFactoryImpl) f.s.getPopulation().getFactory()).getRouteFactory();
		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(-6.0 / 3600, +6.0 / 3600, 0.0);
		LeastCostPathCalculator routeAlgo = new Dijkstra(network, freespeed, freespeed);

		
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		Leg leg = new LegImpl(TransportMode.car);
		Coord fromCoord = new Coord(xFromAct, yFromAct);
		Activity fromAct = new ActivityImpl("h", fromCoord);
		((ActivityImpl) fromAct).setLinkId(NetworkUtils.getNearestLink(((NetworkImpl) network), fromCoord).getId());
		Coord toCoord = new Coord(xToAct, yToAct);
		Activity toAct = new ActivityImpl("h", toCoord);
		((ActivityImpl) toAct).setLinkId(NetworkUtils.getNearestLink(((NetworkImpl) network), toCoord).getId());

		for (int i = 0; i < 1000; i++) {
			//double tt = new NetworkLegRouter(network, routeAlgo, routeFactory).routeLeg(person, leg, fromAct, toAct,
			//		7.0 * 3600);
			throw new UnsupportedOperationException( "if you need this, please use a RoutingModule instead" );
		}
	}

	private static class Fixture {
		
		
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			Network net = this.s.getNetwork();

		}
	}

}
