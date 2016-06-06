/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.LogSumExpCalculator;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedLogitModel;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolver.iterator;
import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.s;
import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.w;

/**
 * @author thibautd
 */
public class CorrectedUtilityCreator<N extends Enum<N>> {
	private final ConstrainedAccessibilityConfigGroup configGroup;
	private final Scenario scenario;
	private final String activityType = null;

	@Inject
	public CorrectedUtilityCreator(
			final ConstrainedAccessibilityConfigGroup configGroup,
			final Scenario scenario ) {
		this.configGroup = configGroup;
		this.scenario = scenario;
	}

	/**
	 * Creates a utility function taking constraints into account the de Palma et Al 2007 way.
	 * This assumes the generated choice sets are stable!
	 */
	public CorrectedUtility<N> createCorrectedUtility( final NestedLogitModel<N> model ) {
		// initialize D_i
		final Demand<N> demand = new Demand<>( model , scenario );
		final IterationInformation iterationInformation = new IterationInformation( demand );

		// compute initial Constrained? (ex-ante)

		iterationInformation.updateIndividualOmegas( demand );

		// compute personal Omegas
		// compute facilities Omegas
		// iterate
		//    compute Constrained
		//    update Omegas
		// until constrained set stable

		// compute correction factors: use Constrained, D, S and Omegas
		return null;
	}

	private class IterationInformation {
		private final TObjectDoubleMap<Id<Person>> individualOmegas = new TObjectDoubleHashMap<>();
		private final Set<Id<ActivityFacility>> constrainedExPost = new HashSet<>();

		IterationInformation( Demand<?> demand ) {
			for ( Id<Person> personId : scenario.getPopulation().getPersons().keySet() ) {
				individualOmegas.put( personId , 1 );
			}
			updateConstrained( demand );
		}

		void updateConstrained( final Demand<?> demand ) {
			throw new UnsupportedOperationException(  );
		}

		void updateIndividualOmegas(
				final Demand<?> demand ) {
			for ( Id<Person> p : scenario.getPopulation().getPersons().keySet() ) {
				final TObjectDoubleMap<Id<ActivityFacility>> probabilities =
						demand.getProbabilitiesForIndividual( p );

				final AtomicDouble sumConstrained = new AtomicDouble( 0 );
				final AtomicDouble sumUnconstrained = new AtomicDouble( 0 );

				probabilities.forEachEntry(
						(facility, probability) -> {
							if ( constrainedExPost.contains( facility ) ) {
								final ActivityFacility f = scenario.getActivityFacilities().getFacilities().get( facility );
								final double supply = f.getActivityOptions().get( activityType ).getCapacity() * configGroup.getCapacityScalingFactor();
								sumConstrained.addAndGet( (supply / demand.getDemand( facility )) * probability );
							}
							else {
								sumUnconstrained.addAndGet( probability );
							}
							return true;
						}
				);

				individualOmegas.put( p , (1 - sumConstrained.get()) / (1 - sumUnconstrained.get()) );
			}
		}
	}

	public static class CorrectedUtility<N extends Enum<N>> implements Utility<N> {
		private final TObjectDoubleMap<Id<ActivityFacility>> demands = new TObjectDoubleHashMap<>();
		private final TObjectDoubleMap<Id<Person>> individualOmegas = new TObjectDoubleHashMap<>();
		private final Set<Id<ActivityFacility>> constrainedExPost = new HashSet<>();

		private final Utility<N> delegateUtility = null;
		private final String activityType = null;

		private final ConstrainedAccessibilityConfigGroup configGroup = null;

		public double calcUncorrectedUtility( final Person p , final Alternative<N> a ) {
			return delegateUtility.calcUtility( p , a );
		}

		public double calcCorrectionFactor( final Person p , final Alternative<N> a ) {
			final ActivityFacility f = a.getAlternative().getDestination();

			if ( constrainedExPost.contains( f.getId() ) ) {
				final double supply = f.getActivityOptions().get( activityType ).getCapacity() * configGroup.getCapacityScalingFactor();
				final double demand = demands.get( f.getId() );
				return Math.log( supply / demand );
			}

			return Math.log( individualOmegas.get( p.getId() ) );
		}

		@Override
		public double calcUtility( final Person p, final Alternative<N> a ) {
			return calcUncorrectedUtility( p , a ) + calcCorrectionFactor( p , a );
		}
	}
}

