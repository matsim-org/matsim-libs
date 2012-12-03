/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningGroup.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Person;

/**
 * Groups together agents which may interact.
 * @author thibautd
 */
public class ReplanningGroup {
	private final List<Person> persons = new ArrayList<Person>();
	private final Collection<Person> immutablePersons = Collections.unmodifiableList( persons );

	public boolean addPerson(final Person person) {
		return persons.add( person );
	}

	public boolean removePerson(final Person person) {
		return persons.remove( person );
	}

	public Collection<Person> getPersons() {
		return immutablePersons;
	}
}

