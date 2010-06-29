/* *********************************************************************** *
 * project: org.matsim.*
 * Accessability.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class Accessability {
	
	public Distribution distribution(Set<? extends SpatialVertex> vertices, SpatialCostFunction costFunction, Set<Point> opportunities) {
		TObjectDoubleHashMap<SpatialVertex> values = values(vertices, costFunction, opportunities);
		TObjectDoubleIterator<SpatialVertex> it = values.iterator();
		Distribution distr = new Distribution();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			distr.add(it.value());
		}
		
		return distr;
	}

	public TObjectDoubleHashMap<SpatialVertex> values(Set<? extends SpatialVertex> vertices, SpatialCostFunction costFunction, Set<Point> opportunities) {
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>(vertices.size());
		
		for(SpatialVertex vertex : vertices) {
			double sum = 0;
			for(Point point : opportunities) {
				sum += costFunction.costs(vertex.getPoint(), point);
			}
			values.put(vertex, sum);
		}
		
		return values;
	}
}
