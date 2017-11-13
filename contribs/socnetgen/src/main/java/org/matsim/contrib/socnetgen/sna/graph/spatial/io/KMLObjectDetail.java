/* *********************************************************************** *
 * project: org.matsim.*
 * KMLObjectDetail.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import net.opengis.kml.v_2_2_0.PlacemarkType;

/**
 * A KMLObjectDetail allows to add further attributes to a placemark (e.g. that
 * represents a vertex or edge).
 * 
 * @author jillenberger
 * 
 */
public interface KMLObjectDetail {

	/**
	 * Will be called by after creating a placemark with its geometry set.
	 * 
	 * @param kmlPlacemark
	 *            a placemark
	 * @param object
	 *            the object that is associated to <tt>kmlPlacemark</tt>,
	 *            usually a vertex or an edge.
	 */
	public void addDetail(PlacemarkType kmlPlacemark, Object object);

}
