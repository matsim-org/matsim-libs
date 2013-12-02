/* *********************************************************************** *
 * project: org.matsim.*
 * SimplisticRelocatorManagerEngine.java
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
package eu.eunoiaproject.bikesharing.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Must be added as first activity handler!
 *
 * This class is meant for demonstration purposes. It shows how one can implement
 * an engine which monitors the state of bike sharing stations and manages a set
 * of truck driver agents.
 *
 * It is not meant to represent a useful relocation strategy! Agents just hop from
 * random station to random station, trying to set the number of bikes in the stations
 * to the half of the capacity.
 *
 * Though the strategy itself is pretty stupid, it shows how one can provide on-the-fly
 * relocation decisions (ie which bikes to relocate where and when is not predetermined,
 * and can depend on what happens in the simualtion in a reactive way).
 *
 * @author thibautd
 */
public class SimplisticRelocatorManagerEngine implements MobsimEngine, ActivityHandler {
	private final Random random;
	private final BikeSharingManager facilities;
	private final VehicleType vehicleType;

	private final int nAgents;
	private final List<SimplisticRelocationAgent> agents = new ArrayList<SimplisticRelocationAgent>();
	private final List<SimplisticRelocationAgent> idleAgents = new ArrayList<SimplisticRelocationAgent>();

	private final TripRouter tripRouter;
	private InternalInterface internalInterface;

	public SimplisticRelocatorManagerEngine(
			final int nAgents,
			final TripRouter tripRouter,
			final BikeSharingManager facilities) {
		this.nAgents = nAgents;
		this.facilities = facilities;
		this.random = MatsimRandom.getLocalInstance();
		this.tripRouter = tripRouter;

		this.vehicleType = VehicleUtils.getDefaultVehicleType();
	}

	@Override
	public void doSimStep(final double time) {
		// for all "idle" (ie non-traveling) agents,
		// handle the stations at the current link, and depart for a new random station.
		for ( SimplisticRelocationAgent agent : idleAgents ) {
			// send somewhere
			final Id linkId = agent.getCurrentLinkId();
			final Collection<? extends StatefulBikeSharingFacility> originFacilities =
				facilities.getFacilitiesAtLinks().get( linkId );

			for ( StatefulBikeSharingFacility facility : originFacilities ) {
				final int differenceToDesired = (int)
					(facility.getNumberOfBikes() - (facility.getCapacity() / 2d) );

				if ( differenceToDesired < 0 ) {
					// need for additional bikes at station
					final int unLoaded = agent.unloadBikes( -differenceToDesired );
					facilities.putBikes(
							facility.getId(),
							unLoaded );
				}
				if ( differenceToDesired > 0 ) {
					// take bikes from station and load them in truck
					facilities.takeBikes(
							facility.getId(),
							differenceToDesired );
					agent.loadBikes( differenceToDesired );
				}
			}

			final StatefulBikeSharingFacility destinationFacility =
				facilities.getFacilities().values().toArray(
						new StatefulBikeSharingFacility[ facilities.getFacilities().size() ] )
							[ random.nextInt( facilities.getFacilities().size() ) ];
			final List<? extends PlanElement> trip =
				tripRouter.calcRoute(
						TransportMode.car,
						new LinkIdFacility( agent.getCurrentLinkId() ),
						destinationFacility,
						time,
						null );
			if ( trip.size() != 1 ) throw new RuntimeException( "cannot handle complex car trips" );

			final Route route = ((Leg) trip.get( 0 )).getRoute();

			agent.setNextDestination(
					destinationFacility,
					((NetworkRoute) route).getLinkIds() );

			// "re-insert" agent in mobsim
			internalInterface.arrangeNextAgentState( agent );
		}
	}

	@Override
	public void onPrepareSim() {
		final QSim qSim = (QSim) internalInterface.getMobsim();

		// could be done smarter by departing from a bike sharing station.
		final Id[] linkIds =
				qSim.getNetsimNetwork().getNetwork().getLinks().keySet().toArray(
						new Id[0] );

		// create n agents, located on random links, and idle
		for ( int i = 0; i < nAgents; i++ ) {
			final Id agentId = new IdImpl( "relocation-truck-"+i );
			final Id linkId = linkIds[ random.nextInt( linkIds.length ) ];

			final SimplisticRelocationAgent agent =
				new SimplisticRelocationAgent(
						agentId,
						linkId );

			agents.add( agent );
			idleAgents.add( agent );

			// we need to create a vehicle at the departure point
			qSim.createAndParkVehicleOnLink(
					VehicleUtils.getFactory().createVehicle(
						agentId,
						vehicleType ),
					linkId );

		}
	}

	@Override
	public void afterSim() {}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public boolean handleActivity(final MobsimAgent agent) {
		if ( agents.contains( agent ) ) {
			// "catch" our agents to "remove" them temporarily from the simulation
			idleAgents.add( (SimplisticRelocationAgent) agent );
			return true;
		}
		return false;
	}

	private static class LinkIdFacility implements Facility {
		private final Id linkId;

		public LinkIdFacility(final Id linkId) {
			this.linkId = linkId;
		}

		@Override
		public Coord getCoord() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getId() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id getLinkId() {
			return linkId;
		}
	}
}

