/* *********************************************************************** *
 * project: org.matsim.*
 * Population.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package teach.multiagent07.population;

import java.util.Collection;

import org.matsim.basic.v01.BasicPopulation;

public class Population extends BasicPopulation<Person> {

	public Collection<Person> getPersons() { return persons.values();};
	
	public void runHandler(PersonHandler handler) {
		
		if (handler == null) return;
		
		for (Person person : persons.values()) {
			handler.handlePerson(person);
		}

	}

}
 