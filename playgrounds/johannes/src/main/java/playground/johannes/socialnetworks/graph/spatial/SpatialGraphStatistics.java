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
import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.spatial.Geometries;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.Zone;
import playground.johannes.socialnetworks.spatial.ZoneLayer;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;
import playground.johannes.socialnetworks.statistics.Correlations;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SpatialGraphStatistics {
	
	private static final Logger logger = Logger.getLogger(SpatialGraphStatistics.class);

	public static Distribution edgeLengthDistribution(SpatialSparseGraph graph) {
		return edgeLengthDistribution(graph.getVertices());
	}

	public static Distribution edgeLengthDistribution(Set<? extends SpatialVertex> vertices) {
		Distribution stats = new Distribution();
		for(SpatialVertex v1 : vertices) {
			for(Edge e : v1.getEdges())
				stats.add(((SpatialSparseEdge)e).length());
		}
		
		return stats;
	}
	
	public static Distribution edgeLengthDistribution(Set<? extends SpatialVertex> vertices, ZoneLayer zones) {
		Distribution stats = new Distribution();
//		if(zoneCache == null)
//			precacheZones(vertices, zones);
		
		int count = 0;
		for(SpatialVertex v1 : vertices) {
			Zone z_i = zones.getZone(v1.getCoordinate());
			for(Edge e : v1.getEdges()) {
				SpatialSparseVertex v2 = (SpatialSparseVertex) e.getOpposite(v1);
				Zone z_j = zones.getZone(v2.getCoordinate());
				if(z_i != null && z_j != null)
					stats.add(((SpatialSparseEdge)e).length());
				else
					count++;
			}
		}
		System.err.println("Skiped "+count+" edges.");
		return stats;
	}

//	public static Distribution normalizedEdgeLengthDistribution(Set<? extends SpatialVertex> vertices, double descretization) {
//		HashMap<SpatialVertex, TDoubleDoubleHashMap> normConstants = new HashMap<SpatialVertex, TDoubleDoubleHashMap>();
//		
//		for(SpatialVertex v_i : vertices) {
//			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
//			
//			Coord c_i = v_i.getCoordinate();
//			for(SpatialVertex v_j : vertices) {
//				if(v_i != v_j) {
//					Coord c_j = v_j.getCoordinate();
//					double d = CoordUtils.calcDistance(c_i, c_j);
//					double bin = Math.ceil(d/descretization);
//					norm_i.adjustOrPutValue(bin, 1, 1);
//				}
//			}
//			normConstants.put(v_i, norm_i);
//		}
//		
//		Distribution stats = new Distribution();
//		for(SpatialVertex v1 : vertices) {
//			for(SparseEdge e : v1.getEdges()) {
//				double d = ((SpatialEdge)e).length();
//				stats.add(d, 1/normConstants.get(v1).get(Math.ceil(d/descretization)));
//			}
//		}
//		
//		return stats;
//	}
	public static Distribution normalizedEdgeLengthDistribution(Set<? extends SpatialSparseVertex> vertices, SpatialSparseGraph vertices2, double descretization) {
		TDoubleIntHashMap normConstants = new TDoubleIntHashMap();
		
		for(SpatialSparseVertex v_i : vertices2.getVertices()) {
//			TDoubleDoubleHashMap norm_i = new TDoubleDoubleHashMap();
			
			Coord c_i = v_i.getCoordinate();
			for(SpatialSparseVertex v_j : vertices2.getVertices()) {
				if(v_i != v_j) {
					Coord c_j = v_j.getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = Math.ceil(d/descretization);
					normConstants.adjustOrPutValue(bin, 1, 1);
				}
			}
//			normConstants.put(v_i, norm_i);
		}
		
		Distribution stats = new Distribution();
		for(SpatialSparseVertex v1 : vertices) {
			for(SparseEdge e : v1.getEdges()) {
				double d = ((SpatialSparseEdge)e).length();
				int n = normConstants.get(Math.ceil(d/descretization));
				if(n > 0)
					stats.add(d, 1/(double)n);
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
	

	public static Distribution normalizedEdgeLengthDistribution(Set<? extends SpatialSparseVertex> vertices, SpatialSparseGraph vertices2, double descretization, ZoneLayer zones) {
//		double bounds[] = vertices2.getBounds();

		HashMap<SpatialSparseVertex, TDoubleDoubleHashMap> normConstants = new HashMap<SpatialSparseVertex, TDoubleDoubleHashMap>();
		for(SpatialSparseVertex v_i : vertices) {
			Coord c_i = v_i.getCoordinate();
			Zone z_i = zones.getZone(c_i);
			if(z_i != null) {
				/*
				 * count vertices per distance bin
				 */
				TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
				for(SpatialSparseVertex v_j : vertices2.getVertices()) {
					Coord c_j = v_j.getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = descretize(d);
					n_d.adjustOrPutValue(bin, 1, 1);
				}
				/*
				 * determine density
				 */
//				TDoubleDoubleHashMap b_d = new TDoubleDoubleHashMap();
				TDoubleDoubleIterator it = n_d.iterator();
				for(int k = 0; k < n_d.size(); k++) {
					it.advance();
//					double r = it.key();
					double count = it.value();
					/*
					 * determine the distances to the system boundaries
					 */
//					double dx1 = descretize(c_i.getX() - bounds[0]);//(c_i.getX() - xmin) / descretization;
//					double dx2 = descretize(bounds[2] - c_i.getX());//(xmax - c_i.getX()) / descretization;
//					double dy1 = descretize(c_i.getY() - bounds[1]);//(c_i.getY() - ymin) / descretization;
//					double dy2 = descretize(bounds[3] - c_i.getY());//(ymax - c_i.getY()) / descretization;
//					/*
//					 * calculate the circle segment for each quadrant
//					 */
//					double b = calculateCircleSegment(dx2, dy2, r);
//					b += calculateCircleSegment(dy1, dx2, r);
//					b += calculateCircleSegment(dx1, dy1, r);
//					b += calculateCircleSegment(dy2, dx1, r);
//					
////					b = 2*Math.PI;
//					/*
//					 * do some checks
//					 */
//					if (Double.isNaN(b)) {
//						throw new IllegalArgumentException("b must not be NaN!");
//					} else if(Double.isInfinite(b)) {
//						throw new IllegalArgumentException("b must not be infinity!");
//					} else if(b == 0) {
//						b = 0.001; //TODO: Check this!
//					}
//					b_d.put(r, b);
//					/*
//					 * set the new value for z_d
//					 */
//					double a = b*(r - 0.5);
//					if(count == 0)
//						count=1;	
//					if(a==0)
//						System.err.println();
					it.setValue(count);
//					it.setValue(count);
				}
			normConstants.put(v_i, n_d);
			}
//		}
			
		}
		
		Distribution stats = new Distribution();
		for(SpatialSparseVertex v1 : vertices) {
			Coord c_i = v1.getCoordinate();
			Zone z_i = zones.getZone(c_i);
			if(z_i != null) {
				for (SparseEdge e : v1.getEdges()) {
					double d = ((SpatialSparseEdge) e).length();
					if (d > 0) {
//						double n = normConstants.get(v1).get(
//								Math.ceil(d / descretization));
						double n = normConstants.get(v1).get(descretize(d));
						if(n > 0)
							stats.add(d, 1/n);
//						stats.add(d, 1 / d);
					}
				}
			}
		}
		
		return stats;
	}

	public static Distribution normalizedEdgeLengthDistribution(Set<? extends SpatialSparseVertex> vertices, SpatialSparseGraph vertices2, double descretization, Geometry boundary) {
		logger.info("Calculating normalized edge length distribution...");
		GeometryFactory factory = new GeometryFactory();
		HashMap<SpatialSparseVertex, TDoubleDoubleHashMap> normConstants = new HashMap<SpatialSparseVertex, TDoubleDoubleHashMap>();
		
		int cnt = 0;
		for(SpatialSparseVertex v_i : vertices) {
			Coord c_i = v_i.getCoordinate();
			Point p_i = factory.createPoint(new Coordinate(c_i.getX(), c_i.getY()));
			if(boundary.contains(p_i)) {
				/*
				 * count vertices per distance bin
				 */
				TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
				for(SpatialSparseVertex v_j : vertices2.getVertices()) {
					Coord c_j = v_j.getCoordinate();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = descretize(d);
					n_d.adjustOrPutValue(bin, 1, 1);
				}
				/*
				 * determine density
				 */
//				TDoubleDoubleHashMap b_d = new TDoubleDoubleHashMap();
				TDoubleDoubleIterator it = n_d.iterator();
				for(int k = 0; k < n_d.size(); k++) {
					it.advance();
					double r = it.key();
					double count = it.value();

					Geometry outer = Geometries.makePolygonFromCircle(r*1000, c_i.getX(), c_i.getY());
					Geometry inner = Geometries.makePolygonFromCircle((r-1)*1000, c_i.getX(), c_i.getY());
					double a_outer = boundary.intersection(outer).getArea();
					double a_inner = boundary.intersection(inner).getArea();
					double a = a_outer - a_inner;
					a = a/(1000*1000);
					it.setValue(count/a);
				}
			normConstants.put(v_i, n_d);
			}
			cnt++;
			if(cnt % 100 == 0)
				logger.info(String.format("Processed %1$s vertices of %2$s...", cnt, vertices.size()));
		}
		
		Distribution stats = new Distribution();
		for(SpatialSparseVertex v1 : vertices) {
			Coord c_i = v1.getCoordinate();
			Point p_i = factory.createPoint(new Coordinate(c_i.getX(), c_i.getY()));
			if(boundary.contains(p_i)) {
				for (SparseEdge e : v1.getEdges()) {
					double d = ((SpatialSparseEdge) e).length();
					if (d > 0) {
						TDoubleDoubleHashMap map = normConstants.get(v1);
						double n = map.get(descretize(d));
						if(n > 0)
							stats.add(d, 1 / n);
					}
				}
			}
		}

		return stats;
	}
	
	public static TObjectDoubleHashMap<? extends SpatialSparseVertex> meanEdgeLength(SpatialSparseGraph g) {
		return meanEdgeLength(g.getVertices());
	}

	public static <V extends SpatialSparseVertex> TObjectDoubleHashMap<V> meanEdgeLength(Set<V> vertices) {
		TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
		for (V i : vertices) {
			if (i.getNeighbours().size() > 0) {
				double sum_d = 0;
				for(SparseEdge e : i.getEdges())
					sum_d += ((SpatialSparseEdge)e).length();
				double d_mean = sum_d / (double) i.getNeighbours().size();
				values.put(i, d_mean);
			}
		}
		return values;
	}
	
	public static <V extends SpatialSparseVertex> TObjectDoubleHashMap<V> meanEdgeLength(Set<V> vertices, ZoneLayer zones) {
		TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
		for (V i : vertices) {
			if(zones.getZone(i.getCoordinate()) != null) {
			if (i.getNeighbours().size() > 0) {
				double sum_d = 0;
				int count = 0;
				for(SpatialSparseVertex e : i.getNeighbours()) {
					if(zones.getZone(e.getCoordinate()) != null) {
						sum_d += CoordUtils.calcDistance(i.getCoordinate(), e.getCoordinate());
						count++;
					}
				}
				double d_mean = sum_d / (double) count;
				values.put(i, d_mean);
			}
			}
		}
		return values;
	}

	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(Set<? extends SpatialVertex> vertices) {
		TObjectDoubleHashMap<SpatialVertex> d_distr = new TObjectDoubleHashMap<SpatialVertex>();
		for(SpatialVertex v : vertices) {
			double sum = 0;
			for(Edge e : v.getEdges())
				sum += ((SpatialSparseEdge)e).length();
			d_distr.put(v, sum/(double)v.getNeighbours().size());
		}
		
		return GraphStatistics.degreeCorrelation(d_distr);
	}

	public static TDoubleDoubleHashMap edgeLengthDegreeCorrelation(SpatialSparseGraph network) {
		return edgeLengthDegreeCorrelation(network.getVertices());
	}

	public static TDoubleDoubleHashMap densityCorrelation(
			TObjectDoubleHashMap<? extends SpatialSparseVertex> vertexValues,
			SpatialGrid<Double> densityGrid, double binsize) {
		double values1[] = new double[vertexValues.size()];
		double values2[] = new double[vertexValues.size()];

		TObjectDoubleIterator<? extends SpatialSparseVertex> it = vertexValues
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

	public static TDoubleDoubleHashMap densityCorrelation(TObjectDoubleHashMap<? extends SpatialSparseVertex> vertexValues, ZoneLayerDouble zones, double binsize) {
		double values1[] = new double[vertexValues.size()];
		double values2[] = new double[vertexValues.size()];

		TObjectDoubleIterator<? extends SpatialSparseVertex> it = vertexValues
				.iterator();
		for (int i = 0; i < values1.length; i++) {
			it.advance();
			double rho = zones.getValue(it.key().getCoordinate());
			if(!Double.isNaN(rho)) {
				values1[i] = rho;
				values2[i] = it.value();
			}
		}

		return Correlations.correlationMean(values1, values2, binsize);
	}
	
	public static TDoubleDoubleHashMap degreeDensityCorrelation(
			Collection<? extends SpatialSparseVertex> vertices,
			SpatialGrid<Double> densityGrid) {
		TObjectDoubleHashMap<SpatialSparseVertex> vertexValues = new TObjectDoubleHashMap<SpatialSparseVertex>();
		for (SpatialSparseVertex e : vertices) {
			vertexValues.put(e, e.getEdges().size());
		}

		return densityCorrelation(vertexValues, densityGrid, densityGrid
				.getResolution());
	}
	
	public static TDoubleDoubleHashMap degreeDensityCorrelation(
			Collection<? extends SpatialSparseVertex> vertices,
			ZoneLayerDouble zones, double binsize) {
		TObjectDoubleHashMap<SpatialSparseVertex> vertexValues = new TObjectDoubleHashMap<SpatialSparseVertex>();
		for (SpatialSparseVertex e : vertices) {
			vertexValues.put(e, e.getEdges().size());
		}

		return densityCorrelation(vertexValues, zones, binsize);
	}

	public static TDoubleDoubleHashMap clusteringDensityCorrelation(
			Collection<? extends SpatialSparseVertex> vertices,
			SpatialGrid<Double> densityGrid) {
		return densityCorrelation(GraphStatistics
				.localClusteringCoefficients(vertices), densityGrid,
				densityGrid.getResolution());
	}
	
	public static TDoubleDoubleHashMap clusteringDensityCorrelation(
			Collection<? extends SpatialSparseVertex> vertices,
			ZoneLayerDouble zones, double binsize) {
		return densityCorrelation(GraphStatistics
				.localClusteringCoefficients(vertices), zones, binsize);
	}

	public static TObjectDoubleHashMap<? extends SpatialSparseVertex> centerDistance(Set<? extends SpatialSparseVertex> vertices) {
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = - Double.MAX_VALUE;
		double ymax = - Double.MAX_VALUE;
		for(SpatialSparseVertex v : vertices) {
			Coord c_i = v.getCoordinate();
			xmin = Math.min(xmin, c_i.getX());
			ymin = Math.min(ymin, c_i.getY());
			xmax = Math.max(xmax, c_i.getX());
			ymax = Math.max(ymax, c_i.getY());
		}
		
		double xcenter = (xmax - xmin)/2.0 + xmin;
		double ycenter = (ymax - ymin)/2.0 + ymin;
		
		TObjectDoubleHashMap<SpatialSparseVertex> values = new TObjectDoubleHashMap<SpatialSparseVertex>();
		for(SpatialSparseVertex v : vertices) {
			Coord c = v.getCoordinate();
			double dx = Math.abs(xcenter - c.getX());
			double dy = Math.abs(ycenter - c.getY());
			double d = Math.sqrt(dx * dx + dy * dy);
			values.put(v, d);
		}
		
		return values;
	}
	
	public static TDoubleDoubleHashMap centerDistanceCorrelation(
			TObjectDoubleHashMap<? extends SpatialSparseVertex> vertexValues,
			double binsize) {
		double values1[] = new double[vertexValues.size()];
		double values2[] = new double[vertexValues.size()];

		Set<SpatialSparseVertex> vertices = new HashSet<SpatialSparseVertex>();
		TObjectDoubleIterator<? extends SpatialSparseVertex> it = vertexValues.iterator();
		for (int i = 0; i < values1.length; i++) {
			it.advance();
			vertices.add(it.key());
		}
		TObjectDoubleHashMap<SpatialSparseVertex> centerDistance = (TObjectDoubleHashMap<SpatialSparseVertex>) centerDistance(vertices); // FIXME!
		
		it = vertexValues.iterator();
		for (int i = 0; i < values1.length; i++) {
			it.advance();
			
			values1[i] = centerDistance.get(it.key());
			values2[i] = it.value();
			
		}

		return Correlations.correlationMean(values1, values2, binsize);
	}
	
	public static TDoubleDoubleHashMap degreeCenterDistanceCorrelation(Set<? extends SpatialSparseVertex> vertices, double binsize) {
		TObjectDoubleHashMap<SpatialSparseVertex> vertexValues = new TObjectDoubleHashMap<SpatialSparseVertex>();
		for(SpatialSparseVertex v : vertices) {
			vertexValues.put(v, v.getEdges().size());
		}
		
		return centerDistanceCorrelation(vertexValues, binsize);
	}
	
	public static TDoubleDoubleHashMap edgeLengthCenterDistanceCorrelation(Set<? extends SpatialSparseVertex> vertices, double binsize) {
		return centerDistanceCorrelation(meanEdgeLength(vertices), binsize);
	}
	
	public static TDoubleDoubleHashMap clusteringCenterDistanceCorrelation(Set<? extends SpatialSparseVertex> vertices, double binsize) {
		return centerDistanceCorrelation(GraphStatistics.localClusteringCoefficients(vertices), binsize);
	}
	
	public static <V extends SpatialSparseVertex> TDoubleObjectHashMap<Set<V>> createDensityPartitions(
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
	
	public static <V extends SpatialSparseVertex> TDoubleObjectHashMap<Set<V>> createDensityPartitions(
			Set<V> vertices, ZoneLayerDouble zones, double binsize) {
		TObjectDoubleHashMap<V> vertexValues = new TObjectDoubleHashMap<V>();
		for (V v : vertices) {
			double rho = zones.getValue(v.getCoordinate());
			if(!Double.isNaN(rho)) {
				vertexValues.put(v, rho);
			}
//			if (densityGrid.isInBounds(v.getCoordinate())) {
//				double rho = densityGrid.getValue(v.getCoordinate());
//				vertexValues.put(v, rho);
//			}
		}
		return Partitions.createPartitions(vertexValues, binsize);
	}

	public static Distribution travelTimeDistribution(Set<? extends SpatialSparseVertex> vertices, ZoneLayer zones, TravelTimeMatrix matrix) {
		Distribution distr = new Distribution();
		
		if(zoneCache == null) {
			precacheZones(vertices, zones);
		}
		
		for(SpatialSparseVertex v_i : vertices) {
//			Zone z_i = zones.getZone(v_i.getCoordinate());
			Zone z_i = zoneCache.get(v_i);
			if(z_i != null) {
				for(SpatialSparseVertex v_j : v_i.getNeighbours()) {
					Zone z_j = zones.getZone(v_j.getCoordinate());
					if(z_j != null) {
						double tt = matrix.getTravelTime(z_i, z_j);
						distr.add(tt);
					} else {
//						System.err.println("null zone!");
					}
				}
			} else {
//				System.err.println("null zone!");
			}
		}
		
		return distr;
	}
	
	public static Map<Vertex, Zone> zoneCache; //FIXME!
	
	public static <V extends SpatialSparseVertex> TObjectDoubleHashMap<V> meanTravelTime(Set<V> vertices, ZoneLayer zones, TravelTimeMatrix matrix) {
		TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
		
		if(zoneCache == null) {
			precacheZones(vertices, zones);
		}
		
		for(V v_i : vertices) {
//			Zone z_i = zones.getZone(v_i.getCoordinate());
			Zone z_i = zoneCache.get(v_i);
			if(z_i != null) {
				double sum = 0;
				int count = 0;
				for(SpatialSparseVertex v_j : v_i.getNeighbours()) {
					Zone z_j = zones.getZone(v_j.getCoordinate());
					if(z_j != null) {
						sum += matrix.getTravelTime(z_i, z_j);
						count++;
					}
				}
				values.put(v_i, sum/(double)count);
			}
		}
		
		return values;
	}
	
	public static void precacheZones(Set<? extends SpatialSparseVertex> vertices, ZoneLayer zones) { // FIXME!
		System.out.println("Precaching zones...");
		zoneCache = new HashMap<Vertex, Zone>();
		for(SpatialSparseVertex v_i : vertices) {
			Zone z_i = zones.getZone(v_i.getCoordinate());
			zoneCache.put(v_i, z_i);
		}
	}
}
