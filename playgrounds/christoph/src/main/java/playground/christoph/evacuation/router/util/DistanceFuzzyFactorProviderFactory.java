/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceFuzzyFactorProviderFactory.java
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

package playground.christoph.evacuation.router.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;

import playground.christoph.evacuation.config.EvacuationConfig;

public class DistanceFuzzyFactorProviderFactory {

	private static final Logger log = Logger.getLogger(DistanceFuzzyFactorProviderFactory.class);
	
	public static boolean useLookupMap = true;
	
	private final Map<Id, Map<Id, Double>> distanceFuzzyFactors;
	private final Set<Id> observedLinks;
		
	/**
	 * Creates and initializes distanceFuzzyFactor lookup maps.
	 * Link pairs located outside the main observation area (e.g.
	 * outside a 30 km radius around the research area) are ignored
	 * and a fuzzy factor of 0.0 is returned for them.
	 */
	DistanceFuzzyFactorProviderFactory(Scenario scenario) {	
		this.observedLinks = new HashSet<Id>();
		identifyObservedLinks(scenario);
		
		this.distanceFuzzyFactors = new ConcurrentHashMap<Id, Map<Id, Double>>();
		for (Id linkId : observedLinks) {
			this.distanceFuzzyFactors.put(linkId, new ConcurrentHashMap<Id, Double>());
		}
		
		if (useLookupMap) fillLookupMap(scenario);
	}
	
	public DistanceFuzzyFactorProvider createInstance() {
		if (useLookupMap) return new DistanceFuzzyFactorProviderLookup(this.distanceFuzzyFactors, this.observedLinks);
		else return new DistanceFuzzyFactorProviderCalculate(observedLinks);
	}
	
