/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleBasedIncompatiblePlansIdentifierFactory.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierImpl;

/**
 * @author thibautd
 */
public class VehicleBasedIncompatiblePlansIdentifierFactory implements IncompatiblePlansIdentifierFactory {
	private final String mode;

	public VehicleBasedIncompatiblePlansIdentifierFactory(final String mode) {
		this.mode = mode;
	}

	@Override
	public IncompatiblePlansIdentifier createIdentifier(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		final IncompatiblePlansIdentifierImpl identifier = new IncompatiblePlansIdentifierImpl();

		final Map<Id, Collection<Plan>> plansPerVehicle = new HashMap<Id, Collection<Plan>>();
		final Map<Plan, Collection<Id>> vehiclesPerPlan = new HashMap<Plan, Collection<Id>>();

		putVehicleInformationInMaps(
				group,
				plansPerVehicle,
				vehiclesPerPlan );

		for ( Map.Entry<Plan, Collection<Id>> entry : vehiclesPerPlan.entrySet() ) {
			final Plan plan = entry.getKey();
			final Collection<Id> vehs = entry.getValue();

			final Collection<Plan> incompatiblePlans = new HashSet<Plan>();
			for ( Id v : vehs ) {
				incompatiblePlans.addAll( plansPerVehicle.get( v ) );
			}
			incompatiblePlans.remove( plan );
			identifier.put( plan , incompatiblePlans );
		}

		return identifier;
	}

	private void putVehicleInformationInMaps(
			final ReplanningGroup group,
			final Map<Id, Collection<Plan>> plansPerVehicle,
			final Map<Plan, Collection<Id>> vehiclesPerPlan) {
		for ( Person person : group.getPersons() ) {
			for ( Plan plan : person.getPlans() ) {
				final Collection<Id> vehicles = SharedVehicleUtils.getVehiclesInPlan( plan , mode );
				vehiclesPerPlan.put( plan , vehicles );
				for ( Id v : vehicles ) {
					getCollection( v , plansPerVehicle ).add( plan );
				}
			}
		}
	}

	private static <K,V> Collection<V> getCollection(
			final K key,
			final Map<K, Collection<V>> map) {
		Collection<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new ArrayList<V>();
			map.put( key , coll );
		}

		return coll;
	}
}

