/* *********************************************************************** *
 * project: org.matsim.*
 * LookupTravelTime.java
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

package playground.christoph.router.traveltime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.router.util.TravelTime;

public class LookupTravelTime implements PersonalizableTravelTime, SimulationBeforeSimStepListener {
	
	private LookupNetwork lookupNetwork;
	private TravelTime travelTime;
	
	private int updateInterval = 1;
	
	/*
	 * For parallel Execution
	 */
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private UpdateTravelTimesThread[] threads;
	private final int numOfThreads;
	
	public LookupTravelTime(LookupNetwork lookupNetwork, TravelTime travelTime, int numOfThreads) {
		this.lookupNetwork = lookupNetwork;
		this.travelTime = travelTime;
		this.numOfThreads = numOfThreads;
		
		this.initParallelThreads();
	}
	
	public void setUpdateInterval(int interval) {
		this.updateInterval = interval;
	}
	
	@Override
	public double getLinkTravelTime(final Link link, double time) {
		if (link instanceof RoutingNetworkLink) return ((LookupNetworkLink)((RoutingNetworkLink) link).getLink()).getLinkTravelTime();
		else if (link instanceof LookupNetworkLink) return ((LookupNetworkLink) link).getLinkTravelTime();
		else throw new RuntimeException("Unexpected link type found: " + link.getClass().toString());
	}

	/*
	 * Update link travel times
	 */
	@Override
	public void notifySimulationBeforeSimStep(final SimulationBeforeSimStepEvent e) {
		
		if (e.getSimulationTime() % updateInterval == 0) {
			// parallel Execution
			this.run(e.getSimulationTime());			
		}		
	}

	@Override
	public void setPerson(Person person) {
		// nothing to do here		
	}

	/*
	 * The Threads are waiting at the TimeStepStartBarrier. We trigger them by
	 * reaching this Barrier. Now the Threads will start moving the Nodes. We
	 * wait until all of them reach the TimeStepEndBarrier to move on. We should
	 * not have any Problems with Race Conditions because even if the Threads
	 * would be faster than this Thread, means they reach the TimeStepEndBarrier
	 * before this Method does, it should work anyway.
	 */
	private void run(double time) {

		try {
			// set current Time
			for (UpdateTravelTimesThread thread : threads) {
				thread.setTime(time);
			}

			this.startBarrier.await();

			this.endBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void initParallelThreads() {

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		threads = new UpdateTravelTimesThread[numOfThreads];

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			UpdateTravelTimesThread thread = new UpdateTravelTimesThread(this.travelTime, startBarrier, endBarrier);
			thread.setName("UpdateTravelTimesThread" + i);
			thread.setDaemon(true); // make the Threads demons so they will terminate automatically
			threads[i] = thread;

			thread.start();
		}

		/*
		 * Assign the links to the threads
		 */
		Map<Integer, List<LookupNetworkLink>> map = new HashMap<Integer, List<LookupNetworkLink>>();
		for (int i = 0; i < numOfThreads; i++) map.put(i, new ArrayList<LookupNetworkLink>());
			
		int roundRobin = 0;
		for (LookupNetworkLink link : lookupNetwork.getLinks().values()) {
			map.get(roundRobin % numOfThreads).add(link);
			roundRobin++;
		}
		
		for (int i = 0; i < numOfThreads; i++) {
			List<LookupNetworkLink> list = map.get(i);
			LookupNetworkLink[] links = new LookupNetworkLink[list.size()];
			threads[i].setLinks(list.toArray(links));
		}
		map.clear();

		/*
		 * After initialization the Threads are waiting at the endBarrier. We
		 * trigger this Barrier once so they wait at the startBarrier what has
		 * to be their state if the run() method is called.
		 */
		try {
			this.endBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}
	
	/*
	 * The thread class that updates the Mean Travel Times in the MyLinksImpls.
	 */
	private static class UpdateTravelTimesThread extends Thread {

		private TravelTime travelTime;
		private CyclicBarrier startBarrier;
		private CyclicBarrier endBarrier;

		private double time = 0.0;
		private LookupNetworkLink[] links;
		
		public UpdateTravelTimesThread(TravelTime travelTime, CyclicBarrier startBarrier, CyclicBarrier endBarrier) {
			this.travelTime = travelTime;
			this.startBarrier = startBarrier;
			this.endBarrier = endBarrier;
		}

		public void setTime(final double t) {
			time = t;
		}

		public void setLinks(LookupNetworkLink[] links) {
			this.links = links;
		}

		@Override
		public void run() {

			while (true) {
				try {
					/*
					 * The End of the update process is synchronized with the
					 * endBarrier. If all Threads reach this Barrier the main
					 * run() Thread can go on.
					 * 
					 * The Threads wait now at the startBarrier until they are
					 * triggered again in the next TimeStep by the main run()
					 * method.
					 */
					endBarrier.await();

					startBarrier.await();

					for (LookupNetworkLink link : links) {
						link.setLinkTravelTime(travelTime.getLinkTravelTime(link, time));
					}
				} catch (InterruptedException e) {
					Gbl.errorMsg(e);
				} catch (BrokenBarrierException e) {
					Gbl.errorMsg(e);
				}
			}
		} // run()
		
	} // UpdateTravelTimesThread
}
