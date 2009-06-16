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
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.Set;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.graph.SparseEdge;
import playground.johannes.socialnetworks.statistics.Correlations;
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
			d_distr.put(v, sum/(double)v.getNeighbours().size());
		}
		
		return GraphStatistics.degreeCorrelation(d_distr);
	}

	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(SpatialGraph network) {
		return edgeLengthDegreeCorrelation(network.getVertices());
	}

	
	public static TDoubleDoubleHashMap densityCorrelation(TObjectDoubleHashMap<? extends SpatialVertex> vertexValues, SpatialGrid<Double> densityGrid, double binsize) {
			double values1[] = new double[vertexValues.size()];
			double values2[] = new double[vertexValues.size()];
			
			TObjectDoubleIterator<? extends SpatialVertex> it = vertexValues.iterator();
			for(int i = 0; i < values1.length; i++) {
				it.advance();
				if(densityGrid.isInBounds(it.key().getCoordinate())) {
					values1[i] = densityGrid.getValue(it.key().getCoordinate());
					values2[i] = it.value();
				}
			}
			
			return Correlations.correlationMean(values1, values2, binsize);
		}

		public static TDoubleDoubleHashMap degreeDensityCorrelation(Collection<? extends SpatialVertex> vertices, SpatialGrid<Double> densityGrid) {
			TObjectDoubleHashMap<SpatialVertex> vertexValues = new TObjectDoubleHashMap<SpatialVertex>();
			for(SpatialVertex e : vertices) {
				vertexValues.put(e, e.getEdges().size());
			}
			
			return densityCorrelation(vertexValues, densityGrid, densityGrid.getResolution());
		}

		public static TDoubleDoubleHashMap clusteringDensityCorrelation(Collection<? extends SpatialVertex> vertices, SpatialGrid<Double> densityGrid) {
			return densityCorrelation(GraphStatistics.localClusteringCoefficients(vertices), densityGrid, densityGrid.getResolution());
		}

		public static <V extends SpatialVertex> TDoubleObjectHashMap<Set<V>> createDensityPartitions(Set<V> vertices, SpatialGrid<Double> densityGrid, double binsize) {
			TObjectDoubleHashMap<V> vertexValues = new TObjectDoubleHashMap<V>();
			for(V v : vertices) {
				if(densityGrid.isInBounds(v.getCoordinate())) {
					double rho = densityGrid.getValue(v.getCoordinate());
					vertexValues.put(v, rho);
				}
			}
			return Partitions.createPartitions(vertexValues, binsize);
		}

}
