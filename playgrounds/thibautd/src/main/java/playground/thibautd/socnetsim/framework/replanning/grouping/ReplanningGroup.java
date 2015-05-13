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
package playground.thibautd.socnetsim.framework.replanning.grouping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Person;

/**
 * Groups together agents which may interact.
 * @author thibautd
 */
public final class ReplanningGroup {
	private final List<Person> persons = new ArrayList<Person>();
	private final List<Person> immutablePersons = Collections.unmodifiableList( persons );

	public boolean addPerson(final Person person) {
		return persons.add( person );
	}

	public boolean removePerson(final Person person) {
		return persons.remove( person );
	}

	public List<Person> getPersons() {
		return immutablePersons;
	}

	@Override
	public String toString() {
		return "[ReplanningGroup:"+persons+"]";
	}

	@Override
	public boolean equals(final Object o) {
		if ( !(o instanceof ReplanningGroup) ) return false;

		return ((ReplanningGroup) o).persons.size() == persons.size() &&
				((ReplanningGroup) o).persons.containsAll( persons );
	}

	@Override
	public int hashCode() {
		int c = 0;
		for (Person p : persons) c += p.hashCode();
		return c;
	}
}

