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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.utils.KDTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author thibautd
 */
@Singleton
public class SocialNetworkSampler {
	private static final Logger log = Logger.getLogger( SocialNetworkSampler.class );
	private final Random random = MatsimRandom.getLocalInstance();

	private final Population population;
	private final EgoCharacteristicsDistribution egoDistribution;
	private final CliquesFiller cliquesFiller;
	private final EgoLocator egoLocator;

	private Consumer<Set<Ego>> cliquesListener = (e) -> {};

	@Inject
	public SocialNetworkSampler(
			final Population population,
			final EgoCharacteristicsDistribution degreeDistribution,
			final CliquesFiller cliquesFiller,
			final EgoLocator egoLocator ) {
		this.population = population;
		this.egoDistribution = degreeDistribution;
		this.cliquesFiller = cliquesFiller;
		this.egoLocator = egoLocator;
	}

	public void addCliqueListener( final Consumer<Set<Ego>> l ) {
		cliquesListener = cliquesListener.andThen( l );
	}

	public SocialNetwork sampleSocialNetwork() {
		final Map<Id<Person>,Ego> egos = new HashMap<>();
		for ( Person p : population.getPersons().values() ) {
			final Ego ego = egoDistribution.sampleEgo( p );
			egos.put( p.getId() , ego );
		}
		final KDTree<Ego> egosWithFreeStubs = new KDTree<>( egoLocator.getDimensionality() , egoLocator );
		egosWithFreeStubs.add( egos.values() );

		log.info( "Start sampling with "+egosWithFreeStubs.size()+" egos with free stubs" );
		final Counter counter = new Counter( "Sample clique # " );
		while ( egosWithFreeStubs.size() > 1 ) {
			counter.incCounter();

			final Ego ego = egosWithFreeStubs.get( random.nextInt( egosWithFreeStubs.size() ) );

			final Set<Ego> clique = cliquesFiller.sampleClique( ego , egosWithFreeStubs );
			// cliquesFiller is allowed to choke on a agent, but it is then expected to take care
			// of updating egosWithFreeStubs itself. Not best design, try to solve that, might get messy!
			if ( clique == null ) continue;
			cliquesListener.accept( clique );

			for ( Ego member : clique ) {
				if ( cliquesFiller.stopConsidering( member ) ) {
					egosWithFreeStubs.remove( member );
				}
			}
		}
		counter.printCounter();

		// to assess what kind of damage the resolution of "conflicts" did
		final int sumPlannedDegrees =
				egos.values().stream()
					.mapToInt( Ego::getDegree )
					.sum();
		final int sumActualDegrees =
				egos.values().stream()
						.mapToInt( e -> e.getAlters().size() )
						.sum();

		log.info( "Average planned degree was "+((double) sumPlannedDegrees / egos.size()) );
		log.info( "Average actual degree is "+((double) sumActualDegrees / egos.size()) );
		log.info( "Number of excedentary ties: "+(sumActualDegrees - sumPlannedDegrees) );

		return new SampledSocialNetwork( egos );
	}

}
