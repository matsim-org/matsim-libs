/* *********************************************************************** *
 * project: org.matsim.*
 * AddZCoordinatesToNetwork.java
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

package playground.christoph.evacuation.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.api.core.v01.Coord3d;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;

/**
 * Adds z-coordinates to the ivt-ch-eu network. Coordinates inside Switzerland
 * are taken from the Swiss DHM25, other coordinates are based on SRTM data.
 * 
 * For each link its steepness is checked because there might be a height offset
 * between DHM25 and SRTM data. Therefore, cross-boarder links might be too steep.
 * 
 * @author cdobler
 */
public class AddZCoordinatesToNetwork {

	final private static Logger log = Logger.getLogger(AddZCoordinatesToNetwork.class);

	private final Scenario scenario;
	private final String DHM25;
	private final String SRTM;
	
	/**
	 * 
	 * @param network MATSim network input file
	 * @param DHM25 DHM25 shp file
	 * @param SRTM SRTM shp file
	 * @param network MATSim network output file
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 4) return;
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(args[0]);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		AddZCoordinatesToNetwork adder = new AddZCoordinatesToNetwork(scenario, args[1], args[2]);
		adder.addZCoordinatesToNetwork();
		adder.checkSteepness();
		
		// so far, the network writer does not write z coordinates
		//new NetworkWriter(scenario.getNetwork()).writeV1(args[3]);
	}
	
	/**
	 * @param network MATSim network
	 * @param DHM25 DHM25 shp file
	 * @param SRTM SRTM shp file
	 */
	public AddZCoordinatesToNetwork(Scenario scenario, String DHM25, String SRTM) {
		this.scenario = scenario;
		this.DHM25 = DHM25;
		this.SRTM = SRTM;
	}
	
	public void addZCoordinatesToNetwork() {
		// read DHM25 shp file
		log.info("reading dhm25 data...");
		Map<Id<Node>, Double> dhm25Heights = new HashMap<>();
		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(DHM25)) {
			Id<Node> id = Id.create(String.valueOf(feature.getAttribute(1)), Node.class);
			double z = (Double) feature.getAttribute(4);
			if (z != 0.0) dhm25Heights.put(id, z);
		}
		log.info("done. read " + dhm25Heights.size() + " height coordinates.");
		
		// read DHM25 shp file
		log.info("reading srtm data...");
		Map<Id<Node>, Double> srtmHeights = new HashMap<>();
		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(SRTM)) {
			Id<Node> id = Id.create(String.valueOf(feature.getAttribute(1)), Node.class);
			double z = (Double) feature.getAttribute(4);
			if (z != 0.0) srtmHeights.put(id, z);
		}
		log.info("done. read " + srtmHeights.size() + " height coordinates.");
		
		log.info("adding height data...");
		int dhm25Count = 0;
		int srtmCount = 0;
		int extrapolateCount = 0;
		Network network = scenario.getNetwork();
		
		// Create a quadtree srtm coordinates which is used to extrapolate coordinates
		log.info("\t create quad tree for extrapolation...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node node : network.getNodes().values()) {
			if (node.getCoord().getX() < minx) { minx = node.getCoord().getX(); }
			if (node.getCoord().getY() < miny) { miny = node.getCoord().getY(); }
			if (node.getCoord().getX() > maxx) { maxx = node.getCoord().getX(); }
			if (node.getCoord().getY() > maxy) { maxy = node.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("\t xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		
		QuadTree<Id<Node>> quadTree = new QuadTree<Id<Node>>(minx, miny, maxx, maxy);
		for (Node node : network.getNodes().values()) {
			
			// add all nodes with a height coordinate in the srtm map
			if (srtmHeights.containsKey(node.getId())) {
				quadTree.put(node.getCoord().getX(), node.getCoord().getY(), node.getId());				
			}
		}
		log.info("\t done.");
		
		for (Node node : network.getNodes().values()) {
			
			// use dhm25 height, if available
			Double dhm25 = dhm25Heights.get(node.getId());
			if (dhm25 != null) {
				addZCoord(node, dhm25);
				dhm25Count++;
				continue;
			}
			
			// otherwise use srtm height, if available
			Double srtm = srtmHeights.get(node.getId());
			if (srtm != null) {
				addZCoord(node, srtm);
				srtmCount++;
				continue;
			}
			
			// no height value found, therefore interpolate it...
			Id<Node> neighbourId = quadTree.getClosest(node.getCoord().getX(), node.getCoord().getY());
			addZCoord(node, srtmHeights.get(neighbourId));
			extrapolateCount++;
		}
		log.info("done. got " + dhm25Count + " height values from dhm25 data, " + srtmCount + 
				" height values from srtm data and " + extrapolateCount + " height values via extrapolation.");	
	}
	
	/*
	 * Replaces a Nodes 2d coordinate with a new 3d coordinate. X and Y values
	 * are taken from the 2d coordinate.
	 */
	private void addZCoord(Node node, double z) {
		Coord coord = node.getCoord();
		Coord3d coord3d = new Coord3dImpl(coord.getX(), coord.getY(), z);
		((NodeImpl) node).setCoord(coord3d);
	}
	
	public void checkSteepness() {
		checkSteepness(20);
		checkSteepness(30);
		checkSteepness(40);
		checkSteepness(50);
		checkSteepness(60);
		checkSteepness(70);
		checkSteepness(80);
	}
	
	private void checkSteepness(int value) {
		Counter counter = new Counter("Links with steepness > +/- " + value + "% ");
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double steepness = calcSteepness(link);
			
			if (Math.abs(steepness) > value) {
				counter.incCounter();
//				if (value >= 80) log.info("Id " + link.getId() + ", length " + link.getLength() + ", steepness " + steepness);
			}
		}
		counter.printCounter();
	}
	
	/*
	 * Returns the steepness of a link in %.
	 */
	private double calcSteepness(Link link) {
		double steepness = 0.0;
		double length = link.getLength();
		if (length > 0.0) {
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			
			/*
			 * If 3d coordinates are available, calculate the link's slope.
			 */
			if (fromCoord instanceof Coord3d && toCoord instanceof Coord3d) {
				double fromHeight = ((Coord3d) fromCoord).getZ();
				double toHeight = ((Coord3d) toCoord).getZ();
				double dHeight = toHeight - fromHeight;
				steepness = dHeight / length;
			}		
		}
		// convert to % and return value
		return 100 * steepness;
	}
}
