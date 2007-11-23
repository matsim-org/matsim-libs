/* *********************************************************************** *
 * project: org.matsim.*
 * PrintStreamVisum9_3I.java
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

package org.matsim.filters.writer;

import java.io.Closeable;

import org.matsim.filters.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen A PrintStreamVisum9_3I is a source or destination of data that
 *         can be printed,implements the interface Closeable and offers
 *         output()-function for every writer-class in the package. and
 */
public interface PrintStreamVisum9_3I extends Closeable {
	/**
	 * Extracts the last Filter and save the usefull information to print
	 * 
	 * @param fef -
	 *            the last Filter to extract
	 */
	public void output(FinalEventFilterA fef);
}
