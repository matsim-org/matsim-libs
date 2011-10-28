/* *********************************************************************** *
 * project: org.matsim.*
 * KMLObjectStyle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.sna.graph.spatial.io;

import net.opengis.kml._2.StyleType;

/**
 * A KMLObjectStyle is used to define the style type of an object (e.g., a
 * vertex or an edge).
 * 
 * @author jillenberger
 * 
 */
public interface KMLObjectStyle {

	/**
	 * Returns the style type used to display <tt>object</tt>.
	 * 
	 * @param object
	 *            an object (e.g., vertex or edge)
	 * @return the style type used to display <tt>object</tt>.
	 */
	public StyleType getStyle(Object object);

}
