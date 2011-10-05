/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoMobsimService.java
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
package playground.johannes.coopsim.services;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.router.util.TravelTime;

import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.socialnetworks.utils.CollectionUtils;

/**
 * @author illenberger
 *
 */
public class PseudoMobsimService implements SimService<Collection<Trajectory>> {

	private ExecutorService executor;
	
	private final MobsimWrapper[] wrappers;
	
	private Future<?>[] futures;
	
	private final SimService<Collection<Plan>> planService;
	
	private final Network network;
	
	private final TravelTime travelTimes;
	
	private final EventsManager eventsManager;
	
	public PseudoMobsimService(SimService<Collection<Plan>> planService, Network network, TravelTime travelTimes, EventsManager eventsManager, int numThreads) {
		this.planService = planService;
		this.network = network;
		this.travelTimes = travelTimes;
		this.eventsManager = eventsManager;
		
		wrappers = new MobsimWrapper[numThreads];
		
		if(numThreads > 1) {
			executor = Executors.newFixedThreadPool(numThreads);
			futures = new Future[numThreads];
		}
	}
	
	@Override
	public void init() {
		
	}

	@Override
	public void run() {
		((EventsManagerImpl)eventsManager).resetHandlers(0);
		
		Collection<Plan> plans = planService.get();
		
		if(executor == null) {
			if(wrappers[0] == null) {
				wrappers[0] = new MobsimWrapper(network, travelTimes, eventsManager);
			}
			
			wrappers[0].init(plans);
			wrappers[0].run();
			
		} else {
			/*
			 * split collection in approx even segments
			 */
			int n = Math.min(plans.size(), wrappers.length);
			List<Plan>[] segments = CollectionUtils.split(plans, n);
			/*
			 * submit tasks
			 */
			for(int i = 0; i < segments.length; i++) {
				if(wrappers[i] == null) {
					wrappers[i] = new MobsimWrapper(network, travelTimes, eventsManager);
				}
				wrappers[i].init(segments[i]);
				futures[i] = executor.submit(wrappers[i]);
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
		}
	}

	@Override
	public Collection<Trajectory> get() {
		return null;
	}

	@Override
	public void terminate() {
		if(executor != null)
			executor.shutdown();
	}

	private class MobsimWrapper implements Runnable {

		private final PseudoMobsim mobsim;
		
		private final EventsManager eventsManager;
		
		private Collection<Plan> plans;
		
		public MobsimWrapper(Network network, TravelTime travelTimes, EventsManager eventsManager) {
			mobsim = new PseudoMobsim(network, travelTimes);
			this.eventsManager = eventsManager;
		}
		
		public void init(Collection<Plan> plans) {
			this.plans = plans;
		}
		
		@Override
		public void run() {
			mobsim.run(plans, eventsManager);
		}
		
	}
}
