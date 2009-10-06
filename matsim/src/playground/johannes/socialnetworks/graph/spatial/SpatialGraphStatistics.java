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
import gnu.trove.TDoubleDoubleIterator;
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
	
	private static double descretize(double d) {
		if(d == 0)
			return 1;
		else
			return Math.ceil(d/1000.0);
	}
	
	private static double calculateCircleSegment(double dx, double dy, double r) {
		if(dx >= r && dy >= r) {
			return Math.PI/2.0;
		} else if (Math.sqrt(dx * dx + dy *dy) <= r){
			return 0.0;
		} else {
			double alpha1 = 0;
			if(dx < r)
				alpha1 = Math.acos(dx/r);
			
			double alpha2 = Math.PI/2.0;
			if(dy < r)
				alpha2 = Math.asin(dy/r);
			
			return Math.abs(alpha2 - alpha1);
		}
	}
	
	public static Distribution normalizedEdgeLengthDistribution(Set<? extends SpatialVertex> vertices, SpatialGraph vertices2, double descretization) {
		HashMap<SpatialVertex, TDoubleDoubleHashMap> normConstants = new HashMap<SpatialVertex, TDoubleDoubleHashMap>();
		
		double bounds[] = vertices2.getBounds();
		for(SpatialVertex v_i : vertices) {
			
			Coord c_i = v_i.getCoordinate();
			if(c_i.getX() >= bounds[0] && c_i.getY() >= bounds[1] && c_i.getX() <= bounds[2] && c_i.getY() <= bounds[3]) {
				/*
				 * count vertices per distance bin
				 */
				TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
				for(SpatialVertex v_j : vertices2.getVertices()) {
					Coord c_j = v_j.getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = descretize(d);
					n_d.adjustOrPutValue(bin, 1, 1);
				}
				/*
				 * determine density
				 */
				TDoubleDoubleHashMap b_d = new TDoubleDoubleHashMap();
				TDoubleDoubleIterator it = n_d.iterator();
				for(int k = 0; k < n_d.size(); k++) {
					it.advance();
					double r = it.key();
					double count = it.value();
					/*
					 * determine the distances to the system boundaries
					 */
					double dx1 = descretize(c_i.getX() - bounds[0]);//(c_i.getX() - xmin) / descretization;
					double dx2 = descretize(bounds[2] - c_i.getX());//(xmax - c_i.getX()) / descretization;
					double dy1 = descretize(c_i.getY() - bounds[1]);//(c_i.getY() - ymin) / descretization;
					double dy2 = descretize(bounds[3] - c_i.getY());//(ymax - c_i.getY()) / descretization;
					/*
					 * calculate the circle segment for each quadrant
					 */
					double b = calculateCircleSegment(dx2, dy2, r);
					b += calculateCircleSegment(dy1, dx2, r);
					b += calculateCircleSegment(dx1, dy1, r);
					b += calculateCircleSegment(dy2, dx1, r);
					/*
					 * do some checks
					 */
					if (Double.isNaN(b)) {
						throw new IllegalArgumentException("b must not be NaN!");
					} else if(Double.isInfinite(b)) {
						throw new IllegalArgumentException("b must not be infinity!");
					} else if(b == 0) {
						b = 0.001; //TODO: Check this!
					}
					b_d.put(r, b);
					/*
					 * set the new value for z_d
					 */
					double a = b*r - 0.5;
					it.setValue(count/a);
				}
			
				
//				
//				TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
//			
//			
////			for(SpatialVertex v_j : vertices) {
//			for(SpatialVertex v_j : vertices2.getVertices()) {
////				if(v_i != v_j) {
//					Coord c_j = v_j.getCoordinate();
//					double d = CoordUtils.calcDistance(c_i, c_j);
//					
//					double bin = Math.ceil(d/descretization);
//					norm_i.adjustOrPutValue(bin, 1, 1);
////				}
//			}
			normConstants.put(v_i, n_d);
			}
		}
		
		Distribution stats = new Distribution();
		for(SpatialVertex v1 : vertices) {
			Coord c_i = v1.getCoordinate();
			if(c_i.getX() >= bounds[0] && c_i.getY() >= bounds[1] && c_i.getX() <= bounds[2] && c_i.getY() <= bounds[3]) {
				for (SparseEdge e : v1.getEdges()) {
					double d = ((SpatialEdge) e).length();
					if (d > 0) {
//						double n = normConstants.get(v1).get(
//								Math.ceil(d / descretization));
						double n = normConstants.get(v1).get(descretize(d));
//						n += 1;
						stats.add(d, 1 / n);
					}
				}
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
