/* *********************************************************************** *
 * project: org.matsim.*
 * PrismicLocationChoiceAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupingUtils;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.utils.CollectionUtils;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;

/**
 * @author thibautd
 */
public class PrismicLocationChoiceAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final PrismicLocationChoiceConfigGroup config;
	
	private final Random random;
	private final ActivityFacilities facilities;
	private final Map<String, QuadTree<ActivityFacility>> facilitiesPerType;
	private final SocialNetwork socialNetwork;

	private final LocationChooser chooser;
	
	private final StageActivityTypes stages;

	public PrismicLocationChoiceAlgorithm(
			final PrismicLocationChoiceConfigGroup config,
			final ActivityFacilities facilities,
			final SocialNetwork socialNetwork,
			final StageActivityTypes stages) {
		this(
			config,
			getChooser( config ),
			facilities,
			socialNetwork,
			stages );
	}

	private static LocationChooser getChooser(
			final PrismicLocationChoiceConfigGroup config) {
		switch ( config.getSamplingMethod() ) {
			case random:
				return new RandomLocationChooser();
			case maximumDistanceInverselyProportional:
				return new MaxDistanceProportionalLocationChooser();
			case maximumDistanceMinimization:
				return new MaxDistanceMinimizingLocationChooser();
			case maximumDistanceLogit:
				return new MaxDistanceLogitLocationChooser( config.getMaximumDistanceLogitBeta() );
			default:
				throw new IllegalArgumentException( ""+config.getSamplingMethod() );
		}
	}

	public PrismicLocationChoiceAlgorithm(
			final PrismicLocationChoiceConfigGroup config,
			final LocationChooser chooser,
			final ActivityFacilities facilities,
			final SocialNetwork socialNetwork,
			final StageActivityTypes stages) {
		this.random = MatsimRandom.getLocalInstance();
		this.chooser = chooser;
		this.config = config;
		this.facilities = facilities;
		this.socialNetwork = socialNetwork;
		this.stages = stages;

		this.facilitiesPerType = new HashMap<String, QuadTree<ActivityFacility>>();
		for ( String type : config.getTypes() ) {
			final QuadTreeRebuilder<ActivityFacility> builder = new QuadTreeRebuilder<ActivityFacility>();
			for ( ActivityFacility facility : facilities.getFacilitiesForActivityType( type ).values() ) {
				builder.put( facility.getCoord() , facility );
			}
			this.facilitiesPerType.put( type , builder.getQuadTree() );
		}
	}

	@Override
	public void run(final GroupPlans plan) {
		final Collection<Collection<Subchain>> activityGroupsToHandle = selectActivityGroups( plan );

		for ( Collection<Subchain> subchains : activityGroupsToHandle ) {
			final List<ActivityFacility> potentialLocations = identifyPotentialLocations( subchains );
			if ( potentialLocations.isEmpty() ) continue;
			final ActivityFacility facility = chooser.choose( subchains , potentialLocations );
			changeLocation( subchains , facility );
		}
	}

	private static void changeLocation(
			final Collection<Subchain> subchains,
			final ActivityFacility facility) {
		for ( Subchain subchain : subchains ) {
			((ActivityImpl) subchain.getToMove()).setFacilityId(
				facility.getId() );
			((ActivityImpl) subchain.getToMove()).setLinkId(
				facility.getLinkId() );
			((ActivityImpl) subchain.getToMove()).setCoord(
				facility.getCoord() );
		}
	}

	private List<ActivityFacility> identifyPotentialLocations(
			final Collection<Subchain> subchains) {
		final String type = CollectionUtils.getElement(0, subchains).getToMove().getType();
		final QuadTree<ActivityFacility> quadTree = facilitiesPerType.get( type );

		// TODO: handle especially case where one agent is far away (ie do choice only for other agents?)
		Set<ActivityFacility> potentialLocations = null;
		int mult=1;
		do {
			final double distanceBudget = mult * config.getCrowflySpeed() * config.getTravelTimBudget_s();
			mult++;

			assert !subchains.isEmpty();
			for ( Subchain subchain : subchains ) {
				final ActivityFacility start = facilities.getFacilities().get( subchain.getStart().getFacilityId() );
				if ( start == null ) throw new RuntimeException( "no facility "+ subchain.getStart().getFacilityId()+" for activity "+ subchain.getStart());
				final ActivityFacility end = facilities.getFacilities().get( subchain.getEnd().getFacilityId() );
				if ( end == null ) throw new RuntimeException( "no facility "+ subchain.getEnd().getFacilityId()+" for activity "+ subchain.getEnd());

				final double minDistance = CoordUtils.calcEuclideanDistance( start.getCoord() , end.getCoord() ) + 1E-9;
				final Collection<ActivityFacility> prism =
					approximatePrism(
							Math.max(
								minDistance,
								distanceBudget ),
							quadTree,
							start,
							end );

				potentialLocations = potentialLocations == null ?
					new HashSet<ActivityFacility>( prism ) :
					CollectionUtils.intersect( potentialLocations , prism );
				if ( potentialLocations.isEmpty() ) break; // early abort: will loop anyway
			}
		}
		while ( potentialLocations.isEmpty() && mult <= config.getMaximumExpansionFactor() );

		final List<ActivityFacility> list = new ArrayList<ActivityFacility>( potentialLocations );
		// for determinsim
		Collections.sort(
				list,
				new Comparator<ActivityFacility>() {
					@Override
					public int compare(
							final ActivityFacility o1,
							final ActivityFacility o2) {
						return o1.getId().compareTo( o2.getId() );
					}
				});

		return list;
	}

	private static Collection<ActivityFacility> approximatePrism(
			final double maxTraveledDistance,
			final QuadTree<ActivityFacility> quadTree,
			final ActivityFacility start,
			final ActivityFacility end) {

		return quadTree.getElliptical(
				start.getCoord().getX(),
				start.getCoord().getY(),
				end.getCoord().getX(),
				end.getCoord().getY(),
				maxTraveledDistance);
	}

	private Collection<Collection<Subchain>> selectActivityGroups(
			final GroupPlans plan) {
		final Collection<Collection<Plan>> planGroups =
			GroupingUtils.randomlyGroup(
					config,
					random,
					plan,
					socialNetwork );
		final Collection<Collection<Subchain>> groups = new ArrayList<Collection<Subchain>>();

		for ( Collection<Plan> group : planGroups ) {
			final Collection<Subchain> subchainGroup = new ArrayList<Subchain>();
			String type = null;
			for ( Plan p : group ) {
				final Subchain sc = type == null ?
						getRandomSubchain( p , config.getTypes() ) :
						getRandomSubchain( p , Collections.singleton( type ) );
				if ( sc != null ) subchainGroup.add( sc );
				if ( type == null && sc != null ) type = sc.getToMove().getType();
			}
			if ( !subchainGroup.isEmpty() ) groups.add( subchainGroup );
		}

		return groups;
	}

	private Subchain getRandomSubchain(
			final Plan plan,
			final Collection<String> types) {
		final List<Subchain> potentialSubchains = new ArrayList<Subchain>();

		Trip accessTrip = null;
		for ( Trip trip : TripStructureUtils.getTrips( plan , stages ) ) {
			if ( accessTrip != null ) {
				assert accessTrip.getDestinationActivity() == trip.getOriginActivity() : accessTrip.getDestinationActivity()+" != "+trip.getOriginActivity();
				potentialSubchains.add(
						new Subchain(
							accessTrip.getOriginActivity(),
							accessTrip.getDestinationActivity(),
							trip.getDestinationActivity() ) );
			}

			accessTrip =
				types.contains( trip.getDestinationActivity().getType() ) ?
				trip :
				null;
		}

		return potentialSubchains.isEmpty() ? null :
			potentialSubchains.get( random.nextInt( potentialSubchains.size() ) );
	}

	public static class Subchain {
		private final Activity start;
		private final Activity toMove;
		private final Activity end;

		public Subchain(
				final Activity start,
				final Activity toMove,
				final Activity end) {
			this.start = start;
			this.toMove = toMove;
			this.end = end;
		}

		public Activity getStart() {
			return start;
		}

		public Activity getToMove() {
			return toMove;
		}

		public Activity getEnd() {
			return end;
		}
	}

	public interface LocationChooser {
		ActivityFacility choose(
				Collection<Subchain> subchains,
				List<ActivityFacility> choiceSet);
	}

	public static class RandomLocationChooser implements LocationChooser {
		private final Random random = MatsimRandom.getLocalInstance();

		@Override
		public ActivityFacility choose(
				final Collection<Subchain> subchains,
				final List<ActivityFacility> choiceSet) {
			 return choiceSet.get( random.nextInt( choiceSet.size() ) );
		}
	}

	public static class MaxDistanceProportionalLocationChooser implements LocationChooser {
		private final Random random = MatsimRandom.getLocalInstance();

		@Override
		public ActivityFacility choose(
				final Collection<Subchain> subchains,
				final List<ActivityFacility> choiceSet) {
			final double[] maxDists = new double[ choiceSet.size() ];

			double sum = 0;
			for ( int i=0; i < maxDists.length; i++ ) {
				final ActivityFacility fac = choiceSet.get( i );
				maxDists[ i ] = Double.NEGATIVE_INFINITY;
				for ( Subchain subchain : subchains ) {
					final double dist = CoordUtils.calcEuclideanDistance(
							subchain.getStart().getCoord(),
							fac.getCoord() ) +
						CoordUtils.calcEuclideanDistance(
							subchain.getEnd().getCoord(),
							fac.getCoord() );
					if ( dist > maxDists[ i ] ) maxDists[ i ] = dist;
				}
				maxDists[ i ] = 1 / (1 + maxDists[ i ] );
				sum += maxDists[ i ];
			}

			double choice = random.nextDouble() * sum;
			for ( int i=0; i < maxDists.length; i++ ) {
				choice -= maxDists[ i ];
				if ( choice <= 0 ) return choiceSet.get( i );
			}

			throw new RuntimeException( ""+sum );
		}
	}

	public static class MaxDistanceMinimizingLocationChooser implements LocationChooser {
		@Override
		public ActivityFacility choose(
				final Collection<Subchain> subchains,
				final List<ActivityFacility> choiceSet) {
			double currentMin = Double.POSITIVE_INFINITY;
			ActivityFacility choice = null;

			for ( ActivityFacility fac : choiceSet ) {
				double max = Double.NEGATIVE_INFINITY;
				for ( Subchain subchain : subchains ) {
					final double dist = CoordUtils.calcEuclideanDistance(
							subchain.getStart().getCoord(),
							fac.getCoord() ) +
						CoordUtils.calcEuclideanDistance(
							subchain.getEnd().getCoord(),
							fac.getCoord() );
					if ( dist > max ) max = dist;
				}

				if ( max < currentMin ) {
					currentMin = max;
					choice = fac;
				}
			}

			return choice;
		}
	}

	public static class MaxDistanceLogitLocationChooser implements LocationChooser {
		private final Random random = MatsimRandom.getLocalInstance();
		private final double beta;

		public MaxDistanceLogitLocationChooser(final double beta) {
			if ( beta > 0 ) throw new IllegalArgumentException( "beta must be negative to make sense. Got "+beta );
			this.beta = beta;
		}

		@Override
		public ActivityFacility choose(
				final Collection<Subchain> subchains,
				final List<ActivityFacility> choiceSet) {
			final double[] vals = new double[ choiceSet.size() ];

			double max = Double.NEGATIVE_INFINITY;
			for ( int i=0; i < vals.length; i++ ) {
				final ActivityFacility fac = choiceSet.get( i );
				vals[ i ] = Double.NEGATIVE_INFINITY;

				for ( Subchain subchain : subchains ) {
					final double dist = CoordUtils.calcEuclideanDistance(
							subchain.getStart().getCoord(),
							fac.getCoord() ) +
						CoordUtils.calcEuclideanDistance(
							subchain.getEnd().getCoord(),
							fac.getCoord() );
					if ( dist > vals[ i ] ) vals[ i ] = dist;
				}

				if ( vals[ i ] > max ) max = vals[ i ];
			}

			double sum = 0;
			for ( int i=0; i < vals.length; i++ ) {
				// substract max to avoid overflow (see comments in ExpBetaPlanSelector)
				vals[ i ] = Math.exp( beta * (vals[ i ] - max) );
				sum += vals[ i ];
			}

			double choice = random.nextDouble() * sum;
			for ( int i=0; i < vals.length; i++ ) {
				choice -= vals[ i ];
				if ( choice <= 0 ) return choiceSet.get( i );
			}

			throw new RuntimeException( ""+sum );
		}
	}
}

