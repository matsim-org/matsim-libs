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
import playground.thibautd.utils.AggregateList;
import playground.thibautd.utils.ArrayUtils;
import playground.thibautd.utils.KDTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.l;

/**
 * @author thibautd
 */
@Singleton
public class SimpleCliquesFiller implements CliquesFiller {
	private static final Logger log = Logger.getLogger( SimpleCliquesFiller.class );
	private static final int[] AGE_CUTTING_POINTS = {24, 38, 51, 66, Integer.MAX_VALUE};

	private final Random random = MatsimRandom.getLocalInstance();
	private final Map<EgoClass, CliqueSampler> cliques = new LinkedHashMap<>(  );
	private final CliqueSampler allCliques;

	private final SnowballSamplingConfigGroup configGroup;

	private final Set<EgoClass> knownEmptyClasses = new HashSet<>();

	public interface Position {
		double[] calcPosition( Ego center , CliquePosition position );
	}

	private final Position position;

	@Inject
	public SimpleCliquesFiller(
			final SnowballCliques snowballCliques,
			final SnowballSamplingConfigGroup configGroup,
			final Position position ) {
		this.configGroup = configGroup;
		this.position = position;

		final Map<EgoClass, List<CliquePositions>> cliquesMap = new LinkedHashMap<>();
		final List<CliquePositions> allCliquesList = new ArrayList<>();
		for ( SnowballCliques.Clique snowballClique : snowballCliques.getCliques().values() ) {
			final CliquePositions clique = new CliquePositions();

			MapUtils.getList( createEgoClass( snowballClique.getEgo() ) , cliquesMap ).add( clique );
			allCliquesList.add( clique );

			for ( SnowballCliques.Member alter : snowballClique.getAlters() ) {
				clique.positions.add( calcPosition( snowballClique.getEgo() , alter ) );
			}
		}

		for ( Map.Entry<EgoClass,List<CliquePositions>> e : cliquesMap.entrySet() ) {
			cliques.put( e.getKey() , new CliqueSampler( e.getValue() ) );
		}
		allCliques = new CliqueSampler( allCliquesList );
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
		final EgoClass egoClass = createEgoClass( ego );
		CliqueSampler cliqueSampler = cliques.get( egoClass );

		if ( cliqueSampler == null ) {
			if ( knownEmptyClasses.add( egoClass ) ) {
				log.warn( "No cliques for egos of class "+egoClass );
				log.warn( "Sampling unconditionned instead" );
			}
			cliqueSampler = allCliques;
		}

		final CliquePositions clique = cliqueSampler.sampleClique( random , egosWithFreeStubs.size() );

		final Set<Ego> members = new HashSet<>();
		members.add( ego );
		for ( CliquePosition cliqueMember : clique ) {
			// TODO: rotate? -> only once per clique
			final double[] point = position.calcPosition( ego , cliqueMember );
			final Ego member = findEgo( egosWithFreeStubs, clique, point , members );

			if ( member == null ) {
				throw new RuntimeException( "no alter found at "+ Arrays.toString( point )+" for clique size "+clique.size() );
			}

			group( member , members );
		}

		return members;
	}

	private Ego findEgo( final KDTree<Ego> egosWithFreeStubs,
			final CliquePositions clique,
			final double[] point,
			final Set<Ego> currentClique ) {
		// Allow more ties than stubs in case no agent can be found.
		// should remain OK, because:
		// - clique size cannot exceed number of agents with free stubs
		// - this can happen at most once per agent
		// - we keep the increase minimal, by increasingly increasing tolerance.
		for ( int i = 1; true; i++ ) {
			final int freeStubs = clique.size() - i;
			final Ego member =
					egosWithFreeStubs.getClosestEuclidean(
							point,
							( e ) -> e.getFreeStubs() >= freeStubs && !currentClique.contains( e ) );
			if ( member != null ) return member;
		}
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

	private EgoClass createEgoClass( final Ego ego ) {
		return createEgoClass( PersonUtils.getAge( ego.getPerson() ),
				getSex( ego ),
				ego.getDegree() );
	}

	private EgoClass createEgoClass( final SnowballCliques.Member ego ) {
		return createEgoClass( ego.getAge(),
				ego.getSex(),
				ego.getDegree() );
	}

	private EgoClass createEgoClass(
			final int age,
			final SnowballCliques.Sex sex,
			final int degree ) {
		final int ageClass = configGroup.isConditionCliqueSizeOnAge() ? calcAgeClass( age ) : -1;
		final SnowballCliques.Sex theSex = configGroup.isConditionCliqueSizeOnSex() ? sex : null;

		return new EgoClass( ageClass , theSex , degree );
	}

	private static class EgoClass {
		private final int ageClass;
		private final SnowballCliques.Sex sex;
		private final int degreeClass;

		private EgoClass(
				final int ageClass,
				final SnowballCliques.Sex sex,
				final int degree ) {
			this.ageClass = ageClass;
			this.sex = sex;
			this.degreeClass = calcDegreeClass( degree );
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
			hashCode += 100 * (sex == null ? 0 : sex.ordinal() + 1);
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

	private static class CliqueSampler {
		private final CliquePositions[] cliques;

		private int currentMaxSize = -1;
		private int currentMaxIndex = -1;

		private CliqueSampler( final Collection<CliquePositions> l ) {
			this.cliques = l.toArray( new CliquePositions[ l.size() ] );
			Arrays.sort( this.cliques , (c1, c2) -> Integer.compare( c1.size() , c2.size() ) );
		}

		public CliquePositions sampleClique(
				final Random random,
				final int maxSize ) {
			updateMaxSize( maxSize );
			return cliques[ random.nextInt( currentMaxIndex ) ];
		}

		private void updateMaxSize( final int maxSize ) {
			if ( maxSize == currentMaxSize ) return;

			// if greater than the known max size, restart from scratch.
			// otherwise, start from known max
			if ( maxSize > currentMaxSize ) {
				currentMaxSize = cliques[ cliques.length - 1 ].size();
				currentMaxIndex = cliques.length;
			}

			currentMaxIndex = ArrayUtils.searchLowest(
					cliques,
					CliquePositions::size,
					maxSize,
					0 , currentMaxIndex );
			currentMaxSize = maxSize;
			assert cliques[ currentMaxIndex ].size() == currentMaxSize;
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
