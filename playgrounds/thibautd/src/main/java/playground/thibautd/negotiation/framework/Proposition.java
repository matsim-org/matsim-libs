/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.negotiation.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author thibautd
 */
public interface Proposition {
	Id<Person> getProposerId();
	Collection<Id<Person>> getProposedIds();

	default Collection<Id<Person>> getGroupIds() {
		final Collection<Id<Person>> ids = new ArrayList<>( getProposedIds().size() + 1 );
		ids.addAll( getProposedIds() );
		ids.add( getProposerId() );
		return ids;
	}
}
