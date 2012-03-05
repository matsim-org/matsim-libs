/* *********************************************************************** *
 * project: org.matsim.*
 * FuzzyTravelTimeEstimatorFactory.java
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

package playground.christoph.evacuation.router.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;

import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

public class FuzzyTravelTimeEstimatorFactory implements PersonalizableTravelTimeFactory {

	private static final Logger log = Logger.getLogger(FuzzyTravelTimeEstimatorFactory.class);
	
	private final Scenario scenario;
	private final PersonalizableTravelTimeFactory timeFactory;
	private final Map<Id, Map<Id, Double>> distanceFuzzyFactors;
	private final AgentsTracker agentsTracker;
	private final VehiclesTracker vehiclesTracker;
	
	public FuzzyTravelTimeEstimatorFactory(Scenario scenario, PersonalizableTravelTimeFactory timeFactory, AgentsTracker agentsTracker,
			VehiclesTracker vehiclesTracker) {
		this.scenario = scenario;
		this.timeFactory = timeFactory;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;

		/*
		 * Create and initialize distanceFuzzyFactor lookup maps
		 */
		this.distanceFuzzyFactors = new ConcurrentHashMap<Id, Map<Id, Double>>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			this.distanceFuzzyFactors.put(link.getId(), new ConcurrentHashMap<Id, Double>());
		}
		initLookupMap(scenario);
	}
	
	@Override
	public FuzzyTravelTimeEstimator createTravelTime() {
		return new FuzzyTravelTimeEstimator(scenario, timeFactory.createTravelTime(), agentsTracker, vehiclesTracker, distanceFuzzyFactors);
	}
	
	/*
	 * Create and initialize distanceFuzzyFactor lookup maps.
	 * Only fuzzy factors < 0.98 are stored in the map to save memory.
	 * Values >= 0.98 are rounded to 1.0 by the FuzzyTravelTimeEstimators.
	 */
	private void initLookupMap(Scenario scenario) {
		
		log.info("Initializing LinkFuzzyFactor lookup map...");
		
		int numThreads = scenario.getConfig().global().getNumberOfThreads();
		Thread[] threads = new Thread[numThreads];
		FuzzyTravelTimeEstimatorRunnable[] runnables = new FuzzyTravelTimeEstimatorRunnable[numThreads];
		Counter counter = new Counter("Entries in link fuzzy factor lookup map ");
		
		for (int i = 0; i < numThreads; i++) {
			FuzzyTravelTimeEstimatorRunnable runnable = new FuzzyTravelTimeEstimatorRunnable(scenario.getNetwork(), this.distanceFuzzyFactors, counter);
			runnables[i] = runnable;
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			thread.setName(FuzzyTravelTimeEstimatorRunnable.class.getName() + i);
			threads[i] = thread;
		}
		
		int i = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			(runnables[i++ % numThreads]).addLink(link);
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
			Gbl.errorMsg(e);
		}
		
		counter.printCounter();
		log.info("done.");
	}
	
	private static class FuzzyTravelTimeEstimatorRunnable implements Runnable {

		private final Network network;
		private final Collection<Link> links;
		private final Map<Id, Map<Id, Double>> distanceFuzzyFactors;
		private final Counter counter;
		
		public FuzzyTravelTimeEstimatorRunnable(Network network, Map<Id, Map<Id, Double>> distanceFuzzyFactors, Counter counter) {
			this.network = network;
			this.distanceFuzzyFactors = distanceFuzzyFactors;
			this.counter = counter;
			
			this.links = new ArrayList<Link>();
		}
		
		public void addLink(Link link) {
			this.links.add(link);
		}
		
		@Override
		public void run() {
			for (Link fromLink : links) {
				
				Map<Id, Double> fuzzyFactors = distanceFuzzyFactors.get(fromLink.getId());
				for (Link toLink : network.getLinks().values()) {
					double distance = CoordUtils.calcDistance(fromLink.getCoord(), toLink.getCoord());
					
					double factor = 1 / (1 + Math.exp((-distance/1500.0) + 4.0));
					
					if (factor < 0.98) {
						fuzzyFactors.put(toLink.getId(), factor);
						counter.incCounter();
					}
				}
			}			
		}
		
	}
}