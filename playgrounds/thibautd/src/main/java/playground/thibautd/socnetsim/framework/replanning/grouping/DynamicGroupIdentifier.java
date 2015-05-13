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
package playground.thibautd.socnetsim.framework.replanning.grouping;

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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author thibautd
 */
@Singleton
public class DynamicGroupIdentifier implements GroupIdentifier {
	private static final Logger log =
		Logger.getLogger(DynamicGroupIdentifier.class);

	private final Scenario scenario;
	private final Random random;

	@Inject
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

		final Queue<TaggedReplanningGroup> jointPlansGroups =
			identifyGroupsBasedOnJointPlans( population , jointPlans );
		final List<ReplanningGroup> mergedGroups =
			randomlyMergeGroups( jointPlansGroups , socialNetwork );

		return mergedGroups;
	}

	private List<ReplanningGroup> randomlyMergeGroups(
			final Queue<TaggedReplanningGroup> groupsToMerge,
			final SocialNetwork socialNetwork) {
		final Map<Id, TaggedReplanningGroup> groupPerPerson = new HashMap<Id, TaggedReplanningGroup>();

		for ( TaggedReplanningGroup g : groupsToMerge ) {
			for ( Person p : g.group.getPersons() ) {
				final TaggedReplanningGroup rem = groupPerPerson.put( p.getId() , g );
				assert rem == null;
			}
		}

		final List<ReplanningGroup> merged = new ArrayList<ReplanningGroup>();
		while ( true ) {
			final TaggedReplanningGroup g = pollNonAllocated( groupsToMerge );

			if ( g == null || groupsToMerge.isEmpty() ) {
				if ( g != null ) merged.add( g.group );
				return merged;
			}
			for ( Person p : g.group.getPersons() ) groupPerPerson.remove( p.getId() );

			final List<Id> groupAlters = getGroupAlters( g.group , socialNetwork , groupPerPerson.keySet() );

			if ( groupAlters.isEmpty() ) {
				// no need to merge
				if ( log.isTraceEnabled() ) {
					log.trace( "add non-merged group "+g );
				}
				merged.add( g.group );
				continue;
			}

			final Id selectedAlter = groupAlters.get( random.nextInt( groupAlters.size() ) );
			final TaggedReplanningGroup selectedGroup = groupPerPerson.remove( selectedAlter );
			assert selectedGroup != null;
			for ( Person p : selectedGroup.group.getPersons() ) groupPerPerson.remove( p.getId() );
			selectedGroup.isAllocated = true;

			final ReplanningGroup newGroup = new ReplanningGroup();
			for ( Person p : g.group.getPersons() ) newGroup.addPerson( p );
			for ( Person p : selectedGroup.group.getPersons() ) newGroup.addPerson( p );

			if ( log.isTraceEnabled() ) {
				log.trace( "add merged groups "+g+" and "+selectedGroup );
			}
			merged.add( newGroup );
		}
	}

	private static TaggedReplanningGroup pollNonAllocated(
			final Queue<TaggedReplanningGroup> groupsToMerge) {
		while ( !groupsToMerge.isEmpty() ) {
			final TaggedReplanningGroup g = groupsToMerge.poll();
			if ( !g.isAllocated ) return g;
		}
		return null;
	}

	private static List<Id> getGroupAlters(
			final ReplanningGroup g,
			final SocialNetwork socialNetwork,
			final Set<Id> allowedPersonIds) {
		final List<Id> groupAlters = new ArrayList<Id>();

		final Set<Id<Person>> groupIds = new HashSet<>( g.getPersons().size() * 2 );
		for ( Person p : g.getPersons() ) groupIds.add( p.getId() );

		for ( Person p : g.getPersons() ) {
			final Set<Id<Person>> alters = socialNetwork.getAlters( p.getId() );
			for ( Id<Person> alter : alters ) {
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

	private Queue<TaggedReplanningGroup> identifyGroupsBasedOnJointPlans(
			final Population population,
			final JointPlans jointPlans) {
		final Queue<Person> remainingPersons = new ArrayDeque<Person>( population.getPersons().values() );
		final Queue<TaggedReplanningGroup> groups = new ArrayDeque<TaggedReplanningGroup>();

		while ( !remainingPersons.isEmpty() ) {
			final ReplanningGroup group = new ReplanningGroup();
			groups.add( new TaggedReplanningGroup( group ) );
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

	// the groups are put in a queue, but we also need to remove
	// specified elements. Tagging them as "allocated" is much faster than
	// removing them.
	private static class TaggedReplanningGroup {
		public final ReplanningGroup group;
		public boolean isAllocated = false;

		public TaggedReplanningGroup(final ReplanningGroup r) {
			this.group = r;
		}
	}
}

