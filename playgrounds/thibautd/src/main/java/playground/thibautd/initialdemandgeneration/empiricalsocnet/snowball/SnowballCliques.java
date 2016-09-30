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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.utils.CsvParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author thibautd
 */
public class SnowballCliques {
	private final Map<Id<Clique>,Clique> cliques = new HashMap<>();
	private final List<Member> egos = new ArrayList<>();

	public Map<Id<Clique>, Clique> getCliques() {
		return cliques;
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

				final Sex egoSex = parser.getEnumField( "E_sex" , Sex.class );
				final double egoLatitude = parser.getDoubleField( "E_latitude" );
				final double egoLongitude = parser.getDoubleField( "E_longitude" );
				final int egoAge = parser.getIntField( "E_age" );
				final int egoDegree = parser.getIntField( "E_degree" );

				final Sex alterSex = parser.getEnumField( "A_sex" , Sex.class );
				final double alterLatitude = parser.getDoubleField( "A_latitude" );
				final double alterLongitude = parser.getDoubleField( "A_longitude" );
				final int alterAge = parser.getIntField( "A_age" );

				final Coord egoCoord = transformation.transform( new Coord( egoLongitude , egoLatitude ) );
				final Coord alterCoord = transformation.transform( new Coord( alterLongitude , alterLatitude ) );

				Clique clique = cliques.cliques.get( cliqueId );
				if ( clique == null ) {
					final Member ego = new Member( egoSex , egoCoord , egoAge , egoDegree );
					clique = new Clique( cliqueId , ego );
					cliques.cliques.put( cliqueId , clique );
					cliques.egos.add( ego );
				}
				clique.alters.add( new Member( alterSex , alterCoord , alterAge , -1 ) );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

		return cliques;
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
		private final Sex sex;
		private final Coord coord;
		private final int age;
		private final int degree;

		public Member( final Sex sex, final Coord coord, final int age, final int degree ) {
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
	}
}
