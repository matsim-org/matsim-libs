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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.utils.KDTree;

import java.util.ArrayList;
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
public class SimpleCliquesFiller implements CliquesFiller {
	private static final int[] AGE_CUTTING_POINTS = {24, 38, 51, 66, Integer.MAX_VALUE};

	private final Random random = MatsimRandom.getLocalInstance();
	private final Map<EgoClass, List<CliquePositions>> cliques = new HashMap<>(  );

	public interface Position {
		double[] calcPosition( Ego center , CliquePosition position );
	}

	private final Position position;

	@Inject
	public SimpleCliquesFiller(
			final Iterable<SnowballCliques.Clique> snowballCliques,
			final Position position ) {
		this.position = position;

		for ( SnowballCliques.Clique snowballClique : snowballCliques ) {
			final CliquePositions clique = new CliquePositions();

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
		final double normalizedXDiff = coord2.getX() - coord1.getX() / CoordUtils.calcProjectedEuclideanDistance( coord1 , coord2 );
		final double sign = coord2.getY() > coord1.getY() ? 1 : -1;

		return sign * Math.acos( normalizedXDiff );
	}

	@Override
	public Set<Ego> sampleClique(
			final Ego ego,
			final KDTree<Ego> egosWithFreeStubs ) {
		final EgoClass egoClass = new EgoClass( ego );
		final List<CliquePositions> cliqueList = cliques.get( egoClass );

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

	private static int calcAgeClass( int age ) {
		for ( int i=0; i < AGE_CUTTING_POINTS.length; i++ ) {
			if ( age < AGE_CUTTING_POINTS[ i ] ) return i;
		}
		throw new RuntimeException( "should not reach this point" );
	}

	private static int calcDegreeClass( int degree ) {
		return degree;
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
					SnowballCliques.Sex.valueOf( PersonUtils.getSex( ego.getPerson() ) ),
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
	private static class CliquePosition {
		private final double distance, bearing;
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
	}
}