	/*
	 * Identify those links that have to be observed.
	 */
	private void identifyObservedLinks(Scenario scenario) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			/*
			 * If the link is located inside the observed circle, which is
			 * the inner evacuation circle + 10km. This guarantees that all link
			 * pairs where at least one link is located inside the inner
			 * evacuation circle AND that have a distance fuzzy factor < ~ 1.00
			 * are included.
			 */
			double distance = CoordUtils.calcDistance(link.getCoord(), EvacuationConfig.centerCoord);
			if (distance < EvacuationConfig.innerRadius + 10000.0) observedLinks.add(link.getId());
		}
		log.info("Observe " + observedLinks.size() + " links out of total " + scenario.getNetwork().getLinks().size() + " links.");
	}
	
	/*
	 * Create and initialize distanceFuzzyFactor lookup maps.
	 * Only fuzzy factors < 0.98 are stored in the map to save memory.
	 * Values >= 0.98 are rounded to 1.0 by the FuzzyTravelTimeEstimators.
	 */
	private void fillLookupMap(Scenario scenario) {
		
		log.info("Initializing LinkFuzzyFactor lookup map using " + scenario.getConfig().global().getNumberOfThreads() + " threads ...");
		
		int numThreads = scenario.getConfig().global().getNumberOfThreads();
		Thread[] threads = new Thread[numThreads];
		DistanceFuzzyFactorProviderRunnable[] runnables = new DistanceFuzzyFactorProviderRunnable[numThreads];
		Counter counter = new Counter("Handled links for fuzzy factor lookup map ");
		AtomicLong entryCount = new AtomicLong();
		
		for (int i = 0; i < numThreads; i++) {
			DistanceFuzzyFactorProviderRunnable runnable = new DistanceFuzzyFactorProviderRunnable(scenario.getNetwork(), 
					this.distanceFuzzyFactors, this.observedLinks, counter, entryCount);
			runnables[i] = runnable;
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			thread.setName(DistanceFuzzyFactorProviderRunnable.class.getName() + i);
			threads[i] = thread;
		}
		
		int i = 0;
		for (Id linkId : this.observedLinks) {
			(runnables[i++ % numThreads]).addLink(linkId);
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
		
		counter.printCounter();
		log.info("Number of fuzzy factor lookup entries " + 2*entryCount.get());	// multiply by two since each entry is used for the link pair
		log.info("Done.");
	}
	
	private static class DistanceFuzzyFactorProviderRunnable implements Runnable {

		private final Network network;
		private final Collection<Id> links;
		private final Set<Id> observedLinks;
		private final Map<Id, Map<Id, Double>> distanceFuzzyFactors;
		private final Counter counter;
		private final AtomicLong entryCount;
		
		private final double c = 1 / Math.exp(4);	// constant for fuzzy factor creation
		
		public DistanceFuzzyFactorProviderRunnable(Network network, Map<Id, Map<Id, Double>> distanceFuzzyFactors, 
				Set<Id> observedLinks, Counter counter, AtomicLong entryCount) {
			this.network = network;
			this.distanceFuzzyFactors = distanceFuzzyFactors;
			this.observedLinks = observedLinks;
			this.counter = counter;
			this.entryCount = entryCount;
			
			this.links = new ArrayList<Id>();
		}
		
		public void addLink(Id linkId) {
			this.links.add(linkId);
		}
		
		/*
		 * So far use hard-coded values between 0.017 (distance 0.0) 
		 * and 1.0 (distance ~ 15000.0).
		 * (15000.0 for -distance/1500.0)
		 */
		@Override
		public void run() {
			
			// iterating over an array should be faster than over a set
			Id[] observedLinksArray = new Id[observedLinks.size()];
			observedLinksArray = observedLinks.toArray(observedLinksArray);
			
			for (Id fromLinkId : links) {
				Link fromLink = network.getLinks().get(fromLinkId);
				
				Map<Id, Double> fuzzyFactors = distanceFuzzyFactors.get(fromLink.getId());
				for (Id toLinkId : observedLinksArray) {
					
					// only store one value for each Id pair
					int cmp = fromLinkId.compareTo(toLinkId);
					if (cmp > 0) continue;
					
					Link toLink = network.getLinks().get(toLinkId);
					
					double distance = CoordUtils.calcDistance(fromLink.getCoord(), toLink.getCoord());
					
					/*
					 * Adapted formula, starting with:
					 * factor = 1 / (1 + Math.exp((-distance/1000.0) + 4.0))
					 * 
					 * Math.exp((-distance/1000.0) + 4) = Math.exp(-distance/1000.0) * Math.exp(4)
					 * -> factor = 1 / (1 + Math.exp(-distance/1000.0) * Math.exp(4));
					 * 
					 * divide by 1 / Math.exp(4)
					 * -> factor = (1 / Math.exp(4)) / ((1 / Math.exp(4)) + Math.exp(-distance/1000.0));
					 * 
					 * define c = 1 / Math.exp(4)
					 * -> factor = c / (c + Math.exp(-distance/1000.0));
					 * 
					 * -> factor = c / (c + 1/Math.exp(distance/1000.0));
					 * -> factor = c / ( (c* Math.exp(distance/1000.0) + 1)/Math.exp(distance/1000.0) );
					 * 
					 * multiply with Math.exp(distance/1000.0)
					 * -> factor = (c * Math.exp(distance/1000.0)) / (c * Math.exp(distance/1000.0) + 1);
					 * 
					 * define c2 = c * Math.exp(distance/1000.0)
					 * -> factor = c2 / (c2 + 1)
					 */
//					double factor = 1 / (1 + Math.exp((-distance/1500.0) + 4.0));
//					double factor = 1 / (1 + Math.exp((-distance/1000.0) + 4.0));
					double c2 = c * Math.exp(distance/1000.0);
					double factor = c2 / (c2 + 1);
					
					if (factor < 0.98) {
						fuzzyFactors.put(toLink.getId(), factor);
						entryCount.incrementAndGet();
					}
				}
				counter.incCounter();
			}
		}		
	}
}
