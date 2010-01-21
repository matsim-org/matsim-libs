/* *********************************************************************** *
 * project: org.matsim.*
 * KMLPartition.java
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
package org.matsim.contrib.sna.graph.spatial.io;

import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

/**
 * @author jillenberger
 *
 */
public interface KMLPartitions {

	public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph);
	
	public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition);
	
}
