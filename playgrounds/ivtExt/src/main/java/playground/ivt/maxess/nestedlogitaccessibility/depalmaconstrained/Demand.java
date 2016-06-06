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

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.LogSumExpCalculator;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedLogitModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
class Demand<N extends Enum<N>> {
	private final Map<Id<Person>, Map<String, TObjectDoubleMap<Id<ActivityFacility>>>> probas = new HashMap<>();

	public Demand( final NestedLogitModel<N> model, final Scenario scenario ) {
		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			probas.put(
					p.getId(),
					computeChoiceProbas(
							p,
							model ) );
		}
	}

	private Map<String, TObjectDoubleMap<Id<ActivityFacility>>> computeChoiceProbas(
			final Person p,
			final NestedLogitModel<N> model ) {
		// different choice sets, one per choice situation (availability constrained etc.)
		final Map<String, NestedChoiceSet<N>> choiceSets =
				model.getChoiceSetIdentifier().identifyChoiceSet( p );

		final Map<String, TObjectDoubleMap<Id<ActivityFacility>>> map = new HashMap<>();

		for ( Map.Entry<String, NestedChoiceSet<N>> choiceSet : choiceSets.entrySet() ) {
			map.put(
					choiceSet.getKey(),
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
				final double utility = model.getUtility().calcUtility( p, alternative );
				utilities.put( alternative.getAlternativeId(), utility );
				logsumNest.addTerm( nest.getMu_n() * utility );
			}

			nestLogsums.put(
					nest.getNestId(),
					( model.getMu() / nest.getMu_n() ) * logsumNest.computeLogsumExp() );
		}

		final TObjectDoubleMap<Id<ActivityFacility>> probabilities = new TObjectDoubleHashMap<>();
		for ( Nest<N> nest : choiceSet.getNests() ) {
			final LogitProbabilityCalculator nestProbabilityCalculator = new LogitProbabilityCalculator();
			nestProbabilityCalculator.setNumeratorUtility( nestLogsums.get( nest.getNestId() ) );
			choiceSet.getNests().stream()
					.mapToDouble( a -> nestLogsums.get( nest.getNestId() ) )
					.forEach( u -> nestProbabilityCalculator.addDenominatorUtility( u ) );
			final double nestProba = nestProbabilityCalculator.calcProbability();

			for ( Alternative<N> alternative : nest.getAlternatives() ) {
				final LogitProbabilityCalculator inNestProbabilityCalculator = new LogitProbabilityCalculator();
				inNestProbabilityCalculator.setNumeratorUtility( nest.getMu_n() * utilities.get( alternative.getAlternativeId() ) );
				nest.getAlternatives().stream()
						.mapToDouble( a -> nest.getMu_n() * utilities.get( a.getAlternativeId() ) )
						.forEach( u -> inNestProbabilityCalculator.addDenominatorUtility( u ) );

				final double prob = nestProba * inNestProbabilityCalculator.calcProbability();
				probabilities.adjustOrPutValue(
						alternative.getAlternative().getDestination().getId(),
						prob , prob );
			}
		}

		return probabilities;
	}
}
