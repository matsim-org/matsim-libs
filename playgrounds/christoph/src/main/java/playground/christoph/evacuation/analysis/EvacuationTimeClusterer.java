/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTimeClusterer.java
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

package playground.christoph.evacuation.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.Clusterable;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import playground.christoph.router.FullNetworkDijkstra;
import playground.christoph.router.FullNetworkDijkstraFactory;

public class EvacuationTimeClusterer {

	private static final Logger log = Logger.getLogger(EvacuationTimeClusterer.class);
	
	private Network network;
	private Map<BasicLocation, List<Double>> locationMap;
	private Map<BasicLocation, Map<BasicLocation, Double>> costMap;
	private QuadTree <ClusterableLocation> quadTree;
	
	private int numOfThreads = 1;
	
	public EvacuationTimeClusterer(Network network, Map<BasicLocation, List<Double>> locationMap, int numOfThreads) {
		this.network = network;
		this.locationMap = locationMap;
		this.numOfThreads = numOfThreads;
		
		initQuadTree();
	}
	
	private void initQuadTree() {
		log.info("initializing ClusterableLocation quad tree...");
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (BasicLocation location : locationMap.keySet()) {
			if (location.getCoord().getX() < minx) { minx = location.getCoord().getX(); }
			if (location.getCoord().getY() < miny) { miny = location.getCoord().getY(); }
			if (location.getCoord().getX() > maxx) { maxx = location.getCoord().getX(); }
			if (location.getCoord().getY() > maxy) { maxy = location.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info("xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
				
		this.quadTree = new QuadTree<ClusterableLocation>(minx, miny, maxx, maxy);
		
		log.info("done.");
	}
	
	private void buildQuadTree(List<ClusterableLocation> locations) {
		log.info("filling ClusterableLocation quad tree...");
		
		for (ClusterableLocation clusterableLocation : locations) {
			BasicLocation location = clusterableLocation.getBasicLocation();
			quadTree.put(location.getCoord().getX(), location.getCoord().getY(), clusterableLocation);
		}
		
		log.info("done.");
	}
	
	/*package*/ Map<BasicLocation, List<Double>> buildCluster(int numClusters, int iterations) {
		
		createCostMap();
		
		KMeansPlusPlusClusterer<ClusterableLocation> clusterer = new KMeansPlusPlusClusterer<ClusterableLocation>(MatsimRandom.getLocalInstance());

		List<ClusterableLocation> points = getClusterableLocations();
		
		buildQuadTree(points);
		
		log.info("do clustering...");
		List<Cluster<ClusterableLocation>> list = clusterer.cluster(points, numClusters, iterations);
		
		Map<BasicLocation, List<Double>> map = new HashMap<BasicLocation, List<Double>>();
		
		for (Cluster<ClusterableLocation> cluster : list) {
			BasicLocation center = cluster.getCenter().getBasicLocation();
			
			List<Double> evacuationTimes = new ArrayList<Double>();
			for (ClusterableLocation location : cluster.getPoints()) {
				List<Double> pointTravelTimes = locationMap.get(location.getBasicLocation());
				evacuationTimes.addAll(pointTravelTimes);
			}
			
			map.put(center, evacuationTimes);
		}
		
		log.info("done.");
		
		return map;
	}
	
	private List<ClusterableLocation> getClusterableLocations() {
		List<ClusterableLocation> points = new ArrayList<ClusterableLocation>();
		
		for (BasicLocation location : locationMap.keySet()) {
			ClusterableLocation clusterableLocation = new ClusterableLocation(location, costMap.get(location), locationMap, quadTree);
			points.add(clusterableLocation);
		}
		
		return points;
	}

	private void createCostMap() {
		log.info("Total OD Pairs to Calculate: " + (int)Math.pow(locationMap.size(), 2));
		Counter counter = new Counter("Calculated OD Pairs: ");
		
		costMap = new ConcurrentHashMap<BasicLocation, Map<BasicLocation, Double>>();

		Thread[] threads = new Thread[numOfThreads];
		for (int i = 0; i < numOfThreads; i++) {

			TravelTimeCost travelTimeCost = new TravelTimeCost();
			FullNetworkDijkstra leastCostPathCalculator = new FullNetworkDijkstraFactory().createPathCalculator(network, travelTimeCost, travelTimeCost);
			
			Thread thread = new ParallelThread();
			thread.setDaemon(true);
			thread.setName("ParallelEvacuationTimeClusterThread" + i);
			((ParallelThread) thread).network = network;
			((ParallelThread) thread).counter = counter;
			((ParallelThread) thread).locationMap = locationMap;
			((ParallelThread) thread).costMap = costMap;
			((ParallelThread) thread).leastCostPathCalculator = leastCostPathCalculator;
			threads[i] = thread;
		}
		
		int i = 0;
		for (BasicLocation fromLocation : locationMap.keySet()) {
			((ParallelThread) threads[i++ % numOfThreads]).fromLocations.add(fromLocation);
		}
		
		for (Thread thread : threads) {
			thread.start();
		}
		
		// wait until each thread is finished
		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		log.info("done.");
	}
		
	private static class ClusterableLocation implements Clusterable<ClusterableLocation> {

		private BasicLocation location;
		private Map<BasicLocation, Double> costMap;	// distances to other locations
		private Map<BasicLocation, List<Double>> locationMap;
		private QuadTree <ClusterableLocation> quadTree;
		
		public ClusterableLocation(BasicLocation location, Map<BasicLocation, Double> costMap, 
				Map<BasicLocation, List<Double>> locationMap, QuadTree <ClusterableLocation> quadTree) {
			this.location = location;
			this.costMap = costMap;
			this.locationMap = locationMap;
			this.quadTree = quadTree;
		}
		
		/*package*/ BasicLocation getBasicLocation() {
			return this.location;
		}

		/*
		 * Calculated weighted by the number of Persons per location!
		 */
		@Override
		public ClusterableLocation centroidOf(Collection<ClusterableLocation> collection) {
			if (collection.size() == 0) return this;
			
			double sumX = 0.0;
			double sumY = 0.0;
			int sumPersons = 0;
			for (ClusterableLocation location : collection) {
				Coord coord = location.getBasicLocation().getCoord();

				int persons = locationMap.get(location.getBasicLocation()).size();
				sumPersons = sumPersons + persons;
				sumX = sumX + coord.getX() * persons;
				sumY = sumY + coord.getY() * persons;
			}
			
			double x = sumX / sumPersons;
			double y = sumY / sumPersons;
			
			return quadTree.get(x, y);
		}

		@Override
		public double distanceFrom(ClusterableLocation location) {
			return costMap.get(location.getBasicLocation());
		}	
	}	// ClusterableLocation
	
	
	private static class TravelTimeCost implements TravelTime, TravelDisutility {

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return link.getLength()/link.getFreespeed();
		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
//			return getLinkTravelTime(link, time);
			return link.getLength();
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return getLinkTravelDisutility(link, 0.0, null, null);
		}
	}
	
	private static class ParallelThread extends Thread {
		
		Network network;
		Counter counter;
		Map<BasicLocation, List<Double>> locationMap;
		Map<BasicLocation, Map<BasicLocation, Double>> costMap;
		FullNetworkDijkstra leastCostPathCalculator;
		List<BasicLocation> fromLocations = new ArrayList<BasicLocation>();
		
		@Override
		public void run() {
					
			for (BasicLocation fromLocation : fromLocations) {
				Map<BasicLocation, Double> toCosts = new HashMap<BasicLocation, Double>();

				calcLeastCostTree(fromLocation);
				
				for (BasicLocation toLocation : locationMap.keySet()) {
					double costs = calcCost(fromLocation, toLocation);
					toCosts.put(toLocation, costs);
					counter.incCounter();
				}
				
				costMap.put(fromLocation, toCosts);
			}
		}
		
		private void calcLeastCostTree(BasicLocation from) {
			Link fromLink;
			
			if (from instanceof Link) fromLink = (Link) from;
			else fromLink = network.getLinks().get(((Facility) from).getLinkId());
			
			leastCostPathCalculator.calcLeastCostTree(fromLink.getToNode(), Time.UNDEFINED_TIME);
		}
		
		private double calcCost(BasicLocation from, BasicLocation to) {
			Link fromLink;
			Link toLink;
			
			if (from instanceof Link) fromLink = (Link) from;
			else fromLink = network.getLinks().get(((Facility) from).getLinkId());
			
			if (to instanceof Link) toLink = (Link) to;
			else toLink = network.getLinks().get(((Facility) to).getLinkId());
			
			Path path = leastCostPathCalculator.calcLeastCostPath(fromLink.getToNode(), toLink.getToNode(), Time.UNDEFINED_TIME, null, null); 
			
			return path.travelCost;
		}
	}
}
