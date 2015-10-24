/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOverDist.java
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
package playground.johannes.studies.coopsim;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleDoubleHashMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;
import playground.johannes.coopsim.util.MatsimCoordUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author illenberger
 * 
 */
public class TimeOverDist {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader creader = new ConfigReader(config);
		creader.readFile("/Users/jillenberger/Work/socialnets/locationChoice/config.xml");

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		Network network = scenario.getNetwork();

//		final TravelTime travelTime = new TravelTimeDecorator(new TravelTimeCalculator(network, 900, 86400, new TravelTimeCalculatorConfigGroup()));
		final TravelTime travelTime = new TravelTimeCalculator(network, 900, 86400,	new TravelTimeCalculatorConfigGroup()).getLinkTravelTimes();
		TravelDisutility travelCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return travelTime.getLinkTravelTime(link, time, person, vehicle);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}
		};

		TravelDisutility travelMinCost = new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return travelTime.getLinkTravelTime(link, time, person, vehicle);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return travelTime.getLinkTravelTime(link, 0, null, null);
			}
		};

		// LeastCostPathCalculator router = new Dijkstra(network, travelCost,
		// travelTime);
		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, travelMinCost);
		LeastCostPathCalculator router = factory.createPathCalculator(network, travelCost, travelTime);
		Geometry g = (Geometry) EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next().getDefaultGeometry();

		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		DistanceCalculator calc = new CartesianDistanceCalculator();
		List<Node> nodes = new ArrayList<Node>(network.getNodes().values());
		Random rnd = new Random();
		for (int i = 0; i < 5000; i++) {
			Node node1 = nodes.get(rnd.nextInt(nodes.size()));
			Node node2 = nodes.get(rnd.nextInt(nodes.size()));

			if (node1 != node2) {
				Point p1 = MatsimCoordUtils.coordToPoint(node1.getCoord());
				Point p2 = MatsimCoordUtils.coordToPoint(node2.getCoord());
				if (g.contains(p1) && g.contains(p2)) {
					Path path = router.calcLeastCostPath(node1, node2, 0, null, null);
					double tt = 0;
					double dist = calc.distance(p1, p2);
					for (Link link : path.links) {
						tt += travelTime.getLinkTravelTime(link, 0, null, null);
						tt+=10;
					}

					tt = tt * (1 + (3 * Math.exp(-dist/10000)));
					hist.put(dist, tt);
				}
			}

			if (i % 100 == 0)
				System.out.println("Calulated " + i + " routes.");
		}

		StatsWriter.writeHistogram(hist, "d", "t", "/Users/jillenberger/Work/socialnets/locationChoice/analysis/tt-vs-d.5.txt");
	}

}
