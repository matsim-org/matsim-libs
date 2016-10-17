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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.utils.CsvParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author thibautd
 */
public class SnowballCliques {
	private static final Logger log = Logger.getLogger( SnowballCliques.class );
	private final Map<Id<Clique>,Clique> cliques = new HashMap<>();
	private final Map<Id<Member>,Set<Clique>> cliquesPerEgo = new HashMap<>();

	private final List<Member> egos = new ArrayList<>();

	public Map<Id<Clique>, Clique> getCliques() {
		return cliques;
	}

	public Map<Id<Member>, Set<Clique>> getCliquesPerEgo() {
		return cliquesPerEgo;
	}

	public List<Member> getEgos() {
		return egos;
	}

	public static SnowballCliques readCliques( final String file ) {
		final CoordinateTransformation transformation =
				TransformationFactory.getCoordinateTransformation(
						TransformationFactory.WGS84,
						TransformationFactory.CH1903_LV03_GT );

		final SnowballCliques cliques = new SnowballCliques();
		try ( final CsvParser parser = new CsvParser( ',' , '\"' , file ) ) {
			while ( parser.nextLine() ) {
				final Id<Clique> cliqueId = parser.getIdField( "Clique_ID" , Clique.class );

				final Id<Member> egoId = parser.getIdField( "E_ID" , Member.class );
				final Sex egoSex = parser.getEnumField( "E_sex" , Sex.class );
				final double egoLatitude = parser.getDoubleField( "E_latitude" );
				final double egoLongitude = parser.getDoubleField( "E_longitude" );
				final int egoAge = parser.getIntField( "E_age" );
				final int egoDegree = parser.getIntField( "E_degree" );

				final Id<Member> alterId = parser.getIdField( "A_Alter_ID" , Member.class );
				final Sex alterSex = parser.getEnumField( "A_sex" , Sex.class );
				final double alterLatitude = parser.getDoubleField( "A_latitude" );
				final double alterLongitude = parser.getDoubleField( "A_longitude" );
				final int alterAge = parser.getIntField( "A_age" );

				final Coord egoCoord = transformation.transform( new Coord( egoLongitude , egoLatitude ) );
				final Coord alterCoord = transformation.transform( new Coord( alterLongitude , alterLatitude ) );

				Clique clique = cliques.cliques.get( cliqueId );
				if ( clique == null ) {
					final Member ego = new Member( egoId , egoSex , egoCoord , egoAge , egoDegree );
					clique = new Clique( cliqueId , ego );
					cliques.cliques.put( cliqueId , clique );
					cliques.egos.add( ego );
					MapUtils.getSet( egoId , cliques.cliquesPerEgo ).add( clique );
				}
				clique.alters.add( new Member( alterId , alterSex , alterCoord , alterAge , -1 ) );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

		cliques.cleanupCliques();
		return cliques;
	}

	private void cleanupCliques() {
		log.info( "Start cleaning up clique composition: ");
		log.info( "Cliques that are a subset of other cliques of the same ego will be removed" );

		final Counter removalCounter = new Counter( "Remove redundant subclique # " , " / "+cliques.size()+" cliques" );
		final Counter counter = new Counter( "Analyse ego # " , " / "+cliquesPerEgo.size() );

		for ( Set<Clique> cliquesOfEgo : cliquesPerEgo.values() ) {
			counter.incCounter();

			final Set<Clique> redundant = new HashSet<>();
			for ( Clique clique : cliquesOfEgo ) {
				if ( isRedundant( clique , cliquesOfEgo ) ) {
					removalCounter.incCounter();
					redundant.add( clique );
				}
			}

			cliquesOfEgo.removeAll( redundant );
			for ( Clique c : redundant ) cliques.remove( c.getCliqueId() );
		}
		removalCounter.printCounter();
		counter.printCounter();
		log.info( "Finished cleaning up clique composition: ");
		log.info( cliques.size()+" cliques remain." );
		assert cliques.size() == cliquesPerEgo.values().stream().mapToInt( Set::size ).sum();
	}

	private static boolean isRedundant( final Clique clique , final Collection<Clique> in ) {
		for ( Clique other : in ) {
			if ( clique == other ) continue;
			if ( isSubset( clique , other ) ) {
				if ( clique.getAlters().size() == other.getAlters().size() ) {
					// avoid removing all duplicates: arbitrary criterion to keep one
					return clique.getCliqueId().compareTo( other.getCliqueId() ) > 0;
				}
				return true;
			}
		}
		return false;
	}

	private static boolean isSubset( final Clique subset , final Clique of ) {
		return of.getEgo().getId().equals( of.getEgo().getId() ) &&
				of.alters.containsAll( subset.alters );
	}

	public enum Sex { female, male }

	public static class Clique {
		private final Id<Clique> cliqueId;
		private final Member ego;
		private final List<Member> alters = new ArrayList<>();
		private final List<Member> unmodifiableAlters = Collections.unmodifiableList( alters );

		public Clique( final Id<Clique> cliqueId, final Member ego ) {
			this.cliqueId = cliqueId;
			this.ego = ego;
		}

		public Id<Clique> getCliqueId() {
			return cliqueId;
		}

		public Member getEgo() {
			return ego;
		}

		public List<Member> getAlters() {
			return unmodifiableAlters;
		}
	}

	public static class Member {
		private final Id<Member> id;
		private final Sex sex;
		private final Coord coord;
		private final int age;
		private final int degree;

		private Member(
				final Id<Member> id,
				final Sex sex,
				final Coord coord,
				final int age,
				final int degree ) {
			this.id = id;
			this.sex = sex;
			this.coord = coord;
			this.age = age;
			this.degree = degree;
		}

		public Sex getSex() {
			return sex;
		}

		public Coord getCoord() {
			return coord;
		}

		public int getAge() {
			return age;
		}

		public int getDegree() {
			if ( degree < 0 ) throw new IllegalStateException( "no degree known" );
			return degree;
		}

		public Id<Member> getId() {
			return id;
		}

		@Override
		public boolean equals( Object o ) {
			if ( o == null ) return false;
			if ( !( o instanceof  Member ) ) return false;
			final Member m = (Member) o;
			// if dataset is consistent this should be fine.
			return id.equals( m.id );
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}
}
