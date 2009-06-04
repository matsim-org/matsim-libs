/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphStatistics.java
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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.SparseEdge;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SpatialGraphStatistics {

	public static Distribution edgeLengthDistribution(SpatialGraph graph) {
		return edgeLengthDistribution(graph.getVertices());
	}

	public static Distribution edgeLengthDistribution(Set<? extends SpatialVertex> vertices) {
		Distribution stats = new Distribution();
		for(SpatialVertex v1 : vertices) {
			for(SparseEdge e : v1.getEdges())
				stats.add(((SpatialEdge)e).length());
//			for(SpatialVertex v2 : v1.getNeighbours()) {
//				double d = CoordUtils.calcDistance(v1.getCoordinate(), v2.getCoordinate());
//				stats.add(d);
//			}
		}
		
		return stats;
	}

	public static TObjectDoubleHashMap<? extends SpatialVertex> meanEdgeLength(SpatialGraph g) {
		return meanEdgeLength(g.getVertices());
	}

	public static <V extends SpatialVertex> TObjectDoubleHashMap<V> meanEdgeLength(Set<V> vertices) {
		TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
		for (V i : vertices) {
			if (i.getNeighbours().size() > 0) {
				double sum_d = 0;
				for(SparseEdge e : i.getEdges())
					sum_d += ((SpatialEdge)e).length();
//				for (SpatialVertex j : i.getNeighbours()) {
//					sum_d += CoordUtils
//							.calcDistance(i.getCoordinate(), j.getCoordinate());
//				}
				double d_mean = sum_d / (double) i.getNeighbours().size();
				values.put(i, d_mean);
			}
		}
		return values;
	}

	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(Set<? extends SpatialVertex> vertices) {
		TObjectDoubleHashMap<SpatialVertex> d_distr = new TObjectDoubleHashMap<SpatialVertex>();
		for(SpatialVertex v : vertices) {
			double sum = 0;
			for(SparseEdge e : v.getEdges())
				sum += ((SpatialEdge)e).length();
//			for(SpatialVertex e2 : e.getNeighbours()) {
//				sum += CoordUtils.calcDistance(e.getCoordinate(), e2.getCoordinate());
//			}
			d_distr.put(v, sum/(double)v.getNeighbours().size());
		}
		
		return GraphStatistics.degreeCorrelation(d_distr);
	}

	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(SpatialGraph network) {
		return edgeLengthDegreeCorrelation(network.getVertices());
	}

//	public static <T extends SpatialVertex> TObjectDoubleHashMap<T> localEdgeLengthMSE(Set<T> vertices) {
//		TObjectDoubleHashMap<T> mse = new TObjectDoubleHashMap<T>();
//		TDoubleDoubleHashMap globalDistr = edgeLengthDistribution(vertices).normalizedDistribution(1000);
//		for(T v : vertices) {
//			Distribution localDistr = new Distribution();
//			Coord c1 = v.getCoordinate();
//			for(SpatialVertex t : v.getNeighbours()) {
//				Coord c2 = t.getCoordinate();
//				double d = CoordUtils.calcDistance(c1, c2);
//				localDistr.add(d);
//			}
//			mse.put(v, Distribution.meanSquareError(localDistr.normalizedDistribution(1000), globalDistr));
//		}
//		return mse;
//	}
//
//	public static <T extends SpatialVertex> TDoubleDoubleHashMap edgeLengthMSEDegreeCorrelation(Set<T> vertices) {
//		TObjectDoubleHashMap<T> d_distr = new TObjectDoubleHashMap<T>();
//		TObjectDoubleHashMap<T> mseDistr = localEdgeLengthMSE(vertices);
//		for(T e : vertices) {
//			d_distr.put(e, mseDistr.get(e));
//		}
//		
//		return GraphStatistics.degreeCorrelation(d_distr);
//	}

}
