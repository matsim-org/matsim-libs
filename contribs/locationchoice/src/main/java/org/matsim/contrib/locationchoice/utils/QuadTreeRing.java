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

package org.matsim.contrib.locationchoice.utils;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;

/*
 * The super-class now also supports the method getRing(...) and its internal realization should be faster.
 * cdobler, oct'15 
 */
@Deprecated
public class QuadTreeRing<T> extends QuadTree<T> {

	private static final long serialVersionUID = 1L;

	public QuadTreeRing(double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
	}
	
	public Collection<T> getRing(final double x, final double y, final double outerRadius, final double innerradius) {
		Collection<T> locations = super.getDisk(x, y, outerRadius);
		Collection<T> innerLocations = super.getDisk(x, y, innerradius);
		locations.removeAll(innerLocations);
		return locations;
	}
}
