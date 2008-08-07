/* *********************************************************************** *
 * project: org.matsim.*
 * CoordinateTransformationI.java
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

package org.matsim.utils.geometry;


/**
 * A simple interface to convert coordinates from one coordinate system to
 * another one.
 *
 * @author mrieser
 */
public interface CoordinateTransformationI {

	/**
	 * Transforms the given coordinate from one coordinate system to the other.
	 *
	 * @param coord The coordinate to transform.
	 * @return The transformed coordinate.
	 */
	public Coord transform(Coord coord);
	
}
