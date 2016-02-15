/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbaConst.java
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import gnu.trove.iterator.TDoubleDoubleIterator;
import gnu.trove.iterator.TDoubleObjectIterator;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleIntHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AbstractVertexProperty;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AttributePartition;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.snowball.spatial.analysis.ObservedLogAccessibility;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class AcceptanceProbaConst extends AbstractVertexProperty {

	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();
	
	private Discretizer distanceDiscretizer = new LinearDiscretizer(1000.0);
	
	private Set<Point> points;
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.VertexProperty#values(java.util.Set)
	 */
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		ObservedLogAccessibility access = new ObservedLogAccessibility();
		GravityCostFunction function = new GravityCostFunction(1.6, 0.0, distanceCalculator);
		TObjectDoubleHashMap<SpatialVertex> values = access.values((Set<? extends SpatialVertex>) vertices, function, points);
		
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(values.values(), 10));
		TDoubleObjectHashMap<Set<SpatialVertex>> partitions = partitioner.partition(values);
		
		TObjectDoubleHashMap<Vertex> constValues = new TObjectDoubleHashMap<Vertex>();
		
		TDoubleObjectIterator<Set<SpatialVertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			double constant = constant(it.value(), points);
			
			for(SpatialVertex v : it.value()) {
				constValues.put(v, constant);
			}
		} 
		return constValues;
	}

	private double constant(Set<? extends SpatialVertex> vertices, Set<Point> points) {
		TDoubleIntHashMap M_d = new TDoubleIntHashMap();
		for(SpatialVertex vertex : vertices) {
			Point p1 = vertex.getPoint();
			if(p1 != null) {
				for(Point p2 : points) {
					double d = distanceCalculator.distance(p1, p2);
					d = distanceDiscretizer.discretize(d);
					M_d.adjustOrPutValue(d, 1, 1);
				}
			}
		}
		
		TDoubleDoubleHashMap m_d = Distance.getInstance().distribution(vertices).absoluteDistribution(1000.0);
		
		TDoubleDoubleIterator it = m_d.iterator();
		double constant = 0;
		int cnt = 0;
		for(int i = 0; i < m_d.size(); i++) {
			it.advance();
			
			double d = it.key();
			double m = it.value();
			
			int M = M_d.get(d);
			if(M > 0) {
				constant += m / (Math.pow(d, -1.6) * M);
				cnt++;
			}
		}
		
		return constant/(double)cnt;
	}
}
