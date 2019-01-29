/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToSubtourAtGroupLevelAlgorithm.java
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
package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;

import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.sharedvehicles.SharedVehicleUtils;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

/**
 * Allocates vehicles to legs with vehicle ressources, trying to minimize
 * number of agents using the same vehicle.
 * Each agent gets allocated exactly one vehicle.
 * No attempt is made to optimize the allocation given time.
 * @author thibautd
 */
public class AllocateVehicleToPlansInGroupPlanAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private static final Logger log =
		Logger.getLogger(AllocateVehicleToPlansInGroupPlanAlgorithm.class);

	private final Random random;
	private final VehicleRessources vehicleRessources;
	private final Collection<String> modes;
	private final boolean allowNullRoutes;
	private final boolean preserveVehicleAllocations;

	/**
	 * @param random the random generator to use
	 * @param vehicleRessources the vehicles
	 * @param modes the modes for which to allocate vehicles
	 * @param allowNullRoutes if true, when a leg of mode
	 * <tt>mode</tt> has a null route, a new empty route
	 * will be created to receive the vehicle Id. If false,
	 * a null route will result in a exception.
	 */
	public AllocateVehicleToPlansInGroupPlanAlgorithm(
			final Random random,
			final VehicleRessources vehicleRessources,
			final Collection<String> modes,
			final boolean allowNullRoutes,
			final boolean preserveVehicleAllocations) {
		this.random = random;
		this.vehicleRessources = vehicleRessources;
		this.modes = modes;
		this.allowNullRoutes = allowNullRoutes;
		this.preserveVehicleAllocations = preserveVehicleAllocations;
	}

	@Override
	public void run(final GroupPlans plan) {
		final Map<Id, Integer> preservedVehiclesCounts = new HashMap<Id, Integer>();
		final List<Plan> plansWithVehicles =
			getPlansWithVehicles(
					preservedVehiclesCounts,
					plan );

		allocateOneVehiclePerPlan( plansWithVehicles , preservedVehiclesCounts );
	}

	private List<Plan> getPlansWithVehicles(
			final Map<Id, Integer> preservedVehiclesCounts,
			final GroupPlans groupPlan) {
		final List<Plan> plans = new ArrayList<Plan>();
		final List<Plan> preservedPlans = new ArrayList<Plan>();

		for ( Plan p : groupPlan.getAllIndividualPlans() ) {
			if ( hasVehicle( p ) ) plans.add( p );
			else preservedPlans.add( p );
		}

		if ( preserveVehicleAllocations ) {
			for ( Plan p : preservedPlans ) {
				final Set<Id> vs = SharedVehicleUtils.getVehiclesInPlan( p , modes );
				for ( Id v : vs ) {
					final Integer c = preservedVehiclesCounts.get( v );
					preservedVehiclesCounts.put(
							v,
							c == null ? 1 : c + 1 );
				}
			}
		}

		return plans;
	}

	private boolean hasVehicle(final Plan p) {
		for ( PlanElement pe : p.getPlanElements() ) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg leg = (Leg) pe;

			if ( !modes.contains( leg.getMode() ) ) continue;
			if ( !( ( allowNullRoutes && leg.getRoute() == null ) ||
					( leg.getRoute() instanceof NetworkRoute ) ) ) {
				throw new RuntimeException( "route for mode "+leg.getMode()+" has non-network route "+leg.getRoute() );
			}
			if ( !preserveVehicleAllocations ||
					leg.getRoute() == null ||
					((NetworkRoute) leg.getRoute()).getVehicleId() == null ) {
				return true;
			}
		}
		return false;
	}

	private void allocateOneVehiclePerPlan(
			final List<Plan> plansWithVehicles,
			final Map<Id, Integer> preservedVehiclesCounts) {
		// make the allocation random by shuffling the order in which the plans
		// are examined
		Collections.shuffle( plansWithVehicles , random );

		final Map<Id, Id> allocation = new LinkedHashMap<Id, Id>();
		allocate( plansWithVehicles , allocation , preservedVehiclesCounts );

		for ( Plan p : plansWithVehicles ) {
			final Id v = allocation.get( p.getPerson().getId() );
			assert v != null;

			for ( PlanElement pe : p.getPlanElements() ) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg leg = (Leg) pe;

				if ( !modes.contains( leg.getMode() ) ) continue;

				if ( allowNullRoutes && leg.getRoute() == null ) {
					// this is not so nice...
					leg.setRoute( new VehicleOnlyNetworkRoute() );
				}

				if ( !( leg.getRoute() instanceof NetworkRoute ) ) {
					throw new RuntimeException( "route for mode "+leg.getMode()+" has non-network route "+leg.getRoute() );
				}

				((NetworkRoute) leg.getRoute()).setVehicleId( v );
			}
		}
	}

	private void allocate(
			final List<Plan> remainingPlans,
			final Map<Id, Id> allocation,
			final Map<Id, Integer> preservedVehiclesCounts) {
		if ( remainingPlans.isEmpty() ) return;

		final Plan currentPlan = remainingPlans.get( 0 );
		final List<Plan> newRemainingPlans =
			remainingPlans.subList(
					1,
					remainingPlans.size() );

		final Id currentPersonId = currentPlan.getPerson().getId();
		final List<Id> possibleVehicles =
			new ArrayList<Id>(
					vehicleRessources.identifyVehiclesUsableForAgent(
						currentPersonId ) );

		if ( possibleVehicles.isEmpty() ) {
			throw new RuntimeException( "no vehicle found for person "+currentPersonId );
		}

		// make sure order is deterministic
		Collections.sort( possibleVehicles );
		Collections.shuffle( possibleVehicles , random );

		// allocate the first available vehicle
		boolean foundVehicle = false;
		for ( Id v : possibleVehicles ) {
			if ( !allocation.values().contains( v ) ) {
				allocation.put( currentPersonId , v );
				foundVehicle = true;
				if ( log.isTraceEnabled() ) {
					log.trace( "found unused vehicle "+v+" for person "+currentPersonId );
				}
				break;
			}
		}

		// if all vehicles are allocated, allocate (one of) the
		// least used vehicle(s)
		if ( !foundVehicle ) {
			final Id v = findLeastUsedVehicle(
					allocation.values(),
					possibleVehicles,
					preservedVehiclesCounts );
			if (log.isTraceEnabled() ) {
				log.trace( "allocate used vehicle "+v+" for person "+currentPersonId );
			}
			allocation.put( currentPersonId , v );
		}

		allocate( newRemainingPlans , allocation , preservedVehiclesCounts );
	}

	private Id findLeastUsedVehicle(
			final Collection<Id> usedVehicles,
			final List<Id> possibleVehicles,
			final Map<Id, Integer> preservedCounts) {
		if ( possibleVehicles.isEmpty() ) return null;
		final Map<Id, Integer> counts = new HashMap<Id, Integer>( preservedCounts );
		for ( Id v : possibleVehicles ) {
			if ( !counts.containsKey( v ) ) counts.put( v , 0 ); 
		}

		for ( Id v : usedVehicles ) {
			final int c = counts.get( v );
			counts.put( v , c + 1 );
		}

		final List<Id> leastUsedVehicles = new ArrayList<Id>();
		int minUses = Collections.min( counts.values() );

		for ( Map.Entry<Id, Integer> e : counts.entrySet() ) {
			final Id v = e.getKey();
			final int c = e.getValue();

			assert c >= minUses;
			if ( c == minUses ) {
				leastUsedVehicles.add( v );
			}
		}

		if ( log.isTraceEnabled() ) {
			log.trace( "counts: "+counts );
			log.trace( "minimum usage: "+minUses );
			log.trace( "least used vehicles: "+leastUsedVehicles );
		}

		// make sure iteration order is deterministic
		Collections.sort( leastUsedVehicles );
		return leastUsedVehicles.get( random.nextInt( leastUsedVehicles.size() ) );
	}
}

