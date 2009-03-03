/* *********************************************************************** *
 * project: org.matsim.*
 * Plans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.interfaces.core.v01;

import java.util.Iterator;
import java.util.Map;

import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.interfaces.basic.v01.Id;

/**
 * Root class of the population description (previously also called "plans file")
 */
public interface Population extends BasicPopulation<Person>, Iterable<Person> {
	// TODO [MR] remove Iterable
	
	public void addPerson(final Person p);

	public PopulationBuilder getPopulationBuilder();

	public Map<Id, Person> getPersons();
	

	// all the rest is deprecated
	
	@Deprecated // "experimental", will be removed in interface
	public void runAlgorithms();

	@Deprecated // "experimental", will be removed in interface
	public void clearAlgorithms();

	@Deprecated // "experimental", will be removed in interface
	public boolean removeAlgorithm(final PersonAlgorithm algo);

	@Deprecated // "experimental", will be removed in interface
	public void addAlgorithm(final PersonAlgorithm algo);

	@Deprecated
	public boolean isStreaming();

	/**
	 * @return the size of the population, i.e. the number of persons in this population.
	 * @deprecated user getPersons().size()
	 */
	public int size();

	@Deprecated
	public void printPlansCount();

	@Deprecated
	public Iterator<Person> iterator();
	

}
