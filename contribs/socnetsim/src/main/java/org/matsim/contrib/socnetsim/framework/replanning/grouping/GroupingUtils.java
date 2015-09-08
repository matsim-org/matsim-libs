/* *********************************************************************** *
 * project: org.matsim.*
 * GroupingUtils.java
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
package org.matsim.contrib.socnetsim.framework.replanning.grouping;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkUtils;
import org.matsim.contrib.socnetsim.utils.CollectionUtils;

import java.util.*;

/**
 * @author thibautd
 */
public class GroupingUtils {
	private  GroupingUtils() {}

	public interface GroupingParameters {
		double getTieActivationProbability();
		double getJointPlanBreakingProbability();
		double getMaxGroupSize();
	}

	public static Collection<Collection<Plan>> randomlyGroup(
			final GroupingParameters params,
			final Random random,
			final GroupPlans groupPlans,
			final SocialNetwork socialNetwork) {
		final Map<Id<Person>, Plan> planPerPerson = new LinkedHashMap<>();
		for ( Plan p : groupPlans.getAllIndividualPlans() ) {
			planPerPerson.put( p.getPerson().getId() , p );
		}

		final Map<Id, Set<Id>> jpTies = getJointPlanLinks( groupPlans );
		final Map<Id<Person>, Set<Id<Person>>> subnet =
			SocialNetworkUtils.getSubnetwork(
					socialNetwork,
					planPerPerson.keySet() );

		assert planPerPerson.keySet().containsAll( jpTies.keySet() ) : planPerPerson+" "+jpTies;
		assert planPerPerson.keySet().equals( subnet.keySet() ) : planPerPerson +" != "+ subnet;

		final Collection<Collection<Plan>> groups = new ArrayList<Collection<Plan>>();
		while ( !subnet.isEmpty() ) {
			final Set<Id> group =
					getRandomGroup(
							params,
							random,
							subnet,
							jpTies );

			assert planPerPerson.keySet().containsAll( group ) : planPerPerson +" - "+group+" - "+groups;

			final Collection<Plan> plans = new ArrayList<Plan>();
			for ( Id id : group ) {
				plans.add( planPerPerson.remove( id ) );
			}

			assert !CollectionUtils.intersects( planPerPerson.keySet() , group ) : planPerPerson+" intersect "+group;
			assert planPerPerson.keySet().containsAll( jpTies.keySet() ) : planPerPerson+" "+jpTies;
			assert planPerPerson.keySet().equals( subnet.keySet() ) : planPerPerson+"  "+subnet;

			groups.add( plans );
		}

		return groups;
	}

	public static Map<Id, Set<Id>> getJointPlanLinks(final GroupPlans groupPlans) {
		final Map<Id, Set<Id>> links = new LinkedHashMap<Id, Set<Id>>();

		for ( JointPlan jp : groupPlans.getJointPlans() ) {
			for ( Id id : jp.getIndividualPlans().keySet() ) {
				final Set<Id> alters = new LinkedHashSet<Id>( jp.getIndividualPlans().keySet() );
				alters.remove( id );
				links.put( id , alters );
			}
		}

		return links;
	}

	public static Collection<ReplanningGroup> randomlyGroupPersons(
			final GroupingParameters params,
			final Random random,
			final Population population,
			final JointPlans jointPlans,
			final SocialNetwork socialNetwork) {

		final Map<Id, Set<Id>> jpTies = getJointPlanNetwork( population , jointPlans );
		final Map<Id<Person>, Set<Id<Person>>> netmap = new LinkedHashMap<>( socialNetwork.getMapRepresentation() );

		final Collection<ReplanningGroup> groups = new ArrayList<ReplanningGroup>();
		while ( !netmap.isEmpty() ) {
			final Set<Id> ids =
					getRandomGroup(
							params,
							random,
							netmap,
							jpTies );

			final ReplanningGroup group = new ReplanningGroup();
			groups.add( group );
			for ( Id id : ids ) group.addPerson( population.getPersons().get( id ) );
		}

		return groups;
	}

	public static Map<Id, Set<Id>> getJointPlanNetwork(
			final Population population,
			final JointPlans jointPlans ) {
		final Map<Id, Set<Id>> links = new LinkedHashMap<Id, Set<Id>>();

		for ( Person person : population.getPersons().values() ) {
			final Set<Id> alters = new LinkedHashSet<Id>();
			links.put( person.getId() , alters );

			for ( Plan plan : person.getPlans() ) {
				final JointPlan jp = jointPlans.getJointPlan( plan );
				if ( jp == null ) continue;
				alters.addAll( jp.getIndividualPlans().keySet() );
			}

			alters.remove( person.getId() );
		}

		return links;
	}

	private static Set<Id> getRandomGroup(
			final GroupingParameters params,
			final Random random,
			final Map<Id<Person>, Set<Id<Person>>> netmap,
			final Map<Id, Set<Id>> jpTies) {
		assert netmap.keySet().containsAll( jpTies.keySet() ) : netmap+" != "+jpTies;
		final Set<Id> group = new LinkedHashSet<Id>();

		final Queue<Id> egoStack = Collections.asLifoQueue( new ArrayDeque<Id>( netmap.size() ) );
		egoStack.add( CollectionUtils.getElement( 0 , netmap.keySet() ) );

		while ( !egoStack.isEmpty() /*&& group.size() < params.getMaxGroupSize()*/ ) {
			final Id ego = egoStack.remove();
			final Set<Id<Person>> alters = netmap.remove( ego );
			final Set<Id> jpAlters = jpTies.remove( ego );

			if ( alters == null ) continue;
			group.add( ego );

			if ( jpAlters != null && random.nextDouble() >= params.getJointPlanBreakingProbability() ) {
				// keep jp
				for ( Id alter : jpAlters ) {
					alters.remove( alter );
					egoStack.add(alter);
					// already add, so that joint plan not broken by early abort
					// group.add( alter );
				}
			}

			for ( Id alter : alters ) {
				if ( random.nextDouble() < params.getTieActivationProbability() ) {
					egoStack.add( alter );
				}
			}
		}

		for ( Id<Person> p : group )  {
			netmap.remove( p );
			jpTies.remove( p );
		}

		assert netmap.keySet().containsAll( jpTies.keySet() ) : netmap+" != "+jpTies;

		return group;
	}
}

