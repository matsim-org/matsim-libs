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
package playground.thibautd.initialdemandgeneration.empiricalsocnet;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.osgeo.proj4j.parser.Proj4Keyword.a;
import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.e;

/**
 * @author thibautd
 */
public class SocialNetworkSampler {
	private final Random random = MatsimRandom.getLocalInstance();

	private final Population population;
	private final DegreeDistribution degreeDistribution;
	private final CliquesFiller cliquesFiller;

	@Inject
	public SocialNetworkSampler(
			final Population population,
			final DegreeDistribution degreeDistribution,
			final CliquesFiller cliquesFiller ) {
		this.population = population;
		this.degreeDistribution = degreeDistribution;
		this.cliquesFiller = cliquesFiller;
	}

	public SocialNetwork sampleSocialNetwork() {
		final Map<Id<Person>,Ego> egos = new HashMap<>();
		final List<Ego> egosWithFreeStubs = new ArrayList<>( population.getPersons().size() );
		for ( Person p : population.getPersons().values() ) {
			final Ego ego = new Ego( p , degreeDistribution.sampledDegree( p ) );
			egos.put( p.getId() , ego );
			egosWithFreeStubs.add( ego );
		}

		while ( !egosWithFreeStubs.isEmpty() ) {
			final Ego ego = egosWithFreeStubs.get( random.nextInt( egosWithFreeStubs.size() ) );

			final Set<Ego> clique = cliquesFiller.sampleClique( ego );

			for ( Ego member : clique ) {
				if ( member.degree <= member.alters.size() ) {
					egosWithFreeStubs.remove( member );
				}
			}
		}

		return new SocialNetwork() {
			@Override
			public void addEgo( final Id<Person> id ) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void addEgos( final Iterable<? extends Id<Person>> ids ) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void addBidirectionalTie( final Id<Person> id1, final Id<Person> id2 ) {
				throw new UnsupportedOperationException();

			}

			@Override
			public void addMonodirectionalTie( final Id<Person> ego, final Id<Person> alter ) {
				throw new UnsupportedOperationException();

			}

			@Override
			public Set<Id<Person>> getAlters( final Id<Person> ego ) {
				return egos.get( ego ).alters
						.stream()
						.map( Ego::getId )
						.collect( Collectors.toSet() );
			}

			@Override
			public Set<Id<Person>> getEgos() {
				return egos.keySet();
			}

			@Override
			public Map<Id<Person>, Set<Id<Person>>> getMapRepresentation() {
				return egos.values().stream()
						.collect( Collectors.toMap(
								Ego::getId,
								e -> e.alters.stream()
										.map( Ego::getId )
										.collect( Collectors.toSet() ) ));
			}

			@Override
			public boolean isReflective() {
				return true;
			}

			private final Map<String, String> metadata = new HashMap<>();
			@Override
			public Map<String, String> getMetadata() {
				return metadata;
			}

			@Override
			public void addMetadata( final String att, final String value ) {
				metadata.put( att , value );
			}
		};
	}

	public interface DegreeDistribution {
		int sampledDegree( Person person );
	}
}
