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

import com.google.inject.Inject;
import gnu.trove.iterator.TDoubleIterator;
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
import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.w;

/**
 * @author thibautd
 */
public class CorrectedUtilityCreator<N extends Enum<N>> {
	private final ConstrainedAccessibilityConfigGroup configGroup;
	private final Population population;
	private final ActivityFacilities alternatives;

	@Inject
	public CorrectedUtilityCreator( final ConstrainedAccessibilityConfigGroup configGroup,
			final Population population,
			final ActivityFacilities alternatives ) {
		this.configGroup = configGroup;
		this.population = population;
		this.alternatives = alternatives;
	}

	public Utility<N> createCorrectedUtility( final Utility<N> u ) {
		// initialize D_i
		// compute initial Constrained? (ex-ante)
		// compute personal Omegas
		// compute facilities Omegas
		// iterate
		//    compute Constrained
		//    update Omegas
		// until constrained set stable

		// compute correction factors: use Constrained, D, S and Omegas
		return null;
	}

	private static class Demand<N extends Enum<N>> {
		private final Map<Id<Person>, Map<String,TObjectDoubleMap<Id<ActivityFacility>>>> probas = new HashMap<>();

		public Demand( final NestedLogitModel<N> model , final Scenario scenario ) {
			for ( Person p : scenario.getPopulation().getPersons().values() ) {

				probas.put( p.getId(),
						computeChoiceProbas(
								p ,
								model ) );
			}
		}

		private Map<String,TObjectDoubleMap<Id<ActivityFacility>>> computeChoiceProbas(
				final Person p,
				final NestedLogitModel<N> model ) {
			// different choice sets, one per choice situation (availability constrained etc.)
			final Map<String, NestedChoiceSet<N>> choiceSets =
					model.getChoiceSetIdentifier().identifyChoiceSet( p );

			final Map<String,TObjectDoubleMap<Id<ActivityFacility>>> map = new HashMap<>(  );

			for ( Map.Entry<String, NestedChoiceSet<N>> choiceSet : choiceSets.entrySet() ) {
				map.put( choiceSet.getKey(),
						computeChoiceProbas(
								model,
								p,
								choiceSet.getValue() ) );
			}

			return map;
		}

		private TObjectDoubleMap<Id<ActivityFacility>> computeChoiceProbas(
				final NestedLogitModel<N> model,
				final Person p,
				final NestedChoiceSet<N> choiceSet ) {
			final TObjectDoubleMap<Id<Alternative>> utilities = new TObjectDoubleHashMap<>();
			final TObjectDoubleMap<N> nestLogsums = new TObjectDoubleHashMap<>();

			for ( Nest<N> nest : choiceSet.getNests() ) {
				final LogSumExpCalculator logsumNest = new LogSumExpCalculator( nest.getAlternatives().size() );

				for ( Alternative<N> alternative : nest.getAlternatives() ) {
					final double utility = model.getUtility().calcUtility( p , alternative );
					utilities.put( alternative.getAlternativeId() , utility );
					logsumNest.addTerm( nest.getMu_n() * utility );
				}

				nestLogsums.put( nest.getNestId(),
						(model.getMu() / nest.getMu_n()) * logsumNest.computeLogsumExp() );
			}

			final TObjectDoubleMap<Id<ActivityFacility>> probabilities = new TObjectDoubleHashMap<>(  );
			for ( Nest<N> nest : choiceSet.getNests() ) {
				final LogitProbabilityCalculator nestProbabilityCalculator = new LogitProbabilityCalculator();
				nestProbabilityCalculator.setNumeratorUtility( nestLogsums.get( nest.getNestId() ) );
				choiceSet.getNests().stream()
						.mapToDouble( a ->  nestLogsums.get( nest.getNestId() ) )
						.forEach( u -> nestProbabilityCalculator.addDenominatorUtility( u ) );
				final double nestProba = nestProbabilityCalculator.calcProbability();

				for ( Alternative<N> alternative : nest.getAlternatives() ) {
					final LogitProbabilityCalculator inNestProbabilityCalculator = new LogitProbabilityCalculator();
					inNestProbabilityCalculator.setNumeratorUtility( nest.getMu_n() * utilities.get( alternative.getAlternativeId() ) );
					nest.getAlternatives().stream()
							.mapToDouble( a -> nest.getMu_n() * utilities.get( a.getAlternativeId() ) )
							.forEach( u -> inNestProbabilityCalculator.addDenominatorUtility( u ) );

					probabilities.adjustValue(
							alternative.getAlternative().getDestination().getId(),
							nestProba * inNestProbabilityCalculator.calcProbability() );
				}
			}

			return probabilities;
		}
	}

	private static class LogitProbabilityCalculator {
		private double numeratorUtility;
		private final TDoubleList denominatorUtilities = new TDoubleArrayList();

		private double max = Double.NEGATIVE_INFINITY;

		public void setNumeratorUtility( final double numeratorUtility ) {
			this.numeratorUtility = numeratorUtility;
			this.max = Math.max( max , numeratorUtility );
		}

		public void addDenominatorUtilities( TDoubleList us ) {
			us.forEach( u -> {
				addDenominatorUtility( u );
				return true;
			} );
		}

		public void addDenominatorUtility( final double u ) {
			this.denominatorUtilities.add( u );
			this.max = Math.max( max , u );
		}

		public double calcProbability() {
			double denominator = 0;

			for ( TDoubleIterator iterator = denominatorUtilities.iterator();
					iterator.hasNext(); ) {
				denominator += Math.exp( iterator.next() - max );
			}

			return Math.exp( numeratorUtility - max ) / denominator;
		}
	}

	private static class IterationInformation {
		private final TObjectDoubleMap<Id<ActivityFacility>> demands = new TObjectDoubleHashMap<>();
		private final TObjectDoubleMap<Id<Person>> individualOmegas = new TObjectDoubleHashMap<>();
		private final Set<Id<ActivityFacility>> constrainedExPost = new HashSet<>();
	}

	public static class CorrectedUtility<N extends Enum<N>> implements Utility<N> {
		private final TObjectDoubleMap<Id<ActivityFacility>> demands = new TObjectDoubleHashMap<>();
		private final TObjectDoubleMap<Id<Person>> individualOmegas = new TObjectDoubleHashMap<>();
		private final Set<Id<ActivityFacility>> constrainedExPost = new HashSet<>();

		private final Utility<N> delegateUtility = null;
		private final String activityType = null;

		public double calcUncorrectedUtility( final Person p , final Alternative<N> a ) {
			return delegateUtility.calcUtility( p , a );
		}

		public double calcCorrectionFactor( final Person p , final Alternative<N> a ) {
			final ActivityFacility f = a.getAlternative().getDestination();

			if ( constrainedExPost.contains( f.getId() ) ) {
				final double supply = f.getActivityOptions().get( activityType ).getCapacity();
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

