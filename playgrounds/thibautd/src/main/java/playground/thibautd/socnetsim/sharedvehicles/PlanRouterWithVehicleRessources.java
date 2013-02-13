/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouterWithVehicleRessources.java
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
package playground.thibautd.socnetsim.sharedvehicles;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.router.JointPlanRouter;

/**
 * @author thibautd
 */
public class PlanRouterWithVehicleRessources implements PlanAlgorithm {
	private final Random random;
	private final PlanAlgorithm delegate;
	private final VehicleRessources ressources;

	public PlanRouterWithVehicleRessources(
			final VehicleRessources ressources,
			final TripRouter tripRouter) {
		this( ressources,
				new JointPlanRouter(
					tripRouter) );
	}

	public PlanRouterWithVehicleRessources(
			final VehicleRessources ressources,
			final TripRouter tripRouter,
			final ActivityFacilities facilities) {
		this( ressources,
				new JointPlanRouter(
					tripRouter,
					facilities) );
	}

	public PlanRouterWithVehicleRessources(
			final VehicleRessources ressources,
			final PlanAlgorithm delegate) {
		this.ressources = ressources;
		this.delegate = delegate;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public void run(final Plan plan) {
		final List<Trip> oldTrips =
			TripStructureUtils.getTrips(
					plan,
					getTripRouter().getStageActivityTypes() );

		delegate.run( plan );

		final Set<Id> vehicles = ressources.identifyVehiclesUsableForAgent( plan.getPerson().getId() );

		final List<Trip> newTrips =
			TripStructureUtils.getTrips(
					plan,
					getTripRouter().getStageActivityTypes() );

		transmitVehicleInformation(
				oldTrips,
				newTrips);

		// TODO: do this intelligently, ie knowing the vehicles allocated
		// to other persons in the joint plan, if any.
		// XXX: this does not consider the type of vehicle, ie if the simulation
		// is run with motorcycle, bikes and car ressources, a car trip could
		// be allocated a bicycle...
		randomlyAllocateMissingVehicleFields( vehicles , newTrips );
	}

	private void randomlyAllocateMissingVehicleFields(
			final Set<Id> vehicles,
			final List<Trip> trips) {
		final Id vehicleId = getRandomVehicle( vehicles );

		for (Trip t : trips) {
			for (Leg l : t.getLegsOnly()) {
				if ( !(l.getRoute() instanceof NetworkRoute) ) continue;
				final NetworkRoute r = (NetworkRoute) l.getRoute();
				if ( r.getVehicleId() == null ) {
					if ( vehicleId == null ) {
						throw new RuntimeException( "no vehicle could be found. Make sure you do not create vehicle-based trips for agents without a vehicle!" );
					}
					r.setVehicleId( vehicleId );
				}
			}
		}
	}

	private Id getRandomVehicle(final Set<Id> vehicles) {
		if ( vehicles.isEmpty() ) return null;
		final int choice = random.nextInt( vehicles.size() );
		int curr = 0;

		for (Id id : vehicles) {
			if ( curr++ == choice ) return id;
		}

		throw new RuntimeException();
	}

	private void transmitVehicleInformation(
			final List<Trip> oldTrips,
			final List<Trip> newTrips) {
		assert oldTrips.size() == newTrips.size();
		final Iterator<Trip> oldIter = oldTrips.iterator();
		final Iterator<Trip> newIter = newTrips.iterator();

		while ( oldIter.hasNext() ) {
			final Trip old = oldIter.next();
			final Trip young = newIter.next();

			final Id oldVeh = getVehicleId( old );
			for (Leg l : young.getLegsOnly()) {
				if ( !(l.getRoute() instanceof NetworkRoute) ) continue;
				((NetworkRoute) l.getRoute()).setVehicleId( oldVeh );
			}
		}
	}

	private static Id getVehicleId(final Trip old) {
		Id id = null;
		for (Leg l : old.getLegsOnly()) {
			if ( !(l.getRoute() instanceof NetworkRoute) ) continue;
			final Id currId = ((NetworkRoute) l.getRoute()).getVehicleId();

			// would be more efficient to just return the first found Id,
			// but there would be a risk of this problem poping up at some
			// point
			if ( id != null && !id.equals( currId ) ) {
				throw new RuntimeException( "cannot handle trips with multiple vehicles, such as "+id+" and "+currId+" in "+l.getRoute() );
			}

			id = currId;
		}

		return id;
	}

	public TripRouter getTripRouter() {
		if (delegate instanceof PlanRouter) {
			return ((PlanRouter) delegate).getTripRouter();
		}
		if (delegate instanceof JointPlanRouter) {
			return ((JointPlanRouter) delegate).getTripRouter();
		}
		throw new IllegalStateException( ""+delegate.getClass() );
	}
}

