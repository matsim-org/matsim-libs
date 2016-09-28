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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.simplesnowball;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.utils.KDTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author thibautd
 */
@Singleton
public class SimpleCliquesFiller implements CliquesFiller {
	private static final Logger log = Logger.getLogger( SimpleCliquesFiller.class );
	private static final int[] AGE_CUTTING_POINTS = {24, 38, 51, 66, Integer.MAX_VALUE};

	private final Random random = MatsimRandom.getLocalInstance();
	private final Map<EgoClass, List<CliquePositions>> cliques = new HashMap<>(  );
	private final List<CliquePositions> allCliques = new ArrayList<>();

	private Set<EgoClass> knownEmptyClasses = new HashSet<>();

	public interface Position {
		double[] calcPosition( Ego center , CliquePosition position );
	}

	private final Position position;

	@Inject
	public SimpleCliquesFiller(
			final SnowballCliques snowballCliques,
			final Position position ) {
		this.position = position;

		for ( SnowballCliques.Clique snowballClique : snowballCliques.getCliques().values() ) {
			final CliquePositions clique = new CliquePositions();

			MapUtils.getList( new EgoClass( snowballClique.getEgo() ) , cliques ).add( clique );
			allCliques.add( clique );

			for ( SnowballCliques.Member alter : snowballClique.getAlters() ) {
				clique.positions.add( calcPosition( snowballClique.getEgo() , alter ) );
			}
		}
	}

	private CliquePosition calcPosition( final SnowballCliques.Member ego, final SnowballCliques.Member alter ) {
		return new CliquePosition(
				CoordUtils.calcEuclideanDistance( ego.getCoord() , alter.getCoord() ),
				calcBearing( ego.getCoord() , alter.getCoord() ),
				Math.abs( calcAgeClass( ego.getAge() ) - calcAgeClass( alter.getAge() ) ),
				ego.getSex() == alter.getSex() );
	}

	private double calcBearing( final Coord coord1, final Coord coord2 ) {
		final double normalizedXDiff = calcNormalizedXDifference( coord1, coord2 );
		final double sign = coord2.getY() > coord1.getY() ? 1 : -1;

		return sign * Math.acos( normalizedXDiff );
	}

	private double calcNormalizedXDifference( final Coord coord1, final Coord coord2 ) {
		final double distance = CoordUtils.calcProjectedEuclideanDistance( coord1 , coord2 );
		if ( distance <= 1E-7 ) return 0;

		final double normalized = (coord2.getX() - coord1.getX()) / distance;

		if ( normalized > 1 && normalized < 1 + 1E-7 ) return 1;
		if ( normalized < -1 && normalized > -1 - 1E-7 ) return 1;
		assert normalized >= -1 && normalized <= 1;
		return normalized;
	}

	@Override
	public Set<Ego> sampleClique(
			final Ego ego,
			final KDTree<Ego> egosWithFreeStubs ) {
		final EgoClass egoClass = new EgoClass( ego );
		List<CliquePositions> cliqueList = cliques.get( egoClass );

		if ( cliqueList == null ) {
			if ( knownEmptyClasses.add( egoClass ) ) {
				log.warn( "No cliques for egos of class "+egoClass );
				log.warn( "Sampling unconditionned instead" );
			}
			cliqueList = allCliques;
		}

		final CliquePositions clique = cliqueList.get( random.nextInt( cliqueList.size() ) );

		final Set<Ego> members = new HashSet<>();
		members.add( ego );
		for ( CliquePosition cliqueMember : clique ) {
			// TODO: rotate?
			final double[] point = position.calcPosition( ego , cliqueMember );
			final Ego member =
					egosWithFreeStubs.getClosestEuclidean(
							point,
							(e) -> e.getFreeStubs() >= clique.size() - 1 );
			if ( member == null ) {
				throw new RuntimeException( "no alter found at "+ Arrays.toString( point )+" for clique size "+clique.size() );
			}
			group( member , members );
		}

		return members;
	}

	private void group( final Ego ego, final Set<Ego> members ) {
		for ( Ego alter : members ) {
			alter.getAlters().add( ego );
			ego.getAlters().add( alter );
		}
		members.add( ego );
	}

	public static int calcAgeClass( int age ) {
		for ( int i=0; i < AGE_CUTTING_POINTS.length; i++ ) {
			if ( age < AGE_CUTTING_POINTS[ i ] ) return i;
		}
		throw new RuntimeException( "should not reach this point" );
	}

	private static int calcDegreeClass( int degree ) {
		return degree;
	}


	public static SnowballCliques.Sex getSex( final Ego ego ) {
		final String sex = PersonUtils.getSex( ego.getPerson() );
		switch ( sex ) {
			case "f":
				return SnowballCliques.Sex.female;
			case "m":
				return SnowballCliques.Sex.male;
			default:
				throw new IllegalArgumentException( sex );
		}
	}

	private static class EgoClass {
		private final int ageClass;
		private final SnowballCliques.Sex sex;
		private final int degreeClass;

		private EgoClass(
				final int age,
				final SnowballCliques.Sex sex,
				final int degree ) {
			this.ageClass = calcAgeClass( age );
			this.sex = sex;
			this.degreeClass = calcDegreeClass( degree );
		}

		private EgoClass( final Ego ego ) {
			this( PersonUtils.getAge( ego.getPerson() ),
					getSex( ego ),
					ego.getDegree() );
		}

		public EgoClass( final SnowballCliques.Member ego ) {
			this( ego.getAge(),
					ego.getSex(),
					ego.getDegree() );
		}

		@Override
		public boolean equals( final Object o ) {
			if ( !(o instanceof EgoClass) ) return false;
			return ( (EgoClass) o ).ageClass == ageClass &&
					( (EgoClass) o ).sex == sex &&
					( (EgoClass) o ).degreeClass == degreeClass;
		}

		@Override
		public int hashCode() {
			// unique if less than 99 age classes
			int hashCode = ageClass;
			hashCode += 100 * (sex.ordinal() + 1);
			hashCode += 1000 * (degreeClass + 1);
			return hashCode;
		}

		@Override
		public String toString() {
			return "[EgoClass: ageClass="+ageClass+
					"; sex="+sex+
					"; degreeClass="+degreeClass+"]";
		}
	}

	private static class CliquePositions implements Iterable<CliquePosition> {
		private final List<CliquePosition> positions = new ArrayList<>();

		public int size() {
			// 1 for the ego
			return 1 + positions.size();
		}

		@Override
		public Iterator<CliquePosition> iterator() {
			return positions.iterator();
		}

		@Override
		public void forEach( final Consumer<? super CliquePosition> action ) {
			positions.forEach( action );
		}

		@Override
		public Spliterator<CliquePosition> spliterator() {
			return positions.spliterator();
		}
	}

	/**
	 * describes the "positions" of members of a clique in the "social space", relative the ego.
	 */
	public static class CliquePosition {
		private final double distance, bearing;
		// one could also use actual age difference (not classified),
		// but this would then require calibrating social distance more carefully
		// (how much kilometers is one year difference worth?)
		private final int ageClassDistance;
		private final boolean sameSex;

		private CliquePosition(
				final double distance,
				final double bearing,
				final int ageClassDistance,
				final boolean sameSex ) {
			this.distance = distance;
			this.bearing = bearing;
			this.ageClassDistance = ageClassDistance;
			this.sameSex = sameSex;
		}

		public double getDistance() {
			return distance;
		}

		public double getBearing() {
			return bearing;
		}

		public int getAgeClassDistance() {
			return ageClassDistance;
		}

		public boolean isSameSex() {
			return sameSex;
		}
	}
}
