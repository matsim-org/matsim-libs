/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.interfaces.population;

import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.population.Person;

/**
 * @author dgrether
 */
public interface Household extends BasicHousehold {
	
	public void addMember(Person member);
	
	public Map<Id, Person> getMembers();
	
}
