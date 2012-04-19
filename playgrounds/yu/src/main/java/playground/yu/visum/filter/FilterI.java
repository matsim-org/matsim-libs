/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.yu.visum.filter;

/**
 * this interface offers the basic functions for
 * org.matsim.playground.filters.filter.Filter und its subclasses.
 * 
 * @author ychen
 * 
 */
public interface FilterI {
	/**
	 * Counts, how many persons (org.matsim.demandmodeling.plans.Person) or
	 * events(org.matsim.demandmodeling.events.Event) were selected
	 */
	void count();

	/**
	 * Returns how many persons (org.matsim.demandmodeling.plans.Person) or
	 * events(org.matsim.demandmodeling.events.Event) were selected
	 * 
	 * @return how many persons (org.matsim.demandmodeling.plans.Person) or
	 *         events(org.matsim.demandmodeling.events.Event) were selected
	 */
	int getCount();
}
