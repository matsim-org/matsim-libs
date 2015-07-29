/* *********************************************************************** *
 * project: org.matsim.*
 * FixedGroupsIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;

import org.matsim.contrib.socnetsim.framework.cliques.config.CliquesConfigGroup;

import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * @author thibautd
 */
@Singleton
public class FixedGroupsIdentifier implements GroupIdentifier {
	private static final Logger log =
		Logger.getLogger(FixedGroupsIdentifier.class);

	private final Collection<? extends Collection<Id<Person>>> groupsInfo;

	public static class FixedGroupsProvider implements Provider<FixedGroupsIdentifier> {
		private final Scenario scenario;

		public FixedGroupsProvider( final Scenario sc ) {
			this.scenario = sc;
		}

		@Override
		public FixedGroupsIdentifier get() {
			final Config config = scenario.getConfig();
			final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
						config.getModule( CliquesConfigGroup.GROUP_NAME );

			return config.households().getInputFile()!=null ?
					new FixedGroupsIdentifier(
							((ScenarioImpl) scenario).getHouseholds() ) :
					FixedGroupsIdentifierFileParser.readCliquesFile(
							cliquesConf.getInputFile() );
		}
	}

	public FixedGroupsIdentifier(final Collection<? extends Collection<Id<Person>>> groups) {
		this.groupsInfo = groups;
	}

	/**
	 * for convenience: takes the grouping information in the household
	 * container.
	 * <br>
	 * After initialization, any change in the household container will
	 * <b><u>NOT</u></b> be reflected here!!!
	 */
	public FixedGroupsIdentifier(final Households households) {
		this( extractGroups( households ) );
	}

	private static Collection<? extends Collection<Id<Person>>> extractGroups(
			final Households households) {
		final List<List<Id<Person>>> groups = new ArrayList<>();

		for ( Household hh : households.getHouseholds().values() ) {
			groups.add( hh.getMemberIds() );
		}

		return groups;
	}

	public Collection<? extends Collection<Id<Person>>> getGroupInfo() {
		return groupsInfo;
	}

	@Override
	public Collection<ReplanningGroup> identifyGroups(final Population population) {
		final List<ReplanningGroup> groups = new ArrayList<ReplanningGroup>();
		final Map<Id, Person> persons = new LinkedHashMap<Id, Person>( population.getPersons() );

		int countGroups = 0;
		int countPersonsExplicit = 0;
		for (Collection<Id<Person>> groupIds : groupsInfo) {
			final ReplanningGroup g = new ReplanningGroup();
			countGroups++;

			for (Id<Person> id : groupIds) {
				countPersonsExplicit++;
				final Person p = persons.remove( id );
				if ( p == null ) {
					if ( population.getPersons().containsKey( id ) ) {
						throw new RuntimeException( "person with id "+id+" was found pertaining to several groups" );
					}
					throw new RuntimeException( "no person with id "+id+" in population "+population+" of size "+population.getPersons().size() );
				}
				g.addPerson( p );
			}

			groups.add( g );
		}
		log.info( countPersonsExplicit+" were allocated to "+countGroups+" groups." );

		// persons not allocated to any group are "grouped" alone
		int countPersonsImplicit = 0;
		for (Person p : persons.values()) {
			final ReplanningGroup g = new ReplanningGroup();
			g.addPerson( p );
			groups.add( g );
			countPersonsImplicit++;
		}
		log.info( countPersonsImplicit+" were not explicitly allocated to any group, and were put in one-person groups" );

		return groups;
	}
}

