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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author thibautd
 */
@Singleton
public class SocialPositions {
	private final int[] ageCuttingPoints;

	@Inject
	private SocialPositions(
			final SnowballSamplingConfigGroup configGroup ) {
		this.ageCuttingPoints = Arrays.copyOf( configGroup.getAgeCuttingPoints() , configGroup.getAgeCuttingPoints().length );
		Arrays.sort( ageCuttingPoints );
	}

	public CliquePosition calcPosition( final SnowballCliques.Member ego, final SnowballCliques.Member alter ) {
		return new CliquePosition(
				CoordUtils.calcEuclideanDistance( ego.getCoord() , alter.getCoord() ),
				calcBearing( ego.getCoord() , alter.getCoord() ),
				Math.abs( calcAgeClass( ego.getAge() ) - calcAgeClass( alter.getAge() ) ),
				ego.getSex() == alter.getSex() );
	}

	private static double calcNormalizedXDifference( final Coord coord1, final Coord coord2 ) {
		final double distance = CoordUtils.calcProjectedEuclideanDistance( coord1 , coord2 );
		if ( distance <= 1E-7 ) return 0;

		final double normalized = (coord2.getX() - coord1.getX()) / distance;

		if ( normalized > 1 && normalized < 1 + 1E-7 ) return 1;
		if ( normalized < -1 && normalized > -1 - 1E-7 ) return 1;
		assert normalized >= -1 && normalized <= 1;
		return normalized;
	}

	private static double calcBearing( final Coord coord1, final Coord coord2 ) {
		final double normalizedXDiff = calcNormalizedXDifference( coord1, coord2 );
		final double sign = coord2.getY() > coord1.getY() ? 1 : -1;

		return sign * Math.acos( normalizedXDiff );
	}

	public int calcAgeClass( final int age ) {
		final int ageClass = Arrays.binarySearch( ageCuttingPoints , age );
		return ageClass >= 0 ? ageClass : 1 - ageClass;
	}

	public static int calcDegreeClass( final int degree ) {
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


	public static void group( final Ego ego, final Set<Ego> members ) {
		for ( Ego alter : members ) {
			alter.getAlters().add( ego );
			ego.getAlters().add( alter );
		}
		members.add( ego );
	}


	public static class CliquePositions implements Iterable<CliquePosition> {
		public final List<CliquePosition> positions = new ArrayList<>();

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
