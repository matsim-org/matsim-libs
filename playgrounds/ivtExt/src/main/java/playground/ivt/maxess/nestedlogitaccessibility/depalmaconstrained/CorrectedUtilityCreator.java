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
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedLogitModel;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static org.osgeo.proj4j.parser.Proj4Keyword.a;

/**
 * @author thibautd
 */
public class CorrectedUtilityCreator<N extends Enum<N>> {
	private static final Logger log = Logger.getLogger( CorrectedUtilityCreator.class );
	private final ConstrainedAccessibilityConfigGroup configGroup;
	private final Scenario scenario;
	private final String activityType;

	@Inject
	public CorrectedUtilityCreator(
			final ConstrainedAccessibilityConfigGroup configGroup,
			final Scenario scenario,
			final String activityType ) {
		this.configGroup = configGroup;
		this.scenario = scenario;
		this.activityType = activityType;
	}

	public CorrectedUtilityCreator(
			final Scenario scenario,
			final String activityType ) {
		this(
				(ConstrainedAccessibilityConfigGroup) scenario.getConfig().getModule( ConstrainedAccessibilityConfigGroup.GROUP_NAME ),
				scenario,
				activityType );
	}

	/**
	 * Creates a utility function taking constraints into account the de Palma et Al 2007 way.
	 * This assumes the generated choice sets are stable!
	 */
	public CorrectedUtility<N> createCorrectedUtility( final NestedLogitModel<N> model ) {
		// TODO check that supply can acomodate demand (otherwise algorithm will never converge)
		// initialize D_i
		final Demand<N> demand = new Demand<>( model , scenario );
		// this initializes omegas to 1 and derives the constrained set
		final IterationInformation iterationInformation = new IterationInformation( demand );

		//final Counter counter = new Counter( "Compute capacity-constrained utility correction: iteration # ");
		int iteration = 0;
		for ( int lastSize = -1,
			  	newSize = iterationInformation.constrainedExPost.size();
				// TODO: less restrictive criterion?
				lastSize != newSize; ) {
			log.info( "Iteration "+(iteration++)+": constrained set has size "+newSize );
			//counter.incCounter();
			// update omegas based on the constrained set
			iterationInformation.updateIndividualOmegas( demand );
			// update constrained set based on new corrected choice probabilities
			iterationInformation.updateConstrained( demand );

			lastSize = newSize;
			newSize = iterationInformation.constrainedExPost.size();
		}
		//counter.printCounter();

		// compute correction factors: use Constrained, D, S and Omegas
		return new CorrectedUtility<>(
				demand,
				iterationInformation.individualOmegas,
				iterationInformation.constrainedExPost,
				model.getUtility(),
				activityType,
				configGroup );
	}

	private class IterationInformation {
		private final TObjectDoubleMap<Id<Person>> individualOmegas = new TObjectDoubleHashMap<>();
		private final Set<Id<ActivityFacility>> constrainedExPost = ConcurrentHashMap.newKeySet();

		IterationInformation( Demand<?> demand ) {
			log.info( "initialize iteration information");
			for ( Id<Person> personId : scenario.getPopulation().getPersons().keySet() ) {
				individualOmegas.put( personId , 1 );
			}
			updateConstrained( demand );
		}

		void updateConstrained( final Demand<?> demand ) {
			log.debug( "update constrained set" );
			final Map<Id<ActivityFacility>, TObjectDoubleMap<Id<Person>>> demandPerFacility = demand.getDemandPerFacility();

			// Trick to be able to set the number of desired threads. see http://stackoverflow.com/q/21163108
			final ForkJoinPool fjp = new ForkJoinPool( scenario.getConfig().global().getNumberOfThreads() );

			fjp.submit( () -> {
				demandPerFacility.entrySet().parallelStream().forEach( entry -> {
					// constrained set can only grow --- do not bother if constrained in previous iteration
					if ( constrainedExPost.contains( entry.getKey() ) ) return;

					final ActivityFacility f = scenario.getActivityFacilities().getFacilities().get( entry.getKey() );
					final double supply = getSupply( f, activityType, configGroup );

					double correctedDemand = 0;
					for ( TObjectDoubleIterator<Id<Person>> iterator = entry.getValue().iterator();
						  iterator.hasNext();
							) {
						iterator.advance();
						final double omega = individualOmegas.get( iterator.key() );
						final double proba = iterator.value();
						correctedDemand += omega * proba;
					}
					if ( supply <= correctedDemand ) constrainedExPost.add( entry.getKey() );
				} );
			}).join();
			fjp.shutdown();
		}

