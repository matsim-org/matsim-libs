/* *********************************************************************** *
 * project: org.matsim.*
 * RandomJointLocationChoiceAlgorithm.java
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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules.randomlocationchoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.utils.CollectionUtils;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;

/**
 * Randomly mutates activity locations, with a high probability that social contacts
 * go to the same location.
 *
 * It uses the following random local search method:
 * <ul>
 * <li> group agents with random social contacts from the replanning group
 * <li> for each of those group, choose a random flexible type to mutate
 * <li> choose a random activity of this type (if any) for each agent
 * <li> mutate the location of those activities to the same random location
 * </ul>
 *
 * the random location is choosen the following way:
 * <ul>
 * <li> compute the center of gravity of the current locations of the activities
 * <li> choose a random direction
 * <li> sample a distance from a normal distribution, to that in 95% of the case,
 * it falls bellow the upperBound95 parameter of the config group
 * <li> choose the facility closest to the corresponding point.
 * </ul>
 * @author thibautd
 */
public class RandomJointLocationChoiceAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final Map<String, QuadTree<ActivityFacility>> quadTreePerType;
	private final ActivityFacilities facilities;
	private final SocialNetwork socialNetwork;
	private final RandomJointLocationChoiceConfigGroup config;
	private final StageActivityTypes activityFilter =
		new StageActivityTypes() {
			@Override
			public boolean isStageActivity(final String activityType) {
				return !config.getTypes().contains( activityType );
			}
		};
	final Random random;

	public RandomJointLocationChoiceAlgorithm(
			final RandomJointLocationChoiceConfigGroup config,
			final ActivityFacilities facilities,
			final SocialNetwork socialNetwork) {
		this.config = config;
		this.socialNetwork = socialNetwork;
		this.facilities = facilities;
		this.quadTreePerType = createQuadTrees( config , facilities );
		this.random = MatsimRandom.getLocalInstance();
	}

	private static Map<String, QuadTree<ActivityFacility>> createQuadTrees(
			final RandomJointLocationChoiceConfigGroup config,
			final ActivityFacilities facilities) {
		final Map<String, QuadTreeRebuilder<ActivityFacility>> builders = new HashMap<String, QuadTreeRebuilder<ActivityFacility>>();

		final MapUtils.Factory<QuadTreeRebuilder<ActivityFacility>> factory =
			new MapUtils.Factory<QuadTreeRebuilder<ActivityFacility>>() {
				@Override
				public QuadTreeRebuilder<ActivityFacility> create() {
					return new QuadTreeRebuilder<ActivityFacility>();
				}
			};

		for ( ActivityFacility facility : facilities.getFacilities().values() ) {
			final Collection<String> types =
				CollectionUtils.intersect(
						facility.getActivityOptions().keySet(),
						config.getTypes() );

			for ( String t : types ) {
				final QuadTreeRebuilder<ActivityFacility> builder =
					MapUtils.getArbitraryObject(
							t,
							builders,
							factory );
				builder.put( facility.getCoord() , facility );
			}
		}

		final Map<String, QuadTree<ActivityFacility>> qts = new HashMap<String, QuadTree<ActivityFacility>>();
		for ( Map.Entry<String, QuadTreeRebuilder<ActivityFacility>> e : builders.entrySet() ) {
			qts.put(
					e.getKey(),
					e.getValue().getQuadTree() );
		}

		return qts;
	}

	@Override
	public void run(final GroupPlans plan) {
		// TODO: do not split joint plans
		final Map<Id, Plan> planPerAgent = getPlansMap( plan );

		while ( !planPerAgent.isEmpty() ) {
			mutateJointly( removeRandomPlanGroup( planPerAgent ) );
		}
	}

	private Collection<Plan> removeRandomPlanGroup(final Map<Id, Plan> planPerAgent) {
		final List<Plan> plans = new ArrayList<Plan>();

		final Plan seed = CollectionUtils.removeRandomElement( random , planPerAgent ).getValue();
		plans.add( seed );

		// TODO: recurse (requires some care)
		final Set<Id> availableAlters = CollectionUtils.intersectSorted(
				socialNetwork.getAlters( seed.getPerson().getId() ),
				planPerAgent.keySet() );

		for ( Id alter : availableAlters ) {
			if ( random.nextDouble() < config.getTieActivationProb() ) {
				plans.add( planPerAgent.remove( alter ) ); 
			}
		}

		return plans;
	}

	private static Map<Id, Plan> getPlansMap(final GroupPlans plan) {
		final Map<Id, Plan> planPerAgent = new LinkedHashMap<Id, Plan>();

		for ( Plan p : plan.getAllIndividualPlans() ) {
			planPerAgent.put( p.getPerson().getId() , p );
		}

		return planPerAgent;
	}

	private void mutateJointly(
			final Collection<Plan> plans) {
		final AgentAndTypeActivityMap actsPerType = new AgentAndTypeActivityMap();
		for ( Plan p : plans ) {
			for ( Activity act : TripStructureUtils.getActivities( p , activityFilter ) ) {
				actsPerType.addActivity( p.getPerson().getId() , act );
			}
		}

		if ( actsPerType.getTypes().isEmpty() ) return;
		final String type = actsPerType.getType( random.nextInt( actsPerType.getTypes().size() ) );
		assert config.getTypes().contains( type );
		final Map<Id, List<Activity>> activitiesPerPerson = actsPerType.getActivitiesOfType( type );

		final List<Activity> activitiesToMutate = new ArrayList<Activity>();
		for ( List<Activity> activitiesOfAgent : activitiesPerPerson.values() ) {
			assert !activitiesOfAgent.isEmpty();
			activitiesToMutate.add(
					activitiesOfAgent.get(
						random.nextInt(
							activitiesOfAgent.size() ) ) );
		}

		mutateLocations(
				type,
				activitiesToMutate );
	}

	private void mutateLocations(
			final String type,
			final List<Activity> activitiesToMutate) {
		final Coord coordBarycenter = calcBarycenterCoord( activitiesToMutate );
		final double angle = random.nextDouble() * Math.PI;
		final double distance = nextNormalDouble() * config.getStandardDeviation();

		final ActivityFacility fac = getFacility(
				type,
				coordBarycenter,
				angle,
				distance );

		for ( Activity act : activitiesToMutate ) {
			((ActivityImpl) act).setFacilityId( fac.getId() );
			((ActivityImpl) act).setLinkId( fac.getLinkId() );
			((ActivityImpl) act).setCoord( fac.getCoord() );
		}
	}

	/* package (tests) */
	final Coord calcBarycenterCoord(
			final List<Activity> activitiesToMutate ) {
		double sumX = 0;
		double sumY = 0;

		for ( Activity act : activitiesToMutate ) {
			final Id facilityId = act.getFacilityId();
			if ( facilityId == null ) throw new RuntimeException( "no facility for act "+act );

			final ActivityFacility fac = facilities.getFacilities().get( facilityId );
			sumX += fac.getCoord().getX();
			sumY += fac.getCoord().getY();
		}

		return new CoordImpl(
				sumX / activitiesToMutate.size(),
				sumY / activitiesToMutate.size() );
	}

	/* package (tests) */
	final ActivityFacility getFacility(
			final String type,
			final Coord coordBarycenter,
			final double angle,
			final double distance ) {
		final double xLoc = coordBarycenter.getX() + distance * Math.cos( angle );
		final double yLoc = coordBarycenter.getY() + distance * Math.sin( angle );

		return quadTreePerType.get( type ).get( xLoc , yLoc );
	}

	private double cachedNormalDouble = Double.NaN;
	private double nextNormalDouble() {
		if ( !Double.isNaN( cachedNormalDouble ) ) {
			final double toReturn = cachedNormalDouble;
			cachedNormalDouble = Double.NaN;
			return toReturn;
		}

		// box-muller transform
		final double u = random.nextDouble();
		final double v = random.nextDouble();

		final double r = Math.sqrt( -2 * Math.log( u ) );
		final double t = v * 2 * Math.PI;

		cachedNormalDouble = r * Math.cos( t );

		return r * Math.sin( t );
	}

	private final class AgentAndTypeActivityMap {
		private final Map<String, Map<Id, List<Activity>>> map = new LinkedHashMap<String, Map<Id, List<Activity>>>();

		public void addActivity( final Id person , final Activity activity ) {
			final Map<Id, List<Activity>> activitiesPerPerson =
				MapUtils.getMap(
						activity.getType(),
						map );
			final List<Activity> activities =
				MapUtils.getList(
						person,
						activitiesPerPerson );
			activities.add( activity );
		}

		public Collection<String> getTypes() {
			return map.keySet();
		}

		public String getType(final int index) {
			int i=0;
			for ( String type : getTypes() ) {
				if ( i++ == index ) return type;
			}
			throw new RuntimeException( "index "+index+" > "+getTypes().size() );
		}

		public Map<Id, List<Activity>> getActivitiesOfType(final String type) {
			return map.get( type );
		}
	}
}

