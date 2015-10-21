/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.demand.tasks;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.router.*;
import org.matsim.pt.config.TransitConfigGroup;
import playground.johannes.gsv.demand.PopulationTask;
import playground.johannes.socialnetworks.utils.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author johannes
 *
 */
public class PlanRouteLegs implements PopulationTask {

	private final Scenario scenario;
	
	private RouteThread[] threads;
	
	private Future<?>[] futures;
	
	private final ExecutorService executor;
	
	private final int numThreads = 24;
	
	public PlanRouteLegs(Scenario scenario) {
		this.scenario = scenario;
		
		executor = Executors.newFixedThreadPool(numThreads);
		
		threads = new RouteThread[numThreads];
		for(int i = 0; i < numThreads; i++)
			threads[i] = new RouteThread();
		
		futures = new Future[numThreads];
	}
	
	@Override
	public void apply(Population pop) {
		Collection<? extends Person> persons = pop.getPersons().values();
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(persons.size(), threads.length);
		List<? extends Person>[] segments = CollectionUtils.split(persons, n);
		/*
		 * submit tasks
		 */
		ProgressLogger.init(persons.size(), 1, 10);
		for(int i = 0; i < segments.length; i++) {
			threads[i].persons = segments[i];
			threads[i].scenario = scenario;
			futures[i] = executor.submit(threads[i]);
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < segments.length; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		ProgressLogger.termiante();
		
	}
	
	private static class RouteThread implements Runnable {

		private List<? extends Person> persons;
		
		private Scenario scenario;
		
		@Override
		public void run() {
			TransitConfigGroup transitConfig = (TransitConfigGroup) scenario.getConfig().getModule(TransitConfigGroup.GROUP_NAME);
			Set<String> modes = new HashSet<String>();
			modes.add("pt");
			transitConfig.setTransitModes(modes);
			
//			TransitRouterConfigGroup routerConfig = (TransitRouterConfigGroup) scenario.getConfig().getModule(TransitRouterConfigGroup.GROUP_NAME);
//			routerConfig.setSearchRadius(0);
//			routerConfig.setExtensionRadius(0);

			TripRouter tripRouter = new TripRouterFactoryBuilderWithDefaults().build(scenario).get();
			PlanRouter router = new PlanRouter(tripRouter);
			for (Person p : persons) {
				router.run( p );
				ProgressLogger.step();
			}

		}
		
	}

}
