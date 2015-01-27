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
package playground.thibautd.socnetsim.replanning.grouping;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkUtils;
import playground.thibautd.utils.CollectionUtils;

/**
 * @author thibautd
 */
public class GroupingUtils {
	private  GroupingUtils() {}

	public static Collection<Collection<Plan>> randomlyGroup(
			final Random random,
			final double probActivationTie,
			final double probBreakingJointPlan,
			final GroupPlans groupPlans,
			final SocialNetwork socialNetwork) {
		final Map<Id, Plan> planPerPerson = new LinkedHashMap<Id, Plan>();
		for ( Plan p : groupPlans.getAllIndividualPlans() ) {
			planPerPerson.put( p.getPerson().getId() , p );
		}

		final Map<Id, Set<Id>> jpTies = getJointPlanLinks( groupPlans );
		final Map<Id<Person>, Set<Id<Person>>> subnet =
			SocialNetworkUtils.getSubnetwork(
					socialNetwork,
					planPerPerson.keySet() );

		final Collection<Collection<Plan>> groups = new ArrayList<Collection<Plan>>();
		while ( !subnet.isEmpty() ) {
			final Set<Id> group =
					getRandomGroup(
						random,
						probActivationTie,
						subnet,
						probBreakingJointPlan,
						jpTies );

			final Collection<Plan> plans = new ArrayList<Plan>();
			for ( Id id : group ) plans.add( planPerPerson.remove( id ) );
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
			final Random random,
			final double probActivationTie,
			final double probBreakingJointPlan,
			final Population population,
			final JointPlans jointPlans,
			final SocialNetwork socialNetwork) {

		final Map<Id, Set<Id>> jpTies = getJointPlanNetwork( population , jointPlans );
		final Map<Id<Person>, Set<Id<Person>>> netmap = new LinkedHashMap<>( socialNetwork.getMapRepresentation() );

		final Collection<ReplanningGroup> groups = new ArrayList<ReplanningGroup>();
		while ( !netmap.isEmpty() ) {
			final Set<Id> ids =
					getRandomGroup(
						random,
						probActivationTie,
						netmap,
						probBreakingJointPlan,
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
			final Random random,
			final double probActivationTie,
			final Map<Id<Person>, Set<Id<Person>>> netmap,
			final double probBreakingJointPlan,
			final Map<Id, Set<Id>> jpTies) {
		final Set<Id> group = new LinkedHashSet<Id>();

		final Queue<Id> egoStack = Collections.asLifoQueue( new ArrayDeque<Id>( netmap.size() ) );
		egoStack.add( CollectionUtils.getElement( 0 , netmap.keySet() ) );

		while ( !egoStack.isEmpty() ) {
			final Id ego = egoStack.remove();
			final Set<Id<Person>> alters = netmap.remove( ego );
			if ( alters == null ) continue;
			group.add( ego );

			final Set<Id> jpAlters = jpTies.remove( ego );
			if ( jpAlters != null && random.nextDouble() >= probBreakingJointPlan ) {
				// keep jp
				for ( Id alter : jpAlters ) {
					alters.remove( alter );
					egoStack.add( alter );
				}
			}

			for ( Id alter : alters ) {
				if ( random.nextDouble() < probActivationTie ) {
					egoStack.add( alter );
				}
			}
		}

		return group;
	}
}

