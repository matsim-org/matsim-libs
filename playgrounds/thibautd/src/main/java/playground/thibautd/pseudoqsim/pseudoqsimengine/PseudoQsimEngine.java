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
package playground.thibautd.pseudoqsim.pseudoqsimengine;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.thibautd.pseudoqsim.QVehicleProvider;

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
public class PseudoQsimEngine implements MobsimEngine, DepartureHandler {
	private static final Logger log =
		Logger.getLogger(PseudoQsimEngine.class);

	private final Collection<String> transportModes;
	private final TravelTime travelTimeCalculator;
	private final Network network;

	private final QVehicleProvider vehicleProvider;

	private InternalInterface internalInterface = null;

	private final BreakableCyclicBarrier startBarrier;
	private final BreakableCyclicBarrier endBarrier;
	private final BreakableCyclicBarrier finalBarrier;
	private final TripHandlingRunnable[] runnables;
	private final Thread[] threads;

	private Throwable crashCause = null;

	private final Random random;

	public PseudoQsimEngine(
			final int nThreads,
			final Collection<String> transportModes,
			final TravelTime travelTimeCalculator,
			final Network network,
			final QVehicleProvider vehicles) {
		this.vehicleProvider = vehicles;
		this.transportModes = transportModes;
		this.travelTimeCalculator = travelTimeCalculator;
		this.network = network;

		log.info( "initializing "+getClass().getName()+" with "+nThreads+" threads" );
		this.startBarrier = new BreakableCyclicBarrier( nThreads + 1 );
		this.endBarrier = new BreakableCyclicBarrier( nThreads + 1 );
		this.finalBarrier = new BreakableCyclicBarrier( nThreads + 1 );
		this.runnables = new TripHandlingRunnable[ nThreads ];
		this.threads = new Thread[ nThreads ];

		final UncaughtExceptionHandler exceptionHandler =
			new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(
						final Thread t,
						final Throwable e) {
					if ( e instanceof UncheckedBrokenBarrierException ) {
						// this is a result of the "reset" below: ignore to avoid spamming the user
						return;
					}
					log.error( "thread "+t.getName()+" threw exception. Aborting." , e );

					PseudoQsimEngine.this.crashCause = e;
					for ( TripHandlingRunnable r : runnables ) r.stopRun();

					startBarrier.makeBroken();
					endBarrier.makeBroken();
					finalBarrier.makeBroken();
				}
			};
		for ( int i = 0; i < nThreads; i++ ) {
			this.runnables[ i ] = new TripHandlingRunnable( i );
			this.threads[ i ] = new Thread( this.runnables[ i ] );
			this.threads[ i ].setName( "PseudoQSimThread."+i );
			this.threads[ i ].setUncaughtExceptionHandler(
					exceptionHandler );
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
		catch (Exception e) {
			if ( crashCause != null ) {
				if ( crashCause instanceof RuntimeException ) throw (RuntimeException) crashCause;
				if ( crashCause instanceof Error ) throw (Error) crashCause;
			}
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
			// agent not null occurs in transit.
			assert vehicle.getDriver() == null || vehicle.getDriver().getId().equals( magent.getId() ) :
				vehicle.getId()+" has non-null driver "+vehicle.getDriver().getId();
			vehicle.setDriver(agent);
			agent.setVehicle(vehicle) ;

			final EventsManager eventsManager = ((QSim) internalInterface.getMobsim()).getEventsManager();
			eventsManager.processEvent(
					new PersonEntersVehicleEvent(
						now,
						agent.getId(),
						vehicleId) );

			//vehicle.setCurrentLink( linkId );

			eventsManager.processEvent(
					new Wait2LinkEvent(
						now,
						agent.getId(),
						linkId,
						vehicleId) );

			chooseRunnable().addArrivalEvent(
					// do not travel on first link
					// calcArrival(
					new InternalArrivalEvent(
						now,
						linkId,
						vehicle) );
		}

		return true;
	}

	private TripHandlingRunnable chooseRunnable() {
		return runnables[ random.nextInt( runnables.length ) ];
	}

	public QVehicle getVehicle(final Id id) {
		return vehicleProvider.getVehicle( id );
	}

