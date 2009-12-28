/* *********************************************************************** *
 * project: org.matsim.*
 * Population.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.view;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author illenberger
 *
 */
public interface Population {
	
	public Map<Id, ? extends Person> getPersons();
	
	public void addPerson(Person person);
	
	public void removePerson(Person person);
	
 	public void removePerson(Id id);

}
