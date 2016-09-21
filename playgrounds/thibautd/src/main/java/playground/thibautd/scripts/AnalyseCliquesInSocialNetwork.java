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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author thibautd
 */
public class AnalyseCliquesInSocialNetwork {
	private static final int SAMPLE_SIZE = 700;

	public static void main( final String[] args ) {
		final String inputSocialNetwork = args[ 0 ];
		final String outputFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).readFile( inputSocialNetwork );
		final SocialNetwork socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );

		final List<Id<Person>> remainingEgos = new ArrayList<>( socialNetwork.getEgos() );

		final Random random = new Random();
		final Counter counter = new Counter( "analyse ego # " , " / " + SAMPLE_SIZE );
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile ) ) {
			writer.write( "egoId\talterId\tcliqueId" );

			for ( int i = 0; i < SAMPLE_SIZE; i++ ) {
				counter.incCounter();
				final Id<Person> ego = remainingEgos.remove( random.nextInt( remainingEgos.size() ) );

				final Collection<Set<Id<Person>>> cliques = identifyCliques( ego, socialNetwork );

				int cliqueNr = 0;
				for ( Set<Id<Person>> clique : cliques ) {
					final String cliqueId = ego + "-c-" + (cliqueNr++);
					for ( Id<Person> alter : clique ) {
						writer.newLine();
						writer.write( ego +"\t"+ alter +"\t"+ cliqueId );
					}
				}
			}
			counter.printCounter();
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static Collection<Set<Id<Person>>> identifyCliques(
			final Id<Person> ego,
			final SocialNetwork socialNetwork ) {
		final Set<Set<Id<Person>>> cliques = new HashSet<>();

		// start with the most minimal of cliques: individual alters
		for ( Id<Person> alter : socialNetwork.getAlters( ego ) ) {
			cliques.add( new HashSet<>( Arrays.asList( ego, alter ) ) );
		}

		// then, iteratively merge cliques, until no more merge is possible
		mergeCliques( socialNetwork, cliques );

		return cliques;
	}

	private static void mergeCliques(
			final SocialNetwork socialNetwork,
			final Set<Set<Id<Person>>> cliques ) {
		// we only try merge on results of previous merge. If a clique can be merged with a result from a previous iteration,
		// its subcomponents can also, so it was already tried
		final Set<Set<Id<Person>>> mergeCandidates = new HashSet<>( cliques );

		for ( List<Tuple<Set<Id<Person>>, Set<Id<Person>>>> toMerge = identifyMergeable( socialNetwork, mergeCandidates );
			  !toMerge.isEmpty();
			  toMerge = identifyMergeable( socialNetwork, mergeCandidates ) ) {
			mergeCandidates.clear();
			for ( Tuple<Set<Id<Person>>, Set<Id<Person>>> tuple : toMerge ) {
				cliques.remove( tuple.getFirst() );
				cliques.remove( tuple.getSecond() );

				final Set<Id<Person>> newClique = new HashSet<>();
				newClique.addAll( tuple.getFirst() );
				newClique.addAll( tuple.getSecond() );

				cliques.add( newClique );
				mergeCandidates.add( newClique );
			}
		}
	}

	private static List<Tuple<Set<Id<Person>>, Set<Id<Person>>>> identifyMergeable(
			final SocialNetwork socialNetwork,
			final Set<Set<Id<Person>>> cliques ) {
		final List<Tuple<Set<Id<Person>>,Set<Id<Person>>>> list = new ArrayList<>();

		// very naive way of doing it
		for ( Set<Id<Person>> clique1 : cliques ) {
			// TODO: only iterate on cliques after clique 1 (just not sure what the most efficient way is.
			// intuitively, first copying the set in an indexable structure would not help. And we need a set
			// to check if the merge was already performed
			boolean look = false;
			Set<Id<Person>> candidate = new HashSet<>();
			for ( Set<Id<Person>> clique2 : cliques ) {
				if ( clique2 == clique1 ) {
					look = true;
					continue;
				}
				if ( !look ) continue;

				if ( candidate == null ) candidate = new HashSet<>();
				else candidate.clear();

				candidate.addAll( clique1 );
				candidate.addAll( clique2 );

				if ( cliques.contains( candidate ) ) continue;

				if ( isClique( candidate , socialNetwork ) ) {
					list.add( new Tuple<>( clique1 , clique2 ) );
					candidate = null;
				}
			}
		}

		return list;
	}

	private static boolean isClique(
			final Collection<Id<Person>> candidate,
			final SocialNetwork socialNetwork ) {
		final Collection<Id<Person>> necessaryAlters = new HashSet<>();

		for ( Id<Person> ego : candidate ) {
			necessaryAlters.clear();
			necessaryAlters.addAll( candidate );
			necessaryAlters.remove( ego );

			if ( !socialNetwork.getAlters( ego ).containsAll( necessaryAlters ) ) return false;
		}

		return true;
	}
}

