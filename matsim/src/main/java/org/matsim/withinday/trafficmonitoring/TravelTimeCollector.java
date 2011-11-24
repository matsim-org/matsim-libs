/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCollector.java
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

package org.matsim.withinday.trafficmonitoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

/**
 * Collects link travel times over a given time span (storedTravelTimesBinSize)
 * and calculates an average travel time over this time span.
 * 
 * TODO:
 * - make storedTravelTimesBinSize configurable (e.g. via config)
 * 
 * @author cdobler
 */
public class TravelTimeCollector implements PersonalizableTravelTime,
		LinkEnterEventHandler, LinkLeaveEventHandler, AgentStuckEventHandler,
		AgentArrivalEventHandler, AgentDepartureEventHandler,
		SimulationInitializedListener, SimulationBeforeSimStepListener, SimulationAfterSimStepListener {

	private static final Logger log = Logger.getLogger(TravelTimeCollector.class);

	private Network network;

	// Trips with no Activity on the current Link
	private Map<Id, TripBin> regularActiveTrips; // PersonId
	private Map<Id, TravelTimeInfo> travelTimeInfos; // LinkId

	// Links that are changed by network change events
	private Map<Double, Collection<Link>> changedLinks;
	
	/*
	 * For parallel Execution
	 */
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private UpdateMeanTravelTimesThread[] updateMeanTravelTimesThreads;
	private final int numOfThreads;

	private final int infoTimeStep = 3600;
	private int nextInfoTime = 0;
	
	private Set<Id> agentsToFilter;
	private final Set<String> analyzedModes;
	private final boolean filterModes;
	
	
	// use the factory
	/*package*/ TravelTimeCollector(Scenario scenario, Set<String> analyzedModes) {
		/*
		 * The parallelization should scale almost linear, therefore we do use
		 * the number of available threads according to the config file.
		 */
		this(scenario.getNetwork(), scenario.getConfig().global().getNumberOfThreads(), analyzedModes);
	}

	// use the factory
	/*package*/ TravelTimeCollector(Network network, int numOfThreads, Set<String> analyzedModes) {
		this.network = network;
		this.numOfThreads = numOfThreads;

		if (analyzedModes == null || analyzedModes.size() == 0) {
			this.filterModes = false;
			this.analyzedModes = null;
		} else {
			this.analyzedModes = new HashSet<String>(analyzedModes);
			filterModes = true;
		}
		
		init();
	}

	private void init() {
		regularActiveTrips = new HashMap<Id, TripBin>();
		travelTimeInfos = new ConcurrentHashMap<Id, TravelTimeInfo>();
		changedLinks = new HashMap<Double, Collection<Link>>();
		agentsToFilter = new HashSet<Id>();
		
		for (Link link : this.network.getLinks().values()) {
			TravelTimeInfo travelTimeInfo = new TravelTimeInfo();
			travelTimeInfos.put(link.getId(), travelTimeInfo);
		}
		
		/*
		 * If the network is time variant, we have to update the link parameters
		 * according to the network change events.
		 */
		if (this.network instanceof NetworkImpl) {
			Collection<NetworkChangeEvent> networkChangeEvents = ((NetworkImpl) this.network).getNetworkChangeEvents();
			if (networkChangeEvents != null) {
				for (NetworkChangeEvent networkChangeEvent : networkChangeEvents) {
					ChangeValue freespeedChange = networkChangeEvent.getFreespeedChange();
					if (freespeedChange != null) {
						double startTime = networkChangeEvent.getStartTime();
						Collection<Link> links = changedLinks.get(startTime);
						if (links == null) {
							links = new HashSet<Link>();
							changedLinks.put(startTime, links);
						}
						links.addAll(networkChangeEvent.getLinks());
					}
				}				
			}
		}		
	}

	@Override
	public double getLinkTravelTime(Link link, double time) {
		return travelTimeInfos.get(link.getId()).travelTime;
	}

	@Override
	public void reset(int iteration) {
		init();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		/* 
		 * If only some modes are analyzed, we check whether the agent
		 * performs a trip with one of those modes. if not, we skip the event.
		 */
		if (filterModes && agentsToFilter.contains(event.getPersonId())) return;
		
		Id personId = event.getPersonId();
		double time = event.getTime();

		TripBin tripBin = new TripBin();
		tripBin.enterTime = time;

		this.regularActiveTrips.put(personId, tripBin);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id linkId = event.getLinkId();
		Id personId = event.getPersonId();
		double time = event.getTime();

		TripBin tripBin = this.regularActiveTrips.remove(personId);
		if (tripBin != null) {
			tripBin.leaveTime = time;

			double tripTime = tripBin.leaveTime - tripBin.enterTime;

			TravelTimeInfo travelTimeInfo = travelTimeInfos.get(linkId);
			travelTimeInfo.tripBins.add(tripBin);
			travelTimeInfo.addedTravelTimes += tripTime;
			travelTimeInfo.addedTrips++;

			travelTimeInfo.checkActiveState();
			travelTimeInfo.checkBinSize(tripTime);
		}
	}

	/*
	 * We don't have to count Stuck Events. The MobSim creates LeaveLink Events
	 * before throwing Stuck Events.
	 */
	@Override
	public void handleEvent(AgentStuckEvent event) {

	}

	/*
	 * If an Agent performs an Activity on a Link we have to remove his current
	 * Trip. Otherwise we would have a Trip with the Duration of the Trip itself
	 * and the Activity.
	 */
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();

		this.regularActiveTrips.remove(personId);
		
		// try to remove agent from set with filtered agents
		if (filterModes) this.agentsToFilter.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		/* 
		 * If filtering transport modes is enabled and the agents
		 * starts a leg on a non analyzed transport mode, add the agent
		 * to the filtered agents set.
		 */
		if (filterModes && !analyzedModes.contains(event.getLegMode())) this.agentsToFilter.add(event.getPersonId());
	}
	
	/*
	 * Initially set free speed travel time.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {

		if (e.getQueueSimulation() instanceof Mobsim) {
			double simStartTime = ((Mobsim) e.getQueueSimulation()).getSimTimer().getSimStartTime();

			/*
			 * infoTime may be < simStartTime, this ensures to print 
			 * out the info at the very first timestep already			
			 */
			this.nextInfoTime = (int)(Math.floor(simStartTime / this.infoTimeStep) * this.infoTimeStep);
		}
		
		
		for (Link link : this.network.getLinks().values()) {
			double freeSpeedTravelTime = link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME);

			TravelTimeInfo travelTimeInfo = travelTimeInfos.get(link.getId());
			travelTimeInfo.travelTime = freeSpeedTravelTime;
			travelTimeInfo.init(freeSpeedTravelTime);
		}

		// Now initialize the Parallel Update Threads
		initParallelThreads();
	}

	// Update Link TravelTimeInfos if link attributes have changed
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		Collection<Link> links = changedLinks.remove(e.getSimulationTime());
		
		if (links != null) {
			for (Link link : links) {
				double freeSpeedTravelTime = link.getLength() / link.getFreespeed(e.getSimulationTime());
				TravelTimeInfo travelTimeInfo = travelTimeInfos.get(link.getId());
				travelTimeInfo.init(freeSpeedTravelTime);
				travelTimeInfo.checkActiveState();	// ensure that the estimated link travel time is updated
			}
		}
	}

	// Update Link TravelTimes
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		// parallel Execution
		this.run(e.getSimulationTime());

		printInfo(e.getSimulationTime());
	}

	private void printInfo(double time) {
		if (time >= this.nextInfoTime) {
			int activeLinks = 0;
			for (UpdateMeanTravelTimesThread thread : this.updateMeanTravelTimesThreads) {
				activeLinks += thread.getActiveLinksCount();
			}

			log.info("TravelTimeCollector at " + Time.writeTime(time) + " #links=" + activeLinks);

			this.nextInfoTime += this.infoTimeStep;
		}
	}

	@Override
	public void setPerson(Person person) {
		// nothing to do here
	}

	private static class TripBin {
		double enterTime;
		double leaveTime;
	}

	private static class TravelTimeInfo {

		UpdateMeanTravelTimesThread thread;
		List<TripBin> tripBins = new ArrayList<TripBin>();

		boolean isActive = false;
		// int numActiveTrips = 0;
		int addedTrips = 0;
		double addedTravelTimes = 0.0;
		double sumTravelTimes = 0.0; // We cache the sum of the TravelTimes

		double freeSpeedTravelTime = Double.MAX_VALUE; // We cache the FreeSpeedTravelTimes
		double travelTime = Double.MAX_VALUE; 

		double dynamicBinSize = 0.0; // size of the time window that is taken into account

		static Counter enlarge = new Counter("TravelTimeCollector: enlarged time bin size: ");
		static Counter shrink = new Counter("TravelTimeCollector: shrinked time bin size: ");

		/*package*/ void init(double freeSpeedTravelTime) {
			this.freeSpeedTravelTime = freeSpeedTravelTime;
			this.dynamicBinSize = freeSpeedTravelTime * 2.5;
		}

		/*package*/ void checkActiveState() {
			if (!isActive) {
				this.isActive = true;
				thread.addTravelTimeInfo(this);
			}
		}

		/*package*/ void checkBinSize(double tripTime) {
			if (tripTime > dynamicBinSize) {
				dynamicBinSize = tripTime * 2;
				enlarge.incCounter();
			} else if (tripTime * 3 < dynamicBinSize) {
				dynamicBinSize = tripTime * 3;
				shrink.incCounter();
			}
		}
	}

	/*
	 * ----------------------------------------------------------------
	 * Methods for parallel Execution
	 * ----------------------------------------------------------------
	 */

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
			for (UpdateMeanTravelTimesThread updateMeanTravelTimesThread : updateMeanTravelTimesThreads) {
				updateMeanTravelTimesThread.setTime(time);
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

		// TravelTimeInfo[][] parallelArrays = createArrays();

		updateMeanTravelTimesThreads = new UpdateMeanTravelTimesThread[numOfThreads];

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			UpdateMeanTravelTimesThread updateMeanTravelTimesThread = new UpdateMeanTravelTimesThread();
			updateMeanTravelTimesThread.setName("UpdateMeanTravelTimes" + i);
			updateMeanTravelTimesThread.setStartBarrier(this.startBarrier);
			updateMeanTravelTimesThread.setEndBarrier(this.endBarrier);
			updateMeanTravelTimesThread.setDaemon(true); // make the Thread demons so they will terminate automatically
			updateMeanTravelTimesThreads[i] = updateMeanTravelTimesThread;

			updateMeanTravelTimesThread.start();
		}

		/*
		 * Assign the TravelTimeInfos to the Threads
		 */
		int roundRobin = 0;
		for (TravelTimeInfo travelTimeInfo : this.travelTimeInfos.values()) {
			travelTimeInfo.thread = updateMeanTravelTimesThreads[roundRobin % numOfThreads];
			roundRobin++;
		}

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
	private static class UpdateMeanTravelTimesThread extends Thread {

		private CyclicBarrier startBarrier;
		private CyclicBarrier endBarrier;

		private double time = 0.0;
		private Collection<TravelTimeInfo> activeTravelTimeInfos;

		public UpdateMeanTravelTimesThread() {
			activeTravelTimeInfos = new ArrayList<TravelTimeInfo>();
		}

		public void setStartBarrier(CyclicBarrier cyclicBarrier) {
			this.startBarrier = cyclicBarrier;
		}

		public void setEndBarrier(CyclicBarrier cyclicBarrier) {
			this.endBarrier = cyclicBarrier;
		}

		// public void setTravelTimeInfos(TravelTimeInfo[] travelTimeInfos) {
		// this.travelTimeInfos = travelTimeInfos;
		// }

		public void setTime(final double t) {
			time = t;
		}

		public void addTravelTimeInfo(TravelTimeInfo travelTimeInfo) {
			this.activeTravelTimeInfos.add(travelTimeInfo);
		}

		public int getActiveLinksCount() {
			return this.activeTravelTimeInfos.size();
		}

		@Override
		public void run() {

			while (true) {
				try {
					/*
					 * The End of the Moving is synchronized with the
					 * endBarrier. If all Threads reach this Barrier the main
					 * run() Thread can go on.
					 * 
					 * The Threads wait now at the startBarrier until they are
					 * triggered again in the next TimeStep by the main run()
					 * method.
					 */
					endBarrier.await();

					startBarrier.await();

					Iterator<TravelTimeInfo> iter = activeTravelTimeInfos.iterator();
					while (iter.hasNext()) {
						TravelTimeInfo travelTimeInfo = iter.next();
						calcBinTravelTime(this.time, travelTimeInfo);

						/*
						 * If no further trips are stored in the TravelTimeInfo,
						 * we deactivate the link and ensure that its expected
						 * travel time is its free speed travel time.
						 */
						if (travelTimeInfo.tripBins.size() == 0) {
							travelTimeInfo.isActive = false;
							travelTimeInfo.travelTime = travelTimeInfo.freeSpeedTravelTime;
							iter.remove();
						}
					}

				} catch (InterruptedException e) {
					Gbl.errorMsg(e);
				} catch (BrokenBarrierException e) {
					Gbl.errorMsg(e);
				}
			}
		} // run()

		private void calcBinTravelTime(double time, TravelTimeInfo travelTimeInfo) {
			double removedTravelTimes = 0.0;

			List<TripBin> tripBins = travelTimeInfo.tripBins;

			// first remove old TravelTimes
			Iterator<TripBin> iter = tripBins.iterator();
			while (iter.hasNext()) {
				TripBin tripBin = iter.next();
				if (tripBin.leaveTime + travelTimeInfo.dynamicBinSize < time) {
					double travelTime = tripBin.leaveTime - tripBin.enterTime;
					removedTravelTimes += travelTime;
					iter.remove();
				} else break;
			}

			/*
			 * We don't need an update if no Trips have been added or removed
			 * within the current SimStep. The initial FreeSpeedTravelTime has
			 * to be set correctly via setTravelTime!
			 */
			if (removedTravelTimes == 0.0 && travelTimeInfo.addedTravelTimes == 0.0) return;

			travelTimeInfo.sumTravelTimes = travelTimeInfo.sumTravelTimes - removedTravelTimes + travelTimeInfo.addedTravelTimes;

			travelTimeInfo.addedTravelTimes = 0.0;

			/*
			 * Ensure, that we don't allow TravelTimes shorter than the
			 * FreeSpeedTravelTime.
			 */
			double meanTravelTime = travelTimeInfo.freeSpeedTravelTime;
			if (!tripBins.isEmpty()) meanTravelTime = travelTimeInfo.sumTravelTimes / tripBins.size();

			if (meanTravelTime < travelTimeInfo.freeSpeedTravelTime) {
				log.warn("Mean TravelTime to short?");
				travelTimeInfo.travelTime = travelTimeInfo.freeSpeedTravelTime;
			} else travelTimeInfo.travelTime = meanTravelTime;
		}

	} // ReplannerThread

}