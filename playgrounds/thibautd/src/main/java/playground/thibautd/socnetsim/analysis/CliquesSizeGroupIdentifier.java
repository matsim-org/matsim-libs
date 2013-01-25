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
package playground.thibautd.socnetsim.analysis;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;

import playground.thibautd.utils.ObjectPool;

/**
 * @author thibautd
 */
public class CliquesSizeGroupIdentifier implements AbstractPlanAnalyzerPerGroup.GroupIdentifier {
	private final Map<Id, Id> personIdToGroupId = new LinkedHashMap<Id, Id>();

	public CliquesSizeGroupIdentifier(final Collection<? extends Collection<Id>> groups) {
		final ObjectPool<Id> idPool = new ObjectPool<Id>();

		for (Collection<Id> group : groups) {
			final Id groupId = idPool.getPooledInstance( new IdImpl( "cliques of size "+group.size() ) );
			for (Id personId : group) {
				personIdToGroupId.put( personId , groupId );
			}
		}
	}

	@Override
	public Id getGroup(final Person person) {
		return personIdToGroupId.get( person.getId() );
	}
}

