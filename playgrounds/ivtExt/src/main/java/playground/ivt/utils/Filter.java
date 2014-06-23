/* *********************************************************************** *
 * project: org.matsim.*
 * Filter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.utils;

/**
 * To use when one needs to provide a way to accept or reject things
 * (typically for filtering persons before analysis).
 * Having this in an interface allows to implement filters only once and
 * use them in all kind of analysis easily.
 * @author thibautd
 */
public interface Filter<T> {
	public boolean accept( T o );
}

