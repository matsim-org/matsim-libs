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

package org.matsim.api.core.v01.population;

import java.io.Serializable;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * Root class of the population description (previously also called "plans file")
 */
public interface Population extends MatsimToplevelContainer, Serializable {
	
	public PopulationFactory getFactory();
	
	public String getName();
	
	public void setName(String name);

	public Map<Id,? extends Person> getPersons();
	
	public void addPerson(final Person p); 

}
