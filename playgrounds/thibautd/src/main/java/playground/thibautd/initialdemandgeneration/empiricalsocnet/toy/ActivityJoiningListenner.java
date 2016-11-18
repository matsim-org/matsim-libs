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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.toy;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSampler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
@Singleton
public class ActivityJoiningListenner implements AutoCloseable {
	private static final Logger log = Logger.getLogger( ActivityJoiningListenner.class );
	private final Map<Id<Person>, Set<Clique>> cliquesPerPerson = new HashMap<>();

	private final String outputFile;

	public ActivityJoiningListenner( final Config config ) {
		this.outputFile = config.controler().getOutputDirectory()+"/allocatedFriendsAndDistances.dat";

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile ) ) {
			writer.write( "egoId\tnCliques\tdegree\tgroupId\tdistanceToCenter" );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Inject
	public void updateListenners(
			final SocialNetworkSampler sampler,
			final AutocloserModule.Closer closer ) {
		sampler.addCliqueListener( this::notifyClique );
		closer.add( this );
	}

	public void bind( final Binder binder ) {
		binder.requestInjection( this );
	}

	private int groupId = 0;
	@Override
	public void close() throws Exception {
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile , Charset.forName("UTF8") , true ) ) {
			final Counter counter = new Counter( "select joint activity participants # " );
			while ( !cliquesPerPerson.isEmpty() ) {
				counter.incCounter();
				groupId++;
				final Clique clique =
						cliquesPerPerson.values().stream()
								.findAny()
								.get()
								.stream()
								.min( (c1,c2) -> Double.compare( c1.avgDistanceToCenter ,c2.avgDistanceToCenter ) )
								.get();

				for ( Ego ego : clique.egos ) {
					final Set<Clique> cliques = cliquesPerPerson.get( ego.getId() );
					writer.newLine();
					writer.write( ego.getId()+"\t"+cliques.size()+"\t"+ego.getAlters().size()+"\t"+groupId+"\t"+clique.avgDistanceToCenter );
				}

				clique.egos.forEach( this::removeEgo );
			}
			counter.printCounter();
		}
	}

	private void removeEgo( final Ego ego ) {
		// recursively remove cliques, and egos that have no cliques from this operation
		final Collection<Clique> cliquesToRemove = cliquesPerPerson.get( ego.getId() );
		if ( cliquesToRemove == null ) return;

		for ( Clique clique : cliquesToRemove ) {
			for ( Ego member : clique.getEgos() ) {
				if ( member == ego ) continue;
				final Set<Clique> cliquesOfMember =
						cliquesPerPerson
								.getOrDefault( member.getId(), Collections.emptySet() );
				cliquesOfMember.remove( clique );
				if ( cliquesOfMember.isEmpty() ) {
					removeEgo( member );
				}
			}
		}
		cliquesPerPerson.remove( ego.getId() );
	}

	public void notifyClique( final Set<Ego> egos ) {
		final Clique c = new Clique( egos );
		for ( Ego e : egos ) {
			MapUtils.getSet( e.getId() , cliquesPerPerson ).add( c );
		}
	}

	private static class Clique {
		private final Set<Ego> egos;
		private final double avgDistanceToCenter;

		public Clique( final Set<Ego> egos ) {
			this.egos = egos;
			final Coord center =
					egos.stream()
							.map( Ego::getPerson )
							.map( p -> (Coord) p.getCustomAttributes().get( "coord" ) )
							.reduce( (c1,c2) -> new Coord( c1.getX() + c2.getX() , c1.getY() + c2.getY() ) )
							.map( c -> new Coord( c.getX() / egos.size() , c.getY() / egos.size() ) )
							.get();
			this.avgDistanceToCenter =
					egos.stream()
							.map( Ego::getPerson )
							.map( p -> (Coord) p.getCustomAttributes().get( "coord" ) )
							.mapToDouble( c -> CoordUtils.calcEuclideanDistance( c , center ) )
							.average()
							.getAsDouble();
		}

		public Set<Ego> getEgos() {
			return egos;
		}

		public double getAvgDistanceToCenter() {
			return avgDistanceToCenter;
		}
	}
}
