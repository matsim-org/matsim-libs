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

		final Random random = new Random( 123 );
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

		for ( Set<Set<Id<Person>>> mergeCandidates = mergeIteration( socialNetwork , cliques , new HashSet<>( cliques ) );
			  !mergeCandidates.isEmpty();
			  mergeCandidates = mergeIteration( socialNetwork , mergeCandidates , cliques ) );
	}

	private static Set<Set<Id<Person>>> mergeIteration(
			final SocialNetwork socialNetwork,
			final Set<Set<Id<Person>>> mergeCandidates,
			final Set<Set<Id<Person>>> cliques ) {
		final Set<Set<Id<Person>>> newMergeCandidates = new HashSet<>();

		// very naive way of doing it
		for ( Set<Id<Person>> clique1 : mergeCandidates ) {
			// TODO: only iterate on cliques after clique 1 (just not sure what the most efficient way is.
			// intuitively, first copying the set in an indexable structure would not help. And we need a set
			// to check if the merge was already performed
			boolean look = false;
			for ( Set<Id<Person>> clique2 : mergeCandidates ) {
				if ( clique2 == clique1 ) {
					look = true;
					continue;
				}
				if ( !look ) continue;

				if ( isClique( clique1 , clique2 , socialNetwork ) ) {
					cliques.remove( clique1 );
					cliques.remove( clique2 );

					// avoid creating a new set
					clique1.addAll( clique2 );
					newMergeCandidates.add( clique1 );

					cliques.add( clique1 );
				}
			}
		}

		return newMergeCandidates;
	}

	private static boolean isClique(
			final Collection<Id<Person>> merge1,
			final Collection<Id<Person>> merge2,
			final SocialNetwork socialNetwork ) {
		// only need to check links between the two cliques to merge (we know they are cliques)
		for ( Id<Person> ego : merge1 ) {
			final Set<Id<Person>> alters = socialNetwork.getAlters( ego );

			for ( Id<Person> cliqueMember : merge2 ) {
				if ( ego == cliqueMember ) continue;
				if ( !alters.contains( cliqueMember ) ) return false;
			}
		}

		return true;
	}
}

