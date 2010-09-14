/* *********************************************************************** *
 * project: org.matsim.*
 * TTMatrixGenerator.java
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
package playground.johannes.socialnetworks.spatial;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class TTMatrixGenerator {

	private static final double startTime = 6*60*60;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		Logger logger = Logger.getLogger(TTMatrixGenerator.class);
		
//		double minx = 5.8;
//		double miny = 45.7;
//		double maxx = 10.6;
//		double maxy = 47.9;
//		
//		CoordinateTransformation transform = new WGS84toCH1903LV03();
//		Coord lowerleft = transform.transform(new CoordImpl(minx, miny));
//		Coord upperright = transform.transform(new CoordImpl(maxx, maxy));
		
		int binSize = 900;
		int maxTime = 60*60*24;
		/*
		 * read network
		 */
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario);
		reader.parse(args[0]);
		/*
		 * load events
		 */
		logger.info("Loading events...");
		final TravelTimeCalculator ttCalculator = new TravelTimeCalculator(network, binSize, maxTime, new TravelTimeCalculatorConfigGroup());
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(ttCalculator);
		EventsReaderTXTv1 eReader = new EventsReaderTXTv1(events);
		eReader.readFile(args[1]);
		/*
		 * load zones
		 */
		ZoneLayerLegacy zoneLayer = ZoneLayerLegacy.createFromShapeFile(args[2]);
		Queue<ZoneLegacy> zones = new ConcurrentLinkedQueue<ZoneLegacy>(zoneLayer.getZones());
		/*
		 * fill travel time matrix
		 */
		TravelTimeMatrix matrix = new TravelTimeMatrix(new HashSet<ZoneLegacy>(zoneLayer.getZones()));
		logger.info("Calculating fastest paths...");
		
		int threads = Runtime.getRuntime().availableProcessors();
		RunThread[] runThreads = new RunThread[threads];
		for(int i = 0; i < threads; i++) {
			runThreads[i] = new RunThread(network, zones, zoneLayer, matrix, ttCalculator);
			runThreads[i].start();
		}
		for(int i = 0; i < threads; i++) {
			runThreads[i].join();
		}
		
		TravelTimeMatrix.toFile(matrix, args[3]);
	}
	
	private static class RunThread extends Thread {
		
		private static final Logger logger = Logger.getLogger(RunThread.class);
		
		private NetworkImpl network;
		
		private Queue<ZoneLegacy> zones;
		
		private TravelTimeMatrix matrix;
		
		private Dijkstra router;
		
		private static int n = 0;
		
		private static int total;
		
		private Map<ZoneLegacy, Set<Node>> nodeMapping;
		
		public RunThread(NetworkImpl network, Queue<ZoneLegacy> zones, ZoneLayerLegacy zoneLayer, TravelTimeMatrix matrix, final TravelTimeCalculator ttCalculator) {
			total = zones.size() * zones.size();
			this.network = network;
			this.zones = zones;
			this.matrix = matrix;
			
			router = new Dijkstra(network, new TravelCost() {
				public double getLinkTravelCost(Link link, double time) {
					return ttCalculator.getLinkTravelTime(link, time);
				}
			}, ttCalculator);
		
			nodeMapping = new HashMap<ZoneLegacy, Set<Node>>();
			for(Node node : network.getNodes().values()) {
				ZoneLegacy zone = zoneLayer.getZone(node.getCoord());
				if(zone != null) {
					Set<Node> nodes = nodeMapping.get(zone);
					if (nodes == null) {
						nodes = new HashSet<Node>();
						nodeMapping.put(zone, nodes);
					}
					nodes.add(node);
				} else {
					logger.warn(String.format("Node %1$s is not located in a zone.", node.getId().toString()));
				}
			}
		}
		
		@Override
		public void run() {
			ZoneLegacy z_i = null;
			while((z_i = zones.poll()) != null) {
//			for(Zone z_i : matrix.getZones()) {
				Point p_i = z_i.getBorder().getCentroid();
				Node l_i = network.getNearestNode(new CoordImpl(p_i.getX(), p_i.getY()));
				for(ZoneLegacy z_j : matrix.getZones()) {
					if(z_i == z_j) {
						double ttSum = 0;
						int i = 0;
						int count = 10;
						Set<Node> nodes = nodeMapping.get(z_i);
						if(nodes == null) {
							matrix.setTravelTime(z_i, z_j, 0.0);
						} else {
						logger.info(String.format("Number of nodes in zone = %1$s", nodes.size()));
						for(Iterator<Node> it = nodes.iterator(); it.hasNext();) {
							Node o = it.next();
							if(it.hasNext()) {
								Node d = it.next();
								ttSum += router.calcLeastCostPath(o, d, startTime).travelTime;
								i++;
								if(i > count)
									break;
							}
						}
						double tt = ttSum/i;
						if(Double.isInfinite(tt)) {
							tt = 0.0;
							logger.warn("Travel time was infinity, set to zero.");
						} else if(Double.isNaN(tt)) {
							tt = 0.0;
							logger.warn("Travel time was NaN, set to zero.");
						}
						matrix.setTravelTime(z_i, z_j, tt);
						}
					} else {
						double tt = calcInterCellTT(z_j, l_i);
						matrix.setTravelTime(z_i, z_j, tt);
					}
					n++;
					if(n % 1000 == 0)
						logger.info(String.format("%1$s of %2$s done (%3$.2f).", n, total, n/(float)total));
				}
			}
		}
		
		private double calcInterCellTT(ZoneLegacy z_j, Node l_i) {
			Point p_j = z_j.getBorder().getCentroid();
			Node l_j = network.getNearestNode(new CoordImpl(p_j
					.getX(), p_j.getY()));

			return router.calcLeastCostPath(l_i, l_j,
					startTime).travelTime;
		}
	}

	
}
