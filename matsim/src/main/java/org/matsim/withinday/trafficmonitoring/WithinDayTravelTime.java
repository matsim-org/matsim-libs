/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayTravelTime.java
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
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * Collects link travel times over a given time span (storedTravelTimesBinSize)
 * and calculates an average travel time over this time span.
 *
 * TODO:
 * - make storedTravelTimesBinSize configurable (e.g. via config)
 *
 * @author cdobler
 */
@Singleton
public class WithinDayTravelTime implements TravelTime,
		LinkEnterEventHandler, LinkLeaveEventHandler, PersonStuckEventHandler,
		VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler,
		MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimAfterSimStepListener,
		MobsimBeforeCleanupListener {

	private static final Logger log = LogManager.getLogger(WithinDayTravelTime.class);

	private Network network;

	// Trips with no Activity on the current Link
	private Map<Id<Vehicle>, TripBin> regularActiveTrips; // VehicleId
	private Map<Id<Link>, TravelTimeInfo> travelTimeInfos; // LinkId

	private TravelTimeInfoProvider travelTimeInfoProvider;

	// Links that are changed by network change events
	private TreeMap<Double, Map<Link,Double>> changedLinksByTime;
	// yy better a priority queue.  kai, dec'17

	/*
	 * For parallel Execution
	 */
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private UpdateMeanTravelTimesRunnable[] updateMeanTravelTimesRunnables;
	private final int numOfThreads;

	private final int infoTimeStep = 3600;
	private int nextInfoTime = 0;

	private Set<Id<Vehicle>> vehiclesToFilter;
	private final Set<String> analyzedModes;
	private final boolean filterModes;

	private boolean problem = true ;
	private int resetCnt = 0;

	private double now = Double.NEGATIVE_INFINITY ;

	@Inject
	WithinDayTravelTime(Scenario scenario) {
		this(scenario, null);
	}

	public WithinDayTravelTime(Scenario scenario, Set<String> analyzedModes) {
//		log.setLevel(Level.DEBUG);

		/*
		 * The parallelization should scale almost linear, therefore we do use
		 * the number of available threads according to the config file.
		 */
		this.network = scenario.getNetwork();
		this.numOfThreads = scenario.getConfig().global().getNumberOfThreads();

		if (analyzedModes == null || analyzedModes.size() == 0) {
			this.filterModes = false;
			this.analyzedModes = null;
		} else {
			this.analyzedModes = new HashSet<>(analyzedModes);
			filterModes = true;
		}

		init();
	}

	private void init() {
		this.regularActiveTrips = new HashMap<>();
		this.travelTimeInfos = new ConcurrentHashMap<>();
		this.changedLinksByTime = new TreeMap<>();
		this.vehiclesToFilter = new HashSet<>();

		// one TravelTimeInfo per link:
		for (Link link : this.network.getLinks().values()) {
			TravelTimeInfo travelTimeInfo = new TravelTimeInfo();
			this.travelTimeInfos.put(link.getId(), travelTimeInfo);
		}

		/*
		 * If no RoutingNetwork is used, ArrayBasedTravelTimeInfoProvider uses
		 * a MapBasedTravelTimeInfoProvider as fall back solution. This increases
		 * routing times of a AStarLandmarks router by ~5%.
		 */
//		this.travelTimeInfoProvider = new MapBasedTravelTimeInfoProvider(this.travelTimeInfos);
		this.travelTimeInfoProvider = new ArrayBasedTravelTimeInfoProvider(this.travelTimeInfos, this.network);

		/*
		 * If the network is time variant, we have to update the link parameters
		 * according to the network change events.
		 */
		Queue<NetworkChangeEvent> networkChangeEvents = NetworkUtils.getNetworkChangeEvents(this.network);

		// I just changed the return type of the network change events from Collection to Queue.
		// This should, in a first step, allow to remove the separate changedLinksByTime data structure.
		// In a second step, this should allow to insert link change events after everything
		// has started.  kai, dec'17
		// Well, but maybe I don't even want this, and I want this class here recognize changes
		// only when someone observes them??? (bushfire evacuation use case). kai, dec'17

		// If one looks at transport telematics, then we need to also be able to set expected
		// speeds for the future!  Which would indeed justify its own data model.  kai, dec'17
		// (in contrast, watch out: the NetworkChangeEventsEngine also keeps its own
		// copy for the mobsim)

		if (networkChangeEvents != null) {
			for (NetworkChangeEvent networkChangeEvent : networkChangeEvents) {
				addNetworkChangeEventToLocalDataStructure(networkChangeEvent);
			}
		}
	}

	private void addNetworkChangeEventToLocalDataStructure(NetworkChangeEvent networkChangeEvent) {
		ChangeValue freespeedChange = networkChangeEvent.getFreespeedChange();
		if (freespeedChange != null) {
			double startTime = networkChangeEvent.getStartTime();
			Map<Link,Double> newLinkSpeedsAtThisTime = changedLinksByTime.computeIfAbsent(startTime, k -> new HashMap<>());
			for ( Link link : networkChangeEvent.getLinks() ) {
				// yy seems that the following should be available centrally. kai, dec'17
				double newSpeed ;
				switch ( freespeedChange.getType() ) {
					case ABSOLUTE_IN_SI_UNITS:
						newSpeed = freespeedChange.getValue() ;
						break;
					case FACTOR:
						newSpeed = link.getFreespeed() * freespeedChange.getValue() ;
						break;
					case OFFSET_IN_SI_UNITS:
						newSpeed = link.getFreespeed() + freespeedChange.getValue() ;
						break;
					default:
						throw new RuntimeException("change event type not implemented") ;
				}
				if ( startTime > 0. ) {
					log.debug( "registering a change event for time=" + startTime
					+ "; linkId=" + link.getId() ) ;
				}
				newLinkSpeedsAtThisTime.put( link, newSpeed ) ;
			}
		}
	}

	public final void addNetworkChangeEvent(NetworkChangeEvent networkChangeEvent) {
		this.addNetworkChangeEventToLocalDataStructure(networkChangeEvent);
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		final double travelTime = this.travelTimeInfoProvider.getTravelTimeInfo(link).travelTime;
		return travelTime;
	}

	@Override
	public void reset(int iteration) {
		init();
		resetCnt++ ;
		if ( resetCnt >1 ) {
			if ( problem ) {
				throw new RuntimeException("using WithinDayTravelTime, but mobsim notifications not called between two resets.  "
						+ "Did you really add this as a mobsim listener?") ;
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		/*
		 * If only some modes are analyzed, we check whether the vehicle
		 * performs a trip with one of those modes. if not, we skip the event.
		 */
		if (filterModes && vehiclesToFilter.contains(event.getVehicleId())) return;

		Id<Vehicle> vehicleId = event.getVehicleId();
		double time = event.getTime();

		TripBin tripBin = new TripBin();
		tripBin.enterTime = time;

		this.regularActiveTrips.put(vehicleId, tripBin);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Vehicle> vehicleId = event.getVehicleId();
		double time = event.getTime();

		TripBin tripBin = this.regularActiveTrips.remove(vehicleId);
		if (tripBin != null) {
			tripBin.leaveTime = time;

			double tripTime = tripBin.leaveTime - tripBin.enterTime;

			TravelTimeInfo travelTimeInfo = this.travelTimeInfoProvider.getTravelTimeInfo(linkId);
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
	public void handleEvent(PersonStuckEvent event) {

	}

	/*
	 * If a vehicle leaves the traffic we have to remove its current
	 * trip. Otherwise we would have a trip with the duration of the trip itself
	 * and the activity.
	 */
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();

		this.regularActiveTrips.remove(vehicleId);

		// try to remove vehicle from set with filtered vehicles
		if (filterModes) this.vehiclesToFilter.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		/*
		 * If filtering transport modes is enabled and the vehicle
		 * starts a leg on a non analyzed transport mode, add the vehicle
		 * to the filtered vehicles set.
		 */
		if (filterModes && !analyzedModes.contains(event.getNetworkMode())) this.vehiclesToFilter.add(event.getVehicleId());
	}

	/*
	 * Initially set free speed travel time.
	 */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		problem = false ;

		if (e.getQueueSimulation() instanceof QSim) {
			double simStartTime = ((QSim) e.getQueueSimulation()).getSimTimer().getSimStartTime();

			/*
			 * infoTime may be < simStartTime, this ensures to print
			 * out the info at the very first timestep already
			 */
			this.nextInfoTime = (int)(Math.floor(simStartTime / this.infoTimeStep) * this.infoTimeStep);
		}


		for (Link link : this.network.getLinks().values()) {
			double freeSpeedTravelTime = link.getLength() / link.getFreespeed();

			TravelTimeInfo travelTimeInfo = this.travelTimeInfoProvider.getTravelTimeInfo(link);
			travelTimeInfo.travelTime = freeSpeedTravelTime;
			travelTimeInfo.init(freeSpeedTravelTime);
		}

		// Now initialize the Parallel Update Threads
		initParallelThreads();
	}

	// Update Link TravelTimeInfos if link attributes have changed
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		problem = false ;

		// yyyy In terms of "bushfire evacuation" design (and maybe even in terms of more general transport telematics)
		// one would need some settable TimeDependentNetworkUpdater class.  kai, dec'17

//		Collection<Link> links = changedLinksByTime.remove(e.getSimulationTime());
		// yyyyyy I find it quite optimistic to do this with doubles. What
		// if someone adds a link change event in between two integer
		// time steps?  kai, dec'17

		while( !changedLinksByTime.isEmpty() && changedLinksByTime.firstKey() <= e.getSimulationTime() ) {
			Map<Link, Double> map = changedLinksByTime.pollFirstEntry().getValue();
			for ( Map.Entry<Link,Double> link2speed : map.entrySet() ) {
				Link link = link2speed.getKey() ;
				double freeSpeedTravelTime = link.getLength() / link2speed.getValue() ;
				if ( e.getSimulationTime() > ((QSim)e.getQueueSimulation()).getSimTimer().getSimStartTime() ) {
					// (otherwise, in some simulations one gets a lot of change events at time 0. kai, dec'17)
					log.debug("time=" + e.getSimulationTime() +
									  "; network change event for link=" + link.getId() +
									  "; new ttime="+ freeSpeedTravelTime );
				}
				TravelTimeInfo travelTimeInfo = this.travelTimeInfoProvider.getTravelTimeInfo(link);
				travelTimeInfo.init(freeSpeedTravelTime);
				travelTimeInfo.checkActiveState();	// ensure that the estimated link travel time is updated
			}
		}

//		if (links != null) {
//			for (Link link : links) {
//				double freeSpeedTravelTime = link.getLength() / link.getFreespeed(e.getSimulationTime());
//				if ( e.getSimulationTime() > 0 ) {
//					log.debug("time=" + e.getSimulationTime() +
//									  ";\tnetwork change event for link=" + link.getId() +
//									  ";\tnew ttime="+ freeSpeedTravelTime );
//				}
//				TravelTimeInfo travelTimeInfo = this.travelTimeInfoProvider.getTravelTimeInfo(link);
//				travelTimeInfo.init(freeSpeedTravelTime);
//				travelTimeInfo.checkActiveState();	// ensure that the estimated link travel time is updated
//			}
//		}
	}

	// Update Link TravelTimes
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		problem = false ;

		now = e.getSimulationTime() ;

		// parallel Execution
		this.run(e.getSimulationTime());

		printInfo(e.getSimulationTime());
	}


	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		problem = false ;

		/*
		 * Calling the afterSim Method of the Threads will set their
		 * simulationRunning flag to false.
		 */
		for (UpdateMeanTravelTimesRunnable runnable : this.updateMeanTravelTimesRunnables) {
			runnable.afterSim();
		}

		/*
		 * Triggering the startBarrier of the UpdateMeanTravelTimesRunnables.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will terminate.
		 */
		try {
			this.startBarrier.await();
		} catch (InterruptedException | BrokenBarrierException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void printInfo(double time) {
		if (time >= this.nextInfoTime) {
			int activeLinks = 0;
			for (UpdateMeanTravelTimesRunnable runnable : this.updateMeanTravelTimesRunnables) {
				activeLinks += runnable.getActiveLinksCount();
			}

			log.info("WithinDayTravelTime at " + Time.writeTime(time) + " #links=" + activeLinks);

			this.nextInfoTime += this.infoTimeStep;
		}
	}

	private static class TripBin {
		double enterTime;
		double leaveTime;
	}

	/*package*/ static class TravelTimeInfo {

		UpdateMeanTravelTimesRunnable runnable;
		List<TripBin> tripBins = new ArrayList<>();

		boolean isActive = false;
		// int numActiveTrips = 0;
		int addedTrips = 0;
		double addedTravelTimes = 0.0;
		double sumTravelTimes = 0.0; // We cache the sum of the TravelTimes

		double freeSpeedTravelTime = Double.MAX_VALUE; // We cache the FreeSpeedTravelTimes
		double travelTime = Double.MAX_VALUE;

		double dynamicBinSize = 0.0; // size of the time window that is taken into account

		static Counter enlarge = new Counter("WithinDayTravelTime: enlarged time bin size: ");
		static Counter shrink = new Counter("WithinDayTravelTime: shrunk time bin size: ");

		/*package*/ void init(double freeSpeedTravelTime) {
			this.freeSpeedTravelTime = freeSpeedTravelTime;
			this.dynamicBinSize = freeSpeedTravelTime * 2.5;
		}

		/*package*/ void checkActiveState() {
			if (!isActive) {
				this.isActive = true;
				runnable.addTravelTimeInfo(this);
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
			for (UpdateMeanTravelTimesRunnable updateMeanTravelTimesRunnable : updateMeanTravelTimesRunnables) {
				updateMeanTravelTimesRunnable.setTime(time);
			}

			this.startBarrier.await();

			this.endBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	private void initParallelThreads() {

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		Thread[] threads = new Thread[numOfThreads];
		this.updateMeanTravelTimesRunnables = new UpdateMeanTravelTimesRunnable[numOfThreads];

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			UpdateMeanTravelTimesRunnable updateMeanTravelTimesRunnable = new UpdateMeanTravelTimesRunnable();
			updateMeanTravelTimesRunnable.setStartBarrier(this.startBarrier);
			updateMeanTravelTimesRunnable.setEndBarrier(this.endBarrier);
			updateMeanTravelTimesRunnables[i] = updateMeanTravelTimesRunnable;

			Thread thread = new Thread(updateMeanTravelTimesRunnable);
			thread.setName("UpdateMeanTravelTimes" + i);
			thread.setDaemon(true); // make the Thread demons so they will terminate automatically
			threads[i] = thread;

			thread.start();
		}

		/*
		 * Assign the TravelTimeInfos to the Threads
		 */
		int roundRobin = 0;
		for (TravelTimeInfo travelTimeInfo : this.travelTimeInfos.values()) {
			travelTimeInfo.runnable = updateMeanTravelTimesRunnables[roundRobin % numOfThreads];
			roundRobin++;
		}

		/*
		 * After initialization the Threads are waiting at the endBarrier. We
		 * trigger this Barrier once so they wait at the startBarrier what has
		 * to be their state when the run() method is called.
		 */
		try {
			this.endBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * The thread class that updates the mean travel times.
	 */
	private static class UpdateMeanTravelTimesRunnable implements Runnable {

		private volatile boolean simulationRunning = true;

		private CyclicBarrier startBarrier = null;
		private CyclicBarrier endBarrier = null;

		private OptionalTime time = OptionalTime.undefined();
		private Collection<TravelTimeInfo> activeTravelTimeInfos;

		public UpdateMeanTravelTimesRunnable() {
			activeTravelTimeInfos = new ArrayList<>();
		}

		public void setStartBarrier(CyclicBarrier cyclicBarrier) {
			this.startBarrier = cyclicBarrier;
		}

		public void setEndBarrier(CyclicBarrier cyclicBarrier) {
			this.endBarrier = cyclicBarrier;
		}

		public void setTime(final double t) {
			time = OptionalTime.defined(t);
		}

		public void addTravelTimeInfo(TravelTimeInfo travelTimeInfo) {
			this.activeTravelTimeInfos.add(travelTimeInfo);
		}

		public int getActiveLinksCount() {
			return this.activeTravelTimeInfos.size();
		}

		public void afterSim() {
			this.simulationRunning = false;
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

					/*
					 * Check if Simulation is still running.
					 * Otherwise print CPU usage and end Thread.
					 */
					if (!simulationRunning) {
						Gbl.printCurrentThreadCpuTime();
						return;
					}

					Iterator<TravelTimeInfo> iter = activeTravelTimeInfos.iterator();
					while (iter.hasNext()) {
						TravelTimeInfo travelTimeInfo = iter.next();
						calcBinTravelTime(this.time.seconds(), travelTimeInfo);

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

				} catch (InterruptedException | BrokenBarrierException e) {
					throw new RuntimeException(e);
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
//			if (removedTravelTimes == 0.0 && travelTimeInfo.addedTravelTimes == 0.0) return;
			// yyyyyy does not work when a network change event comes in. If the old functionality was intentional, we need to talk:
			// We are setting speed to zero in the bushfire, and if there is no car on the link already, no car will enter it
			// (because of special within-day rerouting logic). kai, feb'18

			travelTimeInfo.sumTravelTimes = travelTimeInfo.sumTravelTimes - removedTravelTimes + travelTimeInfo.addedTravelTimes;

			travelTimeInfo.addedTravelTimes = 0.0;
			/*
			 * Ensure that we don't allow TravelTimes shorter than the FreeSpeedTravelTime.
			 */
			double meanTravelTime = travelTimeInfo.freeSpeedTravelTime;
			if (!tripBins.isEmpty()) meanTravelTime = travelTimeInfo.sumTravelTimes / tripBins.size();

			if (meanTravelTime < travelTimeInfo.freeSpeedTravelTime) {
//				log.warn("Mean TravelTime too short?");
				// can happen when network change event came in with lower speed. kai, feb'18
				travelTimeInfo.travelTime = travelTimeInfo.freeSpeedTravelTime;
			} else {
				travelTimeInfo.travelTime = meanTravelTime;
			}
		}

	} // ReplannerRunnable

}