	private InternalArrivalEvent calcArrival(
			final double now,
			final Id linkId,
			final QVehicle vehicle) {
		final Person person =
			vehicle.getDriver() instanceof HasPerson ?
					((HasPerson) vehicle.getDriver()).getPerson() :
					null;
		final double travelTime =
			travelTimeCalculator.getLinkTravelTime(
					network.getLinks().get( linkId ),
					now,
					person,
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
		try {
			if ( log.isTraceEnabled() ) log.trace( "call start barrier..." );
			startBarrier.await();
			if ( log.isTraceEnabled() ) log.trace( "call start barrier... DONE" );

			for ( TripHandlingRunnable r : runnables ) {
				// stop thread
				if ( log.isTraceEnabled() ) log.trace( "stopping runnable "+r );
				r.stopRun();
			}

			if ( log.isTraceEnabled() ) log.trace( "call end barrier..." );
			endBarrier.await();
			if ( log.isTraceEnabled() ) log.trace( "call end barrier... DONE" );

			if ( log.isTraceEnabled() ) log.trace( "call final barrier..." );
			finalBarrier.await();
			if ( log.isTraceEnabled() ) log.trace( "call final barrier... DONE" );
		}
		catch (InterruptedException e) {
			throw new RuntimeException();
		}
		catch (BrokenBarrierException e) {
			throw new RuntimeException();
		}

		for ( TripHandlingRunnable r : runnables ) {
			assert r.isFinished : r.isRunning;
			if ( log.isTraceEnabled() ) log.trace( "clean runnable "+r );
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
		private final Queue<InternalArrivalEvent> arrivalQueue = new PriorityBlockingQueue<InternalArrivalEvent>();

		private boolean isRunning = true;
		private boolean isFinished = false;
		private double time = Double.NaN;

		private final int instanceNr;

		public TripHandlingRunnable(final int nr) {
			this.instanceNr = nr;
		}

		@Override
		public String toString() {
			return "TripHandlingRunnable."+instanceNr;
		}

		public void addArrivalEvent(final InternalArrivalEvent event) {
			arrivalQueue.add( event );
		}

		public void setTime( double time ) {
			this.time = time;
		}

		public void stopRun() {
			isRunning = false;
		}

		public void afterSim() {
			for (InternalArrivalEvent event : arrivalQueue) {
				final QVehicle veh = event.vehicle;
				((QSim) internalInterface.getMobsim()).getEventsManager().processEvent(
						new PersonStuckEvent(
							internalInterface.getMobsim().getSimTimer().getTimeOfDay(),
							veh.getDriver().getId(),
							veh.getDriver().getCurrentLinkId(),
							veh.getDriver().getMode()));
				((QSim) internalInterface.getMobsim()).getAgentCounter().incLost();
				((QSim) internalInterface.getMobsim()).getAgentCounter().decLiving();
			}
		}

		@Override
		public void run() {
			try {
				while ( isRunning ) {
					if (log.isTraceEnabled()) log.trace( this+" starts waiting for start" );
					startBarrier.await();
					if (log.isTraceEnabled()) log.trace( this+" ends waiting for start" );
					assert !Double.isNaN( time );
					// TODO: handle transit drivers their own way.
					while ( !arrivalQueue.isEmpty() &&
							arrivalQueue.peek().time <= time ) {
						handleEvent( arrivalQueue.poll() );
					}
					if (log.isTraceEnabled()) log.trace( this+" starts waiting for end" );
					endBarrier.await();
					if (log.isTraceEnabled()) log.trace( this+" ends waiting for end" );
				}

				isFinished = true;
				// just to make sure we wait for all threads to be finished before cleanup
				// otherwise, in tests, assert isFinished may fail, just because cleanup starts
				// before changing the value.
				if (log.isTraceEnabled()) log.trace( this+" starts final waiting" );
				finalBarrier.await();
				if (log.isTraceEnabled()) log.trace( this+" ends final waiting" );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
			catch (BrokenBarrierException e) {
				throw new UncheckedBrokenBarrierException( e );
			}
		}

		private final void handleEvent(final InternalArrivalEvent event) {
			final MobsimDriverAgent agent = event.vehicle.getDriver();

			final EventsManager eventsManager =
					((QSim) internalInterface.getMobsim()).getEventsManager();

			if ( agent instanceof TransitDriverAgent ) {
				final TransitDriverAgent transitDriver = (TransitDriverAgent) agent;
				final TransitStopFacility stop = transitDriver.getNextTransitStop();

				if ((stop != null) && (stop.getLinkId().equals( event.linkId ) ) ) {
					final double delay = handleTransitStop( transitDriver , stop , time );
					if ( delay > 0 ) {
						arrivalQueue.add(
								new InternalArrivalEvent(
									time + delay,
									event.linkId,
									event.vehicle) );
						return;
					}
				}
			}

			final Id nextLinkId = agent.chooseNextLinkId();

			if ( !agent.isWantingToArriveOnCurrentLink() ) {
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
	}

	// for thread safety: handle transit stop implies some not-thread-safe
	// passenger queue management.
	private static synchronized double handleTransitStop(
			final TransitDriverAgent transitDriver,
			final TransitStopFacility stop,
			final double time) {
		return transitDriver.handleTransitStop( stop , time );
	}

	private static class BreakableCyclicBarrier {
		private boolean broken = false;
		private CyclicBarrier barrier;

		public BreakableCyclicBarrier(final int n) {
			this.barrier = new CyclicBarrier( n );
		}

		public void makeBroken() {
			this.broken = true;
			barrier.reset();
		}

		public int await() throws InterruptedException, BrokenBarrierException {
			if (broken) throw new BrokenBarrierException();
			return barrier.await();
		}
	}

	private static class UncheckedBrokenBarrierException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public UncheckedBrokenBarrierException(final Throwable t) { super( t ); }
	}
}

