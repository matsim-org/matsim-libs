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

package playground.marcel.filters.filter;

import org.matsim.plans.filters.FilterI;

/**
 * @author ychen
 */
public class Filter implements FilterI {

	/* -------------------MEMBER VARIABLE----------- */
	private int count = 0;

	/*
	 * -------------------NORMAL METHOD-------------
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.playground.filters.FilterI#count()
	 */
	/**
	 * This function is called inside function: void
	 * org.matsim.playground.filters.filter.EventFilter.handleEvent(BasicEvent
	 * event) and void
	 * org.matsim.playground.filters.filter.EventFilter.handleEvent(BasicEvent
	 * event), if this Filter is not the last one.
	 */
	public void count() {
		count++;
	}

	/* ----------------GETTER--------------------- */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.demandmodeling.filters.filter.FilterI#getCount()
	 */
	public int getCount() {
		return count;
	}

}
