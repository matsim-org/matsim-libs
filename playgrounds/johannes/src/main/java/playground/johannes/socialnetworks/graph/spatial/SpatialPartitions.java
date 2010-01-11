/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialPartitiona.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.johannes.socialnetworks.spatial.Zone;

/**
 * @author illenberger
 *
 */
public class SpatialPartitions {

	public static <V extends SpatialSparseVertex> Set<V> createSpatialPartition(Set<V> vertices, Zone zone) {
		Set<V> partition = new HashSet<V>();
		GeometryFactory factory = new GeometryFactory();
		Geometry geometry = zone.getBorder();
		for(V v : vertices) {
			Coordinate coordinate = new Coordinate(v.getCoordinate().getX(), v.getCoordinate().getY());
			if(geometry.contains(factory.createPoint(coordinate))) {
				partition.add(v);
			}
		}
		return partition;
	}
	
}
