/* *********************************************************************** *
 * project: org.matsim.*
 * JointModesDepartureHandler.java
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
package org.matsim.contrib.socnetsim.jointtrips.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import org.matsim.contrib.socnetsim.qsim.QVehicleProvider;
import org.matsim.contrib.socnetsim.utils.IdentifiableCollectionsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.VehicleUsingAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;

/**
 * @author thibautd
 */
public class JointModesDepartureHandler implements DepartureHandler , MobsimEngine {
	private static final Logger log =
		LogManager.getLogger(JointModesDepartureHandler.class);

	private final QVehicleProvider vehicleProvider;
	private final DepartureHandler departureHandler;

	private final PassengersWaitingPerDriver passengersWaitingPerDriver = new PassengersWaitingPerDriver();
	// map driverId -> driver info
	private final Map<Id , WaitingDriver> waitingDrivers =
		new LinkedHashMap<Id , WaitingDriver>();
	
	public JointModesDepartureHandler(
			final QNetsimEngineI netsimEngine) {
//		this( new NetsimWrappingQVehicleProvider( netsimEngine ),
//				netsimEngine.getVehicularDepartureHandler() );
		throw new RuntimeException("this execution path is no longer possible.  You have to set the NetworkModeDepartureHandler (aka VehicularDepartureHandler) upstream.  kai, jan'25");
	}

	public JointModesDepartureHandler(
			final QVehicleProvider vehicleProvider,
			final DepartureHandler departureHandler) {
		this.vehicleProvider = vehicleProvider;
		this.departureHandler = departureHandler;
	}

	// /////////////////////////////////////////////////////////////////////////
	// departure handler
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public boolean handleDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		final String mode = agent.getMode();

