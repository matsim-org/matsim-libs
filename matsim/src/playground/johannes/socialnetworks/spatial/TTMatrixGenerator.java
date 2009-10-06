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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
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
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
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
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		Logger logger = Logger.getLogger(TTMatrixGenerator.class);
		
		double minx = 5.8;
		double miny = 45.7;
		double maxx = 10.6;
		double maxy = 47.9;
		
		CoordinateTransformation transform = new WGS84toCH1903LV03();
		Coord lowerleft = transform.transform(new CoordImpl(minx, miny));
		Coord upperright = transform.transform(new CoordImpl(maxx, maxy));
		
		int binSize = 60*60*24; // one day
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
		 * init dijkstra
		 */
		Dijkstra router = new Dijkstra(network, new TravelCost() {
			
			public double getLinkTravelCost(Link link, double time) {
				return ttCalculator.getLinkTravelTime(link, time);
			}
		}, ttCalculator);
		/*
		 * load zones
		 */
		ZoneLayer zoneLayer = ZoneLayer.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		/*
		 * fill travel time matrix
		 */
		TravelTimeMatrix matrix = new TravelTimeMatrix(new HashSet<Zone>(zoneLayer.getZones()));
		logger.info("Calculating fastest paths...");
		
		
		int n = 0;
		int total = zoneLayer.getZones().size();
		for(Zone z_i : matrix.getZones()) {
			Point p_i = z_i.getBorder().getCentroid();
			Node l_i = network.getNearestNode(new CoordImpl(p_i.getX(), p_i.getY()));
			for(Zone z_j : matrix.getZones()) {
//				if(z_i != z_j) {
				Point p_j = z_j.getBorder().getCentroid();
				Node l_j = network.getNearestNode(new CoordImpl(p_j.getX(), p_j.getY()));
				
				double tt = router.calcLeastCostPath(l_i, l_j, 6*60*60).travelTime;
				matrix.setTravelTime(z_i, z_j, tt);
				n++;
				if(n % 1000 == 0)
					logger.info(String.format("%1$s of %2$s done (%3$.2f).", n, total, n/(float)total));
//				}
			}
		}
		
		TravelTimeMatrix.toFile(matrix, "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/ttmatrix.txt");
	}

}
