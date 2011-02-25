/* *********************************************************************** *
 * project: org.matsim.*
 * KMLEgoPartition.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;

/**
 * @author illenberger
 *
 */
public class KMLEgoPartition implements KMLPartitions {

	@Override
	public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
		SocialSampledGraphProjection<?, ?, ?> socialGraph = (SocialSampledGraphProjection<?, ?, ?>) graph;
		
		List<Set<? extends SpatialVertex>> partitions = new ArrayList<Set<? extends SpatialVertex>>(1);
		partitions.add(SnowballPartitions.createSampledPartition(socialGraph.getVertices()));
		
		return partitions;
	}

	@Override
	public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
	}

}
