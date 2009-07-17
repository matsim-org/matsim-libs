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
import gnu.trove.TObjectByteHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

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

	public static Distribution normalizedEdgeLengthDistribution(Set<? extends SpatialVertex> vertices, double descretization) {
		HashMap<SpatialVertex, TDoubleDoubleHashMap> normConstants = new HashMap<SpatialVertex, TDoubleDoubleHashMap>();
		
		for(SpatialVertex v_i : vertices) {
			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
			
			Coord c_i = v_i.getCoordinate();
			for(SpatialVertex v_j : vertices) {
				if(v_i != v_j) {
					Coord c_j = v_j.getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = Math.ceil(d/descretization);
					norm_i.adjustOrPutValue(bin, 1, 1);
				}
			}
			normConstants.put(v_i, norm_i);
		}
		
		Distribution stats = new Distribution();
		for(SpatialVertex v1 : vertices) {
			for(SparseEdge e : v1.getEdges()) {
				double d = ((SpatialEdge)e).length();
				stats.add(d, 1/normConstants.get(v1).get(Math.ceil(d/descretization)));
			}
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

	public static TDoubleDoubleHashMap densityCorrelation(
			TObjectDoubleHashMap<? extends SpatialVertex> vertexValues,
			SpatialGrid<Double> densityGrid, double binsize) {
		double values1[] = new double[vertexValues.size()];
		double values2[] = new double[vertexValues.size()];

		TObjectDoubleIterator<? extends SpatialVertex> it = vertexValues
				.iterator();
		for (int i = 0; i < values1.length; i++) {
			it.advance();
			if (densityGrid.isInBounds(it.key().getCoordinate())) {
				values1[i] = densityGrid.getValue(it.key().getCoordinate());
				values2[i] = it.value();
			}
		}

		return Correlations.correlationMean(values1, values2, binsize);
	}

	public static TDoubleDoubleHashMap degreeDensityCorrelation(
			Collection<? extends SpatialVertex> vertices,
			SpatialGrid<Double> densityGrid) {
		TObjectDoubleHashMap<SpatialVertex> vertexValues = new TObjectDoubleHashMap<SpatialVertex>();
		for (SpatialVertex e : vertices) {
			vertexValues.put(e, e.getEdges().size());
		}

		return densityCorrelation(vertexValues, densityGrid, densityGrid
				.getResolution());
	}

	public static TDoubleDoubleHashMap clusteringDensityCorrelation(
			Collection<? extends SpatialVertex> vertices,
			SpatialGrid<Double> densityGrid) {
		return densityCorrelation(GraphStatistics
				.localClusteringCoefficients(vertices), densityGrid,
				densityGrid.getResolution());
	}

	public static TObjectDoubleHashMap<? extends SpatialVertex> centerDistance(Set<? extends SpatialVertex> vertices) {
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = - Double.MAX_VALUE;
		double ymax = - Double.MAX_VALUE;
		for(SpatialVertex v : vertices) {
			Coord c_i = v.getCoordinate();
			xmin = Math.min(xmin, c_i.getX());
			ymin = Math.min(ymin, c_i.getY());
			xmax = Math.max(xmax, c_i.getX());
			ymax = Math.max(ymax, c_i.getY());
		}
		
		double xcenter = (xmax - xmin)/2.0 + xmin;
		double ycenter = (ymax - ymin)/2.0 + ymin;
		
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>();
		for(SpatialVertex v : vertices) {
			Coord c = v.getCoordinate();
			double dx = Math.abs(xcenter - c.getX());
			double dy = Math.abs(ycenter - c.getY());
			double d = Math.sqrt(dx * dx + dy * dy);
			values.put(v, d);
		}
		
		return values;
	}
	
	public static TDoubleDoubleHashMap centerDistanceCorrelation(
			TObjectDoubleHashMap<? extends SpatialVertex> vertexValues,
			double binsize) {
		double values1[] = new double[vertexValues.size()];
		double values2[] = new double[vertexValues.size()];

		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		TObjectDoubleIterator<? extends SpatialVertex> it = vertexValues.iterator();
		for (int i = 0; i < values1.length; i++) {
			it.advance();
			vertices.add(it.key());
		}
		TObjectDoubleHashMap<SpatialVertex> centerDistance = (TObjectDoubleHashMap<SpatialVertex>) centerDistance(vertices); // FIXME!
		
		it = vertexValues.iterator();
		for (int i = 0; i < values1.length; i++) {
			it.advance();
			
			values1[i] = centerDistance.get(it.key());
			values2[i] = it.value();
			
		}

		return Correlations.correlationMean(values1, values2, binsize);
	}
	
	public static TDoubleDoubleHashMap degreeCenterDistanceCorrelation(Set<? extends SpatialVertex> vertices, double binsize) {
		TObjectDoubleHashMap<SpatialVertex> vertexValues = new TObjectDoubleHashMap<SpatialVertex>();
		for(SpatialVertex v : vertices) {
			vertexValues.put(v, v.getEdges().size());
		}
		
		return centerDistanceCorrelation(vertexValues, binsize);
	}
	
	public static TDoubleDoubleHashMap edgeLengthCenterDistanceCorrelation(Set<? extends SpatialVertex> vertices, double binsize) {
		return centerDistanceCorrelation(meanEdgeLength(vertices), binsize);
	}
	
	public static TDoubleDoubleHashMap clusteringCenterDistanceCorrelation(Set<? extends SpatialVertex> vertices, double binsize) {
		return centerDistanceCorrelation(GraphStatistics.localClusteringCoefficients(vertices), binsize);
	}
	
	public static <V extends SpatialVertex> TDoubleObjectHashMap<Set<V>> createDensityPartitions(
			Set<V> vertices, SpatialGrid<Double> densityGrid, double binsize) {
		TObjectDoubleHashMap<V> vertexValues = new TObjectDoubleHashMap<V>();
		for (V v : vertices) {
			if (densityGrid.isInBounds(v.getCoordinate())) {
				double rho = densityGrid.getValue(v.getCoordinate());
				vertexValues.put(v, rho);
			}
		}
		return Partitions.createPartitions(vertexValues, binsize);
	}

}
