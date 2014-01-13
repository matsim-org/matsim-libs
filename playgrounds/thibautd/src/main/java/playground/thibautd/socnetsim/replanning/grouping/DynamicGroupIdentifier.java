/* *********************************************************************** *
 * project: org.matsim.*
 * DynamicGroupIdentifier.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * @author thibautd
 */
public class DynamicGroupIdentifier implements GroupIdentifier {
	private final Scenario scenario;
	private final Random random;

	public DynamicGroupIdentifier(final Scenario scenario) {
		this.scenario = scenario;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public Collection<ReplanningGroup> identifyGroups(
			final Population population) {
		final SocialNetwork socialNetwork = (SocialNetwork)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		final JointPlans jointPlans = (JointPlans)
			scenario.getScenarioElement( JointPlans.ELEMENT_NAME );

		final Queue<ReplanningGroup> jointPlansGroups =
			identifyGroupsBasedOnJointPlans( population , jointPlans );
		final List<ReplanningGroup> mergedGroups =
			randomlyMergeGroups( jointPlansGroups , socialNetwork );

		assert mergedGroups.size() <= jointPlansGroups.size();

		return mergedGroups;
	}

	private List<ReplanningGroup> randomlyMergeGroups(
			final Queue<ReplanningGroup> groupsToMerge,
			final SocialNetwork socialNetwork) {
		final Map<Id, ReplanningGroup> groupPerPerson = new HashMap<Id, ReplanningGroup>();

		for ( ReplanningGroup g : groupsToMerge ) {
			for ( Person p : g.getPersons() ) {
				final ReplanningGroup rem = groupPerPerson.put( p.getId() , g );
				assert rem == null;
			}
		}

		final List<ReplanningGroup> merged = new ArrayList<ReplanningGroup>();
		while ( true ) {
			final ReplanningGroup g = groupsToMerge.remove();

			if ( g == null || groupsToMerge.isEmpty() ) {
				if ( g != null ) merged.add( g );
				return merged;
			}

			final List<Id> groupAlters = getGroupAlters( g , socialNetwork , groupPerPerson.keySet() );

			final Id selectedAlter = groupAlters.get( random.nextInt( groupAlters.size() ) );
			final ReplanningGroup selectedGroup = groupPerPerson.remove( selectedAlter );
			assert selectedGroup != null;
			for ( Person p : selectedGroup.getPersons() ) groupPerPerson.remove( p.getId() );

			final ReplanningGroup newGroup = new ReplanningGroup();
			for ( Person p : g.getPersons() ) newGroup.addPerson( p );
			for ( Person p : selectedGroup.getPersons() ) newGroup.addPerson( p );
		}
	}

	private static List<Id> getGroupAlters(
			final ReplanningGroup g,
			final SocialNetwork socialNetwork,
			final Set<Id> allowedPersonIds) {
		final List<Id> groupAlters = new ArrayList<Id>();

		final Set<Id> groupIds = new HashSet<Id>( g.getPersons().size() * 2 );
		for ( Person p : g.getPersons() ) groupIds.add( p.getId() );

		for ( Person p : g.getPersons() ) {
			final Set<Id> alters = socialNetwork.getAlters( p.getId() );
			for ( Id alter : alters ) {
				// group alters are agents which are not in the group but alters
				// of a member of the group.
				// Alters are added several times if they are alters of several egos
				// in the group, so that the probability of selecting them
				// is proportionnal to their "popularity" in the group.
				if ( !groupIds.contains( alter ) && allowedPersonIds.contains( alter ) ) groupAlters.add( alter );
			}
		}

		return groupAlters;
	}

	private Queue<ReplanningGroup> identifyGroupsBasedOnJointPlans(
			final Population population,
			final JointPlans jointPlans) {
		final Queue<Person> remainingPersons = new ArrayDeque<Person>( population.getPersons().values() );
		final Queue<ReplanningGroup> groups = new ArrayDeque<ReplanningGroup>();

		while ( !remainingPersons.isEmpty() ) {
			final ReplanningGroup group = new ReplanningGroup();
			groups.add( group );
			final Person person = remainingPersons.remove();
			group.addPerson( person );
			addPersonsLinkedByJointPlans(
					group,
					person,
					remainingPersons,
					jointPlans );
		}

		return groups;
	}

	private void addPersonsLinkedByJointPlans(
			final ReplanningGroup group,
			final Person person,
			final Queue<Person> remainingPersons,
			final JointPlans jointPlans) {
		for ( Plan plan : person.getPlans() ) {
			final JointPlan jp = jointPlans.getJointPlan( plan );
			if ( jp == null ) continue;

			for ( Plan planInJp : jp.getIndividualPlans().values() ) {
				final Person alter = planInJp.getPerson();
				if ( alter == person ) continue;
				// should be more efficient than checking if in remaining persons
				if ( group.getPersons().contains( alter ) ) continue;
				if ( !remainingPersons.remove( alter ) ) {
					// should not happen
					throw new RuntimeException( "alter "+alter+" was not available and not in group "+group+" for person "+person );
				}

				group.addPerson( alter );
				// TODO: avoid recursion. we fill the stack with redundant information.
				addPersonsLinkedByJointPlans(
						group,
						alter,
						remainingPersons,
						jointPlans );
			}
		}
	}
}

