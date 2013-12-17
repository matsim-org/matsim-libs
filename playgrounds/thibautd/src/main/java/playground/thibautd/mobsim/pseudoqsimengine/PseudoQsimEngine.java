/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoQsimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.mobsim.pseudoqsimengine;

import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriver;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleUtils;

import playground.thibautd.mobsim.QVehicleProvider;

/**
 * An engine aimed at replacing the QnetsimEngine for a "PSim" like behavior.
 * The advantage compared to the PSim is that the rest of the QSim architecture
 * is still used.
 *
 * This is, for instance, very useful if the results of the simulation depend
 * on special agents, such as is the case for ride sharing.
 *
 * It does NOT allow to reuse special QLink implementations, for obvious reasons
 * @author thibautd
 */
public class PseudoQsimEngine implements MobsimEngine, DepartureHandler, QVehicleProvider {
	private static final Logger log =
		Logger.getLogger(PseudoQsimEngine.class);

	private final Collection<String> transportModes;
	private final TravelTime travelTimeCalculator;
	private final Network network;

	private final Map<Id, QVehicle> vehicles = new ConcurrentHashMap<Id, QVehicle>();

	private InternalInterface internalInterface = null;

	private final CyclicBarrier startBarrier;
	private final CyclicBarrier endBarrier;
	private final TripHandlingRunnable[] runnables;
	private final Thread[] threads;

	private final Random random;

	public PseudoQsimEngine(
			final Collection<String> transportModes,
			final TravelTime travelTimeCalculator,
			final Network network) {
		this( 1 , transportModes , travelTimeCalculator , network );
	}

