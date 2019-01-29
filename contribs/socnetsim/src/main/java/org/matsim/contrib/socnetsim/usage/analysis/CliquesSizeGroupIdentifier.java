/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesSizeGroupIdentifier.java
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
package org.matsim.contrib.socnetsim.usage.analysis;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import org.matsim.contrib.socnetsim.framework.cliques.Clique;
import org.matsim.contrib.socnetsim.utils.ObjectPool;

/**
 * @author thibautd
 */
public class CliquesSizeGroupIdentifier implements AbstractPlanAnalyzerPerGroup.GroupIdentifier {
	private final Map<Id<Person>, Id<Clique>> personIdToGroupId = new LinkedHashMap<Id<Person>, Id<Clique>>();
	private final Id<Clique> fullGroupId = Id.create( "all" , Clique.class);

	public CliquesSizeGroupIdentifier(final Collection<? extends Collection<Id<Person>>> groups) {
		final ObjectPool<Id<Clique>> idPool = new ObjectPool<>();

		for (Collection<Id<Person>> group : groups) {
			final Id<Clique> groupId = idPool.getPooledInstance( Id.create( "cliques of size "+group.size() , Clique.class ) );
			for (Id<Person> personId : group) {
				personIdToGroupId.put( personId , groupId );
			}
		}
	}

	@Override
	public Iterable<Id<Clique>> getGroups(final Person person) {
		return Arrays.asList(
				personIdToGroupId.get( person.getId() ),
				fullGroupId);
	}
}

