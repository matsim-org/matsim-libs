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
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkLayer;
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
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(network);
		reader.parse(args[0]);
		/*
		 * load events
		 */
		logger.info("Loading events...");
		final TravelTimeCalculator ttCalculator = new TravelTimeCalculator(network, binSize, maxTime, new TravelTimeCalculatorConfigGroup());
		EventsImpl events = new EventsImpl();
		events.addHandler(ttCalculator);
		EventsReaderTXTv1 eReader = new EventsReaderTXTv1(events);
		eReader.readFile(args[1]);
		/*
		 * load zones
		 */
		ZoneLayer zoneLayer = ZoneLayer.createFromShapeFile(args[2]);
		Queue<Zone> zones = new ConcurrentLinkedQueue<Zone>(zoneLayer.getZones());
		/*
		 * fill travel time matrix
		 */
		TravelTimeMatrix matrix = new TravelTimeMatrix(new HashSet<Zone>(zoneLayer.getZones()));
		logger.info("Calculating fastest paths...");
		
		int threads = Runtime.getRuntime().availableProcessors();
		RunThread[] runThreads = new RunThread[threads];
		for(int i = 0; i < threads; i++) {
			runThreads[i] = new RunThread(network, zones, matrix, ttCalculator);
			runThreads[i].start();
		}
		for(int i = 0; i < threads; i++) {
			runThreads[i].join();
		}
		
		TravelTimeMatrix.toFile(matrix, args[3]);
	}
	
	private static class RunThread extends Thread {
		
		private static final Logger logger = Logger.getLogger(RunThread.class);
		
		private NetworkLayer network;
		
		private Queue<Zone> zones;
		
		private TravelTimeMatrix matrix;
		
		private Dijkstra router;
		
		private static int n = 0;
		
		private static int total;
		
		public RunThread(NetworkLayer network, Queue<Zone> zones, TravelTimeMatrix matrix, final TravelTimeCalculator ttCalculator) {
			total = zones.size() * zones.size();
			this.network = network;
			this.zones = zones;
			this.matrix = matrix;
			
			router = new Dijkstra(network, new TravelCost() {
				public double getLinkTravelCost(Link link, double time) {
					return ttCalculator.getLinkTravelTime(link, time);
				}
			}, ttCalculator);
		}
		
		public void run() {
			Zone z_i = null;
			while((z_i = zones.poll()) != null) {
//			for(Zone z_i : matrix.getZones()) {
				Point p_i = z_i.getBorder().getCentroid();
				Node l_i = network.getNearestNode(new CoordImpl(p_i.getX(), p_i.getY()));
				for(Zone z_j : matrix.getZones()) {
					Point p_j = z_j.getBorder().getCentroid();
					Node l_j = network.getNearestNode(new CoordImpl(p_j.getX(), p_j.getY()));
					
					double tt = router.calcLeastCostPath(l_i, l_j, 6*60*60).travelTime;
					matrix.setTravelTime(z_i, z_j, tt);
					n++;
					if(n % 1000 == 0)
						logger.info(String.format("%1$s of %2$s done (%3$.2f).", n, total, n/(float)total));
				}
			}
		}
	}

}
