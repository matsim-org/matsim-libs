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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import net.opengis.kml._2.FolderType;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;

import java.util.List;
import java.util.Set;

/**
 * A KMLPartition implementation allows to organize partitions of vertices into
 * separate folders.
 * 
 * @author jillenberger
 * 
 */
public interface KMLPartitions {

	/**
	 * Returns a list of vertex partitions that will be organized into separate
	 * folders.
	 * 
	 * @param graph
	 *            a graph
	 * @return a list of vertex partitions that will be organized into separate
	 *         folders.
	 */
	public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph);

	/**
	 * Adds details to the folder (such as name or description) of a partition.
	 * 
	 * @param kmlFolder
	 *            the folder of <tt>partition</tt>.
	 * @param partition
	 *            a partition of vertices.
	 */
	public void addDetail(FolderType kmlFolder,
			Set<? extends SpatialVertex> partition);

}