		if ( mode.equals( JointActingTypes.DRIVER ) ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "Handling DRIVER departure for agent "+agent );
			}
			handleDriverDeparture( now , agent , linkId );
			return true;
		}

		if ( mode.equals( JointActingTypes.PASSENGER ) ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "Handling PASSENGER departure for agent "+agent );
			}
			handlePassengerDeparture( now , agent , linkId );
			return true;
		}

		return departureHandler.handleDeparture( now , agent , linkId );
	}

	private void handleDriverDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		assert agent.getCurrentLinkId().equals( linkId ) : agent+" not at link "+linkId;

		final Id<Person> driverId = agent.getId();
		final Collection<Id<Person>> passengerIds = getPassengerIds( agent );
		final Id vehicleId = getVehicleId( agent );
		final MobsimVehicle vehicle = vehicleProvider.getVehicle( vehicleId );

		final Map<Id, PassengerAgent> passengersWaiting =
			passengersWaitingPerDriver.getPassengersWaitingDriverAtLink(
					driverId , linkId );
		final Collection<Id> presentPassengers = new ArrayList<Id>();
		presentPassengers.addAll( passengersWaiting.keySet() );
		IdentifiableCollectionsUtils.addAll( presentPassengers , vehicle.getPassengers() );

		// all persons in vehicle should be identified as passengers
		assert IdentifiableCollectionsUtils.containsAll( passengerIds , vehicle.getPassengers() ) :
			passengerIds+" does not contains all of "+vehicle.getPassengers()+" with present passengers "+presentPassengers;

		if ( presentPassengers.containsAll( passengerIds ) ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "All passengers "+passengerIds+" in present passengers "+presentPassengers );
				log.trace( "Processing to departure for driver "+driverId );
			}

			// all passengers are or already in the car,
			// or waiting. Board waiting passengers and depart.
			for (Id passengerId : passengerIds) {
				final PassengerAgent passenger = passengersWaiting.remove( passengerId );
				if ( passenger != null ) {
					// say to the driver to board the passenger before leaving the first
					// link. We cannot add the passengers to the vehicle here, as the driver
					// may have to wait for the vehicle to come before departing.
					((PassengerUnboardingDriverAgent) agent).addPassengerToBoard( passenger );
				}
				else assert IdentifiableCollectionsUtils.contains( vehicle.getPassengers(), passengerId );
			}

			// this needs to happen before calling the network departure handler!
			// in case of A-to-A legs or zero duration activities,
			// the departure handler will call arangeAgentNextState before returning.
			// This caused problems when there were other joint trips in this sequence.
			waitingDrivers.remove( driverId );
			if ( log.isTraceEnabled() ) {
				log.trace( "driver "+driverId+" removed from waiting list." );
				log.trace( "waiting list is now "+waitingDrivers.keySet() );
			}

			// it is possible that not all passengers boarded yet,
			// but the car mustn't contain passengers that are not
			// identified as such.
			assert IdentifiableCollectionsUtils.containsAll( passengerIds , vehicle.getPassengers() ) :
				passengerIds+" does not contains all of "+vehicle.getPassengers()+" with present passengers "+presentPassengers;

			final boolean handled =
				departureHandler.handleDeparture(
						now,
						agent,
						linkId );

			if ( !handled ) {
				throw new RuntimeException( "failed to handle departure. Check the main modes?" );
			}

			if ( log.isTraceEnabled() ) {
				log.trace( "departure of driver "+driverId+" handled succesfully" );
			}
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.trace( "NOT all passengers "+passengerIds+" in present passengers "+presentPassengers );
				log.trace( "NOT (yet) processing to departure for driver "+driverId );
			}

			waitingDrivers.put( driverId , new WaitingDriver( agent , linkId ) );

			if ( log.isTraceEnabled() ) {
				log.trace( "driver "+driverId+" added to waiting list." );
				log.trace( "waiting list is now "+waitingDrivers.keySet() );
			}
		}
	}

	private static Id getVehicleId(final MobsimAgent agent) {
		if ( !(agent instanceof VehicleUsingAgent) ) throw new RuntimeException( agent.getClass().toString() );
		return ((VehicleUsingAgent) agent).getPlannedVehicleId();
	}

	private static Collection<Id<Person>> getPassengerIds(final MobsimAgent agent) {
		if ( !(agent instanceof PlanAgent) ) throw new RuntimeException( agent.getClass().toString() );
		final Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
		final DriverRoute route = (DriverRoute) currentLeg.getRoute();
		return route.getPassengersIds();
	}

	private void handlePassengerDeparture(
			final double now,
			final MobsimAgent agent,
			final Id linkId) {
		final Id driverId = getDriverId( agent );

		// go in the "queue"
		final Map<Id, PassengerAgent> waiting =
			passengersWaitingPerDriver.getPassengersWaitingDriverAtLink(
					driverId,
					linkId );

		waiting.put( agent.getId() , (PassengerAgent) agent );

		// if the driver is waiting, as him to depart.
		// departure will succeed only of all passengers are here;
		// otherwise, everybody will wait for everybody to be here.
		final WaitingDriver wDriver = waitingDrivers.get( driverId );
		if ( wDriver != null && wDriver.linkId.equals( linkId ) ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "driver "+driverId+" found waiting: ask him to depart for passenger "+agent.getId() );
			}
			handleDriverDeparture(
					now,
					wDriver.driverAgent,
					linkId );
		}
		else if ( log.isTraceEnabled() ) {
			log.trace( "driver "+driverId+" NOT found waiting: DO NOTHING for passenger "+agent.getId() );
			log.trace( "waiting list is "+waitingDrivers.keySet() );
		}
	}

	private static Id getDriverId(final MobsimAgent agent) {
		if ( !(agent instanceof PlanAgent) ) throw new RuntimeException( agent.getClass().toString() );
		final Leg currentLeg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
		final PassengerRoute route = (PassengerRoute) currentLeg.getRoute();
		return route.getDriverId();
	}

	// /////////////////////////////////////////////////////////////////////////
	// engine
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void doSimStep(final double time) {
		// do nothing
	}

	@Override
	public void onPrepareSim() {
		// do nothing
	}

	@Override
	public void afterSim() {
		// do nothing
	}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested classes
	// /////////////////////////////////////////////////////////////////////////
	private final class WaitingDriver {
		public final MobsimAgent driverAgent;
		public final Id linkId;

		public WaitingDriver(
				final MobsimAgent agent,
				final Id link) {
			this.driverAgent = agent;
			this.linkId = link;
		}
	}

	private final class PassengersWaitingPerDriver {
		private final Map<Id, PassengersWaitingForDriver> map =
			new LinkedHashMap<Id, PassengersWaitingForDriver>();

		public Map<Id, PassengerAgent> getPassengersWaitingDriverAtLink(
				final Id driverId,
				final Id linkId) {
			PassengersWaitingForDriver ps = map.get( driverId );

			if ( ps == null ) {
				ps = new PassengersWaitingForDriver();
				map.put( driverId , ps );
			}

			return ps.getPassengersWaitingAtLink( linkId );
		}
	}

	private final class PassengersWaitingForDriver {
		private final Map<Id, Map<Id, PassengerAgent>> agentsAtLink = new LinkedHashMap<Id, Map<Id, PassengerAgent>>();

		public Map<Id, PassengerAgent> getPassengersWaitingAtLink(final Id linkId) {
			Map<Id, PassengerAgent> ps = agentsAtLink.get( linkId );

			if ( ps == null ) {
				ps = new LinkedHashMap<Id, PassengerAgent>();
				agentsAtLink.put( linkId , ps );
			}

			return ps;
		}
	}
}