		void updateIndividualOmegas(
				final Demand<?> demand ) {
			log.debug( "update individual omegas" );
			final Counter counter = new Counter( "look at person # " , " / "+scenario.getPopulation().getPersons().size() );
			for ( Id<Person> p : scenario.getPopulation().getPersons().keySet() ) {
				counter.incCounter();
				final TObjectDoubleMap<Id<ActivityFacility>> probabilities =
						demand.getProbabilitiesForIndividual( p );

				final AtomicDouble sumConstrained = new AtomicDouble( 0d );
				final AtomicDouble sumUnconstrained = new AtomicDouble( 0d );

				probabilities.forEachEntry(
						(facility, probability) -> {
							if ( constrainedExPost.contains( facility ) ) {
								final ActivityFacility f = scenario.getActivityFacilities().getFacilities().get( facility );
								final double facilitySupply = getSupply( f, activityType, configGroup );
								final double facilityDemand = demand.getDemand( facility );

								// wrong: need to correct demand with omega
								//assert facilityDemand >= facilitySupply : facilityDemand+" < "+facilitySupply;

								sumConstrained.addAndGet( (facilitySupply / facilityDemand ) * probability );
							}
							else {
								sumUnconstrained.addAndGet( probability );
							}
							return true;
						}
				);


				if ( sumUnconstrained.get() == 0d ) {
					log.error( "UNABLE TO PROCEED:\n"+
								 "\tThe unconstrained set is empty for person "+p+", resulting in an infinite \'omega\'.\n"+
								 "\tThe paper avoids this by checking that supply is higher than demand, but we use here restricted choice\n"+
								 "\tsets, that make that a person might only have constrained alternatives in its choice set.\n"+
								 "\tThe only solution right now is to increase the size of the choice set..." );
					throw new RuntimeException( "unable to proceed" );
				}

				assert sumUnconstrained.get() <= 1 : sumUnconstrained.get();
				assert sumUnconstrained.get() >= 0 : sumUnconstrained.get();

				assert sumConstrained.get() <= 1 : sumConstrained.get();
				assert sumConstrained.get() >= 0 : sumConstrained.get();

				individualOmegas.put( p , (1 - sumConstrained.get()) / sumUnconstrained.get() );
			}
			counter.printCounter();
		}
	}

	private static double getSupply( final ActivityFacility f,
			final String activityType,
			final ConstrainedAccessibilityConfigGroup configGroup ) {
		final double scaledSupply = f.getActivityOptions().get( activityType ).getCapacity() * configGroup.getCapacityScalingFactor();

		if ( scaledSupply < 0 || Double.isNaN( scaledSupply ) ) {
			throw new RuntimeException( "invalid supply for facility "+f+" and type "+activityType+": "+scaledSupply );
		}

		return scaledSupply;
	}

	public static class CorrectedUtility<N extends Enum<N>> implements Utility<N> {
		private final TObjectDoubleMap<Id<ActivityFacility>> demands;
		private final TObjectDoubleMap<Id<Person>> individualOmegas;
		private final Set<Id<ActivityFacility>> constrainedExPost;

		private final Utility<N> delegateUtility;
		private final String activityType;

		private final ConstrainedAccessibilityConfigGroup configGroup;

		private CorrectedUtility(
				final Demand<N> demand,
				final TObjectDoubleMap<Id<Person>> individualOmegas,
				final Set<Id<ActivityFacility>> constrainedExPost,
				final Utility<N> delegateUtility,
				final String activityType,
				final ConstrainedAccessibilityConfigGroup configGroup ) {
			this.demands = demand.getSummedDemandPerFacility();
			this.individualOmegas = individualOmegas;
			this.constrainedExPost = constrainedExPost;
			this.delegateUtility = delegateUtility;
			this.activityType = activityType;
			this.configGroup = configGroup;
		}

		public double calcUncorrectedUtility( final Person p , final Alternative<N> a ) {
			return delegateUtility.calcUtility( p , a );
		}

		public double calcCorrectionFactor( final Person p , final Alternative<N> a ) {
			final ActivityFacility f = a.getAlternative().getDestination();

			if ( constrainedExPost.contains( f.getId() ) ) {
				final double supply = getSupply( f, activityType, configGroup );
				final double demand = demands.get( f.getId() );
				return Math.log( supply / demand );
			}

			final double omega =  individualOmegas.get( p.getId() );

			if ( Double.isInfinite( omega ) || Double.isNaN( omega ) || omega <= 0d ) {
				throw new RuntimeException( "invalid omega for individual "+p.getId()+": "+omega );
			}

			return Math.log( omega );
		}


		@Override
		public double calcUtility( final Person p, final Alternative<N> a ) {
			return calcUncorrectedUtility( p , a ) + calcCorrectionFactor( p , a );
		}
	}
}

