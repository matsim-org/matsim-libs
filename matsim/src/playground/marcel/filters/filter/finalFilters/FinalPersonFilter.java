/* *********************************************************************** *
 * project: org.matsim.*
 * FinalPersonFilter.java
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

package playground.marcel.filters.filter.finalFilters;

import org.matsim.plans.Person;

import playground.marcel.filters.filter.PersonFilterA;

/**
 * @author ychen
 * 
 */
public class FinalPersonFilter extends PersonFilterA {
	/**
	 * does nothing
	 * 
	 * @param person -
	 *            a person transfered from another PersonFilter
	 */
	@Override
	public boolean judge(Person person) {
		return false;
	}
}
