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
package playground.thibautd.maxess.nestedlogitaccessibility;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author thibautd
 */
public class NestedLogitAccessibilityCalculator {
	// act as "measuring points", located at the coordinate of the first activity
	private final Population population;
	// "universal choice set"
	private final ActivityFacilities facilities;

	private final NestedLogitModel model;

	public NestedLogitAccessibilityCalculator(
			final Population population,
			final ActivityFacilities facilities,
			final NestedLogitModel model ) {
		this.population = population;
		this.facilities = facilities;
		this.model = model;
	}

	public TObjectDoubleMap<Id<Person>> computeAccessibilities() {
		final TObjectDoubleMap<Id<Person>> accessibilities = new TObjectDoubleHashMap<>( );

		for ( Person p : population.getPersons().values() ) {
			accessibilities.put( p.getId(), computeAccessibility( p ) );
		}

		return accessibilities;
	}

	private double computeAccessibility( Person p ) {
		final NestedChoiceSet choiceSet = model.getChoiceSetIdentifier().identifyChoiceSet( p );

		double sum = 0;
		for ( Nest nest : choiceSet.getNests() ) {
			sum += Math.exp( logSumNestUtilities( p , nest ) );
			// TODO: prevent overflow!!!
			if ( sum == Double.POSITIVE_INFINITY ) {
				throw new RuntimeException( "overflow in exponential" );
			}
		}

		return Math.log( sum ) / model.getMu();
	}

	private double logSumNestUtilities(
			final Person p,
			final Nest nest ) {
		double sum = 0;

		for ( Alternative alternative : nest.getAlternatives() ) {
			sum += Math.exp( nest.getMu_n() *
							model.getUtility().calcUtility(
								p,
								alternative ) );
		}

		return ( model.getMu() / nest.getMu_n() ) * Math.log( sum );
	}

	public interface ChoiceSetIdentifier {
		NestedChoiceSet identifyChoiceSet( Person p );
	}

	/**
	 * if other use cases, might be transformed to an interface
	 */
	public static class NestedChoiceSet {
		private final Collection<Nest> nests = new ArrayList<>();

		public Collection<Nest> getNests() {
			return nests;
		}
	}

	public static class Nest {
		private final Id<Nest> name;
		private final double mu_n;
		// if need exists, could easily be made generic (with alternatives type as a class parameter)
		private final List<Alternative> alternatives;

		public Nest( Id<Nest> name, double mu_n, List<Alternative> alternatives ) {
			this.name = name;
			this.mu_n = mu_n;
			this.alternatives = alternatives;
		}

		public Id<Nest> getNestId() {
			return name;
		}

		public List<Alternative> getAlternatives() {
			return alternatives;
		}

		public double getMu_n() {
			return mu_n;
		}
	}

	public static class Alternative {
		private final Id<Nest> nestId;
		private final Id<Alternative> alternativeId;
		private final Trip alternative;

		public Alternative( Id<Nest> nestId,
				Id<Alternative> alternativeId,
				Trip alternative ) {
			this.nestId = nestId;
			this.alternativeId = alternativeId;
			this.alternative = alternative;
		}
	}

	public interface Utility {
		double calcUtility( Person p , Alternative a );
	}

	public class NestedLogitModel {
		private final double mu;
		private final Utility utility;
		private final ChoiceSetIdentifier choiceSetIdentifier;

		public NestedLogitModel(
				final double mu,
				final Utility utility,
				final ChoiceSetIdentifier choiceSetIdentifier ) {
			this.mu = mu;
			this.utility = utility;
			this.choiceSetIdentifier = choiceSetIdentifier;
		}

		public ChoiceSetIdentifier getChoiceSetIdentifier() {
			return choiceSetIdentifier;
		}

		public double getMu() {
			return mu;
		}

		public Utility getUtility() {
			return utility;
		}
	}
}
