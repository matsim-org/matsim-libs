/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriterListener.java
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

import org.matsim.vis.kml.KMZWriter;

/**
 * A KMZWriterListener allows to modify the contents of KMZ archive, e.g. to add
 * further resource to a KMZ archive linked by the main KML document.
 * 
 * @author jillenberger
 * 
 */
public interface KMZWriterListener {

	/**
	 * Will be called by {@link SpatialGraphKMLWriter} directly after creating
	 * the {@link KMZWriter}.
	 * 
	 * @param writer the KMZWriter used by the {@link SpatialGraphKMLWriter}
	 */
	public void openWriter(KMZWriter writer);
	
	/**
	 * Will be called by {@link SpatialGraphKMLWriter} before closing
	 * the {@link KMZWriter}.
	 * 
	 * @param writer the KMZWriter used by the {@link SpatialGraphKMLWriter}
	 */
	public void closeWriter(KMZWriter writer);
}
