/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbability.java
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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.ProgressLogger;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class AcceptanceProbability {

	private static final Logger logger = Logger.getLogger(AcceptanceProbability.class);
	
//	private Discretizer discretizer = new LinearDiscretizer(1000);

	private DistanceCalculator distanceCalculator = new OrthodromicDistanceCalculator();

	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.distanceCalculator = calculator;
	}

	public DescriptivePiStatistics distribution(Set<? extends SpatialVertex> vertices, Set<Point> choiceSet) {
//		logger.info("Building spatial index...");
//		SpatialIndex spatialIndex = new Quadtree();
//		for(Point p : choiceSet) {
//			spatialIndex.insert(p.getEnvelopeInternal(), p);
//		}
		
		DescriptivePiStatistics distribution = new DescriptivePiStatistics();
//		Set<SpatialEdge> touched = new HashSet<SpatialEdge>();

		logger.info("Calculating acceptance probability...");
		ProgressLogger.init(vertices.size(), 1, 5);
		for (SpatialVertex vertex : vertices) {
			Point p1 = vertex.getPoint();
			if(p1 != null) {
//			TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
			TDoubleArrayList ds = new TDoubleArrayList(choiceSet.size());
			for (Point p2 : choiceSet) {
				if (p2 != null) {
//					double d = distanceCalculator.distance(p1, p2);
//					n_d.adjustOrPutValue(discretizer.discretize(d), 1, 1);
					ds.add(distanceCalculator.distance(p1, p2));
				}
			}
			double[] dArray = ds.toNativeArray();
			Discretizer discretizer = FixedSampleSizeDiscretizer.create(dArray, 100, 500);
			TDoubleDoubleHashMap n_d = Histogram.createHistogram(dArray, discretizer, true);

			for (int i = 0; i < vertex.getEdges().size(); i++) {
				SpatialEdge e = vertex.getEdges().get(i);
//				if (touched.add(e)) {
					SpatialVertex neighbor = e.getOpposite(vertex);

					if (neighbor.getPoint() != null) {
						double d = distanceCalculator.distance(p1, neighbor.getPoint());
						if(d > 0) {
//							if(!n_d.containsKey(d)) {
//								Envelope env = new Envelope(p1.getX() - d, p1.getX() + d, p1.getY() - d, p1.getY() + d);
//								List<Point> points = spatialIndex.query(env);
//								for (Point p2 : points) {
//									double d2 = distanceCalculator.distance(p1, p2);
//									n_d.adjustOrPutValue(discretizer.discretize(d2), 1, 1);
//								}
//							}
							double n = n_d.get(discretizer.discretize(d));
							
							if (n > 0)
								distribution.addValue(d, n);
						}
					}
				}
			}
			
			ProgressLogger.step();
		}
		return distribution;
	}
}
