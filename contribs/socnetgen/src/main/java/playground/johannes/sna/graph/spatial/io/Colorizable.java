/* *********************************************************************** *
 * project: org.matsim.*
 * Colorizable.java
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

import java.awt.Color;

/**
 * A Colorizable is used to define the color with which an object (e.g., a
 * vertex or an edge) is displayed.
 * 
 * @author jillenberger
 * 
 */
public interface Colorizable {

	/**
	 * Returns the color used to display <tt>object</tt>.
	 * 
	 * @param object
	 *            an object (e.g., a vertex or an edge).
	 * @return the color used to display <tt>object</tt>.
	 */
	public Color getColor(Object object);

}