	public PseudoQsimEngine(
			final int nThreads,
			final Collection<String> transportModes,
			final TravelTime travelTimeCalculator,
			final Network network) {
		this.transportModes = transportModes;
		this.travelTimeCalculator = travelTimeCalculator;
		this.network = network;

		this.startBarrier = new CyclicBarrier( nThreads + 1 );
		this.endBarrier = new CyclicBarrier( nThreads + 1 );
		this.runnables = new TripHandlingRunnable[ nThreads ];
		this.threads = new Thread[ nThreads ];
		for ( int i = 0; i < nThreads; i++ ) {
			this.runnables[ i ] = new TripHandlingRunnable();
			this.threads[ i ] = new Thread( this.runnables[ i ] );
			this.threads[ i ].start();
		}

		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public void doSimStep(final double time) {
		for ( TripHandlingRunnable r : runnables ) r.setTime( time );

		try {
			startBarrier.await();
			endBarrier.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		catch (BrokenBarrierException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent magent,
			final Id linkId) {
		if ( !transportModes.contains( magent.getMode() ) ) return false;

		// code adapted from VehicularDepartureHandler
		
		final DriverAgent agent = (DriverAgent) magent;

		final Id vehicleId = agent.getPlannedVehicleId() ;
		// TODO: check that vehicle not in use
		// TODO: insert vehicles at beginning and track them on links
		// (NetsimEngine like).
		// The problem is that currently only the NetsimEngine can do that.
		final QVehicle vehicle = getVehicle( vehicleId );

		// Treat the situation where startLink == endLink.
		// Transit vehicles do this differently than others, because there could be a stop on it.
		// Other vehicles _do not_ traverse their only link but arrive right away.
		if ( ! (agent instanceof AbstractTransitDriver) ) { 	
			if (linkId.equals(agent.getDestinationLinkId())) {
				if ( agent.chooseNextLinkId() == null ) {
					if ( log.isTraceEnabled() ) {
						log.info( "handling case startLink == endLink for agent "+agent );
					}
					assert vehicle.getDriver() == null;
					// do NOT do that! the agent is NOT removed from the vehicle after it!
                    //vehicle.setDriver(agent);
                    //agent.setVehicle(vehicle);

					// the QNetsimEngine does not bother to generate an event...
					//final EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
					//eventsManager.processEvent(
					//		new PersonEntersVehicleEvent(
					//			now,
					//			agent.getId(),
					//			vehicleId) );

					magent.endLegAndComputeNextState(now) ;
					this.internalInterface.arrangeNextAgentState(magent) ;
					return true;
				}
			}
		}		

		if (vehicle == null) {
			throw new RuntimeException( "TODO" );
			//if (vehicleBehavior == VehicleBehavior.TELEPORT) {
			//	vehicle = qNetsimEngine.getVehicles().get(vehicleId);
			//	if ( vehicle==null ) {
			//		throw new RuntimeException("could not find requested vehicle in simulation; aborting ...") ;
			//	}
			//	teleportVehicleTo(vehicle, linkId);

			//	vehicle.setDriver(agent);
			//	agent.setVehicle(vehicle) ;

			//	qlink.letVehicleDepart(vehicle, now);
			//	// (since the "teleportVehicle" does not physically move the vehicle, this is finally achieved in the departure
			//	// logic.  kai, nov'11)
			//} else if (vehicleBehavior == VehicleBehavior.WAIT_UNTIL_IT_COMES_ALONG) {
			//	// While we are waiting for our car
			//	qlink.registerDriverAgentWaitingForCar(agent);
			//} else {
			//	throw new RuntimeException("vehicle " + vehicleId + " not available for agent " + agent.getId() + " on link " + linkId);
			//}
		}
		else {
			assert vehicle.getDriver() == null;
			vehicle.setDriver(agent);
			agent.setVehicle(vehicle) ;

			final EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
			eventsManager.processEvent(
					new PersonEntersVehicleEvent(
						now,
						agent.getId(),
						vehicleId) );

			//vehicle.setCurrentLink( linkId );

			runnables[ random.nextInt( runnables.length ) ].addArrivalEvent(
					// do not travel on first link
					// calcArrival(
					new InternalArrivalEvent(
						now,
						linkId,
						vehicle) );

			eventsManager.processEvent(
					new Wait2LinkEvent(
						now,
						agent.getId(),
						linkId,
						vehicleId) );
		}

		return true;
	}

	@Override
	public QVehicle getVehicle(final Id id) {
		QVehicle v = vehicles.get( id );

		if ( v == null ) {
			v = new QVehicle(
				VehicleUtils.getFactory().createVehicle(
					id,
					VehicleUtils.getDefaultVehicleType() ) );
			vehicles.put( id , v );
		}

		return v;
	}

	private InternalArrivalEvent calcArrival(
			final double now,
			final Id linkId,
			final QVehicle vehicle) {
		final double travelTime =
			travelTimeCalculator.getLinkTravelTime(
					network.getLinks().get( linkId ),
					now,
					((HasPerson) vehicle.getDriver()).getPerson(),
					vehicle.getVehicle() );
		return new InternalArrivalEvent(
				now + travelTime,
				linkId,
				vehicle);
	}

	@Override
	public void onPrepareSim() {}
	
	@Override
	public void afterSim() {
		for ( TripHandlingRunnable r : runnables ) {
			r.afterSim();
		}
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	private static class InternalArrivalEvent implements Comparable<InternalArrivalEvent> {
		private final double time;
		private final Id linkId;
		private final QVehicle vehicle;

		public InternalArrivalEvent(
				final double time,
				final Id linkId,
				final QVehicle vehicle) {
			this.time = time;
			this.linkId = linkId;
			this.vehicle = vehicle;
		}

		@Override
		public int compareTo(final InternalArrivalEvent o) {
			return Double.compare( time , o.time );
		}
	}

	private class TripHandlingRunnable implements Runnable {
		private final Queue<InternalArrivalEvent> arrivalQueue = new PriorityQueue<InternalArrivalEvent>();

		private double time = Double.NaN;

		public void addArrivalEvent(final InternalArrivalEvent event) {
			arrivalQueue.add( event );
		}

		public void setTime( double time ) {
			this.time = time;
		}

		public void afterSim() {
			for (InternalArrivalEvent event : arrivalQueue) {
				final QVehicle veh = event.vehicle;
				internalInterface.getMobsim().getEventsManager().processEvent(
						new PersonStuckEvent(
							internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
							veh.getDriver().getId(),
							veh.getDriver().getCurrentLinkId(),
							veh.getDriver().getMode()));
				internalInterface.getMobsim().getAgentCounter().incLost();
				internalInterface.getMobsim().getAgentCounter().decLiving();
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					startBarrier.await();
					assert !Double.isNaN( time );
					// TODO: handle transit drivers their own way.
					while ( !arrivalQueue.isEmpty() &&
							arrivalQueue.peek().time <= time ) {
						final InternalArrivalEvent event = arrivalQueue.poll();
						final MobsimDriverAgent agent = event.vehicle.getDriver();
						final Id nextLinkId = agent.chooseNextLinkId();

						final EventsManager eventsManager =
							internalInterface.getMobsim().getEventsManager();
						if ( nextLinkId != null ) {
							eventsManager.processEvent(
								new LinkLeaveEvent(
									time,
									agent.getId(),
									event.linkId,
									event.vehicle.getId() ) );

							agent.notifyMoveOverNode( nextLinkId );

							eventsManager.processEvent(
								new LinkEnterEvent(
									time,
									agent.getId(),
									nextLinkId,
									event.vehicle.getId() ) );

							arrivalQueue.add(
									calcArrival(
										time,
										nextLinkId,
										event.vehicle) );
						}
						else {
							eventsManager.processEvent(
									new PersonLeavesVehicleEvent(
										time,
										agent.getId(),
										event.vehicle.getId()));
							// reset vehicles driver
							event.vehicle.setDriver(null);

							agent.endLegAndComputeNextState( time );
							internalInterface.arrangeNextAgentState( agent );
						}
					}
					endBarrier.await();
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
			catch (BrokenBarrierException e) {
				throw new RuntimeException( e );
			}
		}
	}
}

