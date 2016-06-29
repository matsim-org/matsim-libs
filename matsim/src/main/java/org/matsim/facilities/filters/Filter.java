/* *********************************************************************** *
 * project: org.matsim.*
 * Filter.java
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

package org.matsim.facilities.filters;

/**
 * basic interface for implementing Filters
 * 
 * @author ychen
 */
public interface Filter {
	/**
	 * Counts, how many objects (e.g. {@link org.matsim.api.core.v01.population.Person}s, 
	 * {@link org.matsim.api.core.v01.events.Event Events}) were selected by the filter.
	 */
	void count();

	/**
	 * @return the number of objects (e.g. {@link org.matsim.api.core.v01.population.Person}s, 
	 * {@link org.matsim.api.core.v01.events.Event Events}) were selected by the filter.
	 */
	int getCount();
}
