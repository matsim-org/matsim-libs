/* *********************************************************************** *
 * project: org.matsim.*
 * FractalDim.java
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
package playground.johannes.studies.gis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.spatial.Geometries;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class FractalDim {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String pointFile = args[0];
//		String pointFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml";
		String polyFile = args[1];
//		String polyFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp";
		String outFile = args[2];
//		String outFile = "/Users/jillenberger/Work/socialnets/spatialchoice/fdim.point-point.boundary.txt";
		
		Set<Point> targetPoints = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(pointFile);

		for(SpatialVertex v : graph2.getVertices()) {	
//			if(Math.random() < 0.1)
				targetPoints.add(v.getPoint());
		}

		Feature feature = FeatureSHP.readFeatures(polyFile).iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		geometry.setSRID(21781);
		
//		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
//		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
//		Set<? extends SpatialVertex> egos = SnowballPartitions.createSampledPartition(graph.getVertices());
//		Set<Point> startPoints = new HashSet<Point>();
//		for(SpatialVertex v : egos)
//			if(v.getPoint() != null)
//				startPoints.add(v.getPoint());
		
		DescriptivePiStatistics stats = calcuate(targetPoints, targetPoints, geometry);
		
//		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new FixedSampleSizeDiscretizer().create(stats.getValues(), 1000));
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(1000.0));
		hist = Histogram.normalize(hist);
		TXTWriter.writeMap(hist, "d", "n", outFile);
	}

	public static DescriptivePiStatistics calcuate(Set<Point> startPoints, Set<Point> targetPoints, Geometry boundary) {
		DescriptivePiStatistics stats = new DescriptivePiStatistics();
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		Discretizer discretizer = new LinearDiscretizer(1000.0);
//		Discretizer pointDiscretizer = new LinearDiscretizer(100.0);
		int cnt = 0;
		
		
		List<Point> startList = new ArrayList<Point>(startPoints);
		
		int N = startList.size() * (startList.size() - 1) /2;
//		int N = startPoints.size() * targetPoints.size();
//		
		
		Point lastPoint = startList.get(0);
		for(int i = 0; i < startList.size(); i++) {
			Point p1 = startList.get(i);
			TIntObjectHashMap<Geometry> geoCache = new TIntObjectHashMap<Geometry>();
//		for(Point p1 : startPoints) {
			for(int j = i+1; j < startList.size(); j++) {
				Point p2 = startList.get(j);
//			for(Point p2 : targetPoints) {
				double d = dCalc.distance(p1, p2);
				int idx = (int) discretizer.index(d); 

				Geometry outer = geoCache.get(idx);
				if(outer == null) {
					outer = Geometries.makePolygonFromCircle(discretizer.discretize(d), p1.getX(), p1.getY());
					geoCache.put(idx, outer);
				}
				Geometry inner = geoCache.get(idx-1);
				if(inner == null) {
					inner = Geometries.makePolygonFromCircle(discretizer.discretize(d-1000), p1.getX(), p1.getY());
					geoCache.put(idx-1, inner);
				}

				lastPoint = p1;
				
				double a_outer = boundary.intersection(outer).getArea()/(1000.0*1000.0);
				double a_inner = boundary.intersection(inner).getArea()/(1000.0*1000.0);
				double a = a_outer - a_inner;
				stats.addValue(d, a);
				
				cnt++;
				
				if(cnt % 1000 == 0)
					System.out.println(String.format("Processed %1$s ", cnt/(double)N*100));
			}
		}
		
		return stats;
	}
	
	private static void moveGeometry(final double x, final double y, Geometry geometry) {
		for(Coordinate c : geometry.getCoordinates()) {
			c.x = c.x + x;
			c.y = c.y + y;
		}
		geometry.geometryChanged();
	}
}
