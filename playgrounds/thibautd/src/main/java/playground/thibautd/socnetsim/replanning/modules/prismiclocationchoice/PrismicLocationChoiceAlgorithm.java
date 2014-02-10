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
package playground.thibautd.socnetsim.replanning.modules.prismiclocationchoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.utils.CollectionUtils;
import playground.thibautd.utils.QuadTreeRebuilder;

/**
 * @author thibautd
 */
public class PrismicLocationChoiceAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final PrismicLocationChoiceConfigGroup config;
	
	private Random random;
	private final ActivityFacilities facilities;
	private final Map<String, QuadTree<ActivityFacility>> facilitiesPerType;
	private final SocialNetwork socialNetwork;
	
	private final StageActivityTypes stages;

	public PrismicLocationChoiceAlgorithm(
			final PrismicLocationChoiceConfigGroup config,
			final ActivityFacilities facilities,
			final SocialNetwork socialNetwork,
			final StageActivityTypes stages) {
		this.random = MatsimRandom.getLocalInstance();
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
			final ActivityFacility facility = potentialLocations.get( random.nextInt( potentialLocations.size() ) );
			changeLocation( subchains , facility );
		}
	}

	private static void changeLocation(
			final Collection<Subchain> subchains,
			final ActivityFacility facility) {
		for ( Subchain subchain : subchains ) {
			((ActivityImpl) subchain.toMove).setFacilityId(
				facility.getId() );
			((ActivityImpl) subchain.toMove).setLinkId(
				facility.getLinkId() );
			((ActivityImpl) subchain.toMove).setCoord(
				facility.getCoord() );
		}
	}

	private List<ActivityFacility> identifyPotentialLocations(
			final Collection<Subchain> subchains) {
		final String type = CollectionUtils.getElement( 0 , subchains ).toMove.getType();
		final QuadTree<ActivityFacility> quadTree = facilitiesPerType.get( type );

		// TODO: iterate Andi-like (extend search space if not enough potential solutions)
		// TODO: handle case where one agent is far away (ie do choice only for other agents?)
		Set<ActivityFacility> potentialLocations = null;
		for ( Subchain subchain : subchains ) {
			final ActivityFacility start = facilities.getFacilities().get( subchain.start.getFacilityId() );
			final ActivityFacility end = facilities.getFacilities().get( subchain.end.getFacilityId() );

			final Collection<ActivityFacility> prism = approximatePrism( quadTree , start , end );

			potentialLocations = potentialLocations == null ?
				new HashSet<ActivityFacility>( prism ) :
				CollectionUtils.intersect( potentialLocations , prism );

			if ( potentialLocations.isEmpty() ) return Collections.emptyList();
		}

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

	private Collection<ActivityFacility> approximatePrism(
			final QuadTree<ActivityFacility> quadTree,
			final ActivityFacility start,
			final ActivityFacility end) {
		final double minDistance = CoordUtils.calcDistance( start.getCoord() , end.getCoord() );

		return quadTree.getElliptical(
				start.getCoord().getX(),
				start.getCoord().getY(),
				end.getCoord().getX(),
				end.getCoord().getY(),
				Math.max(
					minDistance,
					config.getCrowflySpeed() * config.getTravelTimBudget_s() ) );
	}

	private Collection<Collection<Subchain>> selectActivityGroups(
			final GroupPlans plan) {
		final Map<Id, Plan> plans = new LinkedHashMap<Id, Plan>();

		for ( Plan p : plan.getAllIndividualPlans() ) {
			plans.put( p.getPerson().getId() , p );
		}

		final Collection<Collection<Subchain>> groups = new ArrayList<Collection<Subchain>>();

		// TODO: separate in methods to replace continues by returns
		while ( !plans.isEmpty() ) {
			final Plan seed = CollectionUtils.removeRandomElement( random , plans ).getValue();
			final Subchain seedSubchain = getRandomSubchain( seed , config.getTypes() );
			if ( seedSubchain == null ) continue;

			final Collection<Subchain> subchainGroup = new ArrayList<Subchain>();
			subchainGroup.add( seedSubchain );
			groups.add( subchainGroup );

			final Collection<String> subchainType = Collections.singleton( seedSubchain.toMove.getType() );

			final Set<Id> availableAlters =
				CollectionUtils.intersectSorted( 
						socialNetwork.getAlters( seed.getPerson().getId() ),
						plans.keySet() );

			for ( Id alter : availableAlters ) {
				if ( random.nextDouble() > config.getTieActivationProb() ) continue;
				final Plan alterPlan = plans.remove( alter );
				final Subchain sc = getRandomSubchain( alterPlan , subchainType );
				// keep in map if sc null? may have subchains of other types.
				if ( sc == null ) continue;
				subchainGroup.add( sc );
			}
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
				assert accessTrip.getDestinationActivity() == trip.getOriginActivity();
				potentialSubchains.add(
						new Subchain(
							accessTrip.getOriginActivity(),
							accessTrip.getDestinationActivity(),
							trip.getDestinationActivity() ) );
			}

			if ( types.contains( trip.getDestinationActivity().getType() ) ) {
				accessTrip = trip;
			}
		}

		return potentialSubchains.isEmpty() ? null :
			potentialSubchains.get( random.nextInt( potentialSubchains.size() ) );
	}

	private static class Subchain {
		final Activity start, toMove, end;

		public Subchain(
				final Activity start,
				final Activity toMove,
				final Activity end) {
			this.start = start;
			this.toMove = toMove;
			this.end = end;
		}
	}
}

