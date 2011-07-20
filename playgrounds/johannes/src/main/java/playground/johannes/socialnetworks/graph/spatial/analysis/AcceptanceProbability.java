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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.util.MultiThreading;
import org.matsim.contrib.sna.util.ProgressLogger;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class AcceptanceProbability {

	private static final Logger logger = Logger.getLogger(AcceptanceProbability.class);

//	private DistanceCalculator distanceCalculator = new OrthodromicDistanceCalculator();
	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();

	private int numThreads;
	
	public AcceptanceProbability() {
		numThreads = MultiThreading.getNumAllowedThreads();
	}
	
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}
	
	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.distanceCalculator = calculator;
	}

	public DescriptivePiStatistics distribution(Set<? extends SpatialVertex> vertices, Set<Point> choiceSet) {
		DescriptivePiStatistics distribution = new DescriptivePiStatistics();
	
		logger.info("Calculating acceptance probability...");
		ProgressLogger.init(vertices.size(), 1, 5);
		/*
		 * calculate thread size
		 */
		List<Calculator> threads = new ArrayList<Calculator>();
		int size = (int) Math.floor(vertices.size()/ (double) numThreads);
		int i_start = 0;
		int i_stop = size;
		/*
		 * init threads
		 */
		Iterator<? extends SpatialVertex> it = vertices.iterator();
		for (int i = 0; i < numThreads - 1; i++) {
			int subSize = i_stop - i_start;
			List<SpatialVertex> subVertices = new ArrayList<SpatialVertex>(subSize);
			for(int k = 0; k < subSize; k++) {
				subVertices.add(it.next());
			}
			threads.add(new Calculator(subVertices, choiceSet));
			i_start = i_stop;
			i_stop += size;
		}
		
		int subSize = vertices.size() - i_start;
		List<SpatialVertex> subVertices = new ArrayList<SpatialVertex>(subSize);
		for(int k = 0; k < subSize; k++) {
			subVertices.add(it.next());
		}
		threads.add(new Calculator(subVertices, choiceSet));
		/*
		 * start threads
		 */
		for (Calculator thread : threads) {
			thread.start();
		}
		/*
		 * wait for threads
		 */
		for (Calculator thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ProgressLogger.termiante();
		/*
		 * merge results
		 */
		for (Calculator thread : threads) {
			for(int i = 0; i < thread.edgeLengths.size(); i++) {
				distribution.addValue(thread.edgeLengths.get(i), thread.oppCounts.get(i));
			}
		}
		
		return distribution;
	}

	private class Calculator extends Thread {

		private List<SpatialVertex> vertices;

		private Set<Point> opportunities;

		private TDoubleArrayList edgeLengths;

		private TDoubleArrayList oppCounts;

		public Calculator(List<SpatialVertex> vertices, Set<Point> opportunities) {
			this.vertices = vertices;
			this.opportunities = opportunities;
			edgeLengths = new TDoubleArrayList(vertices.size() * 20);
			oppCounts = new TDoubleArrayList(vertices.size() * 20);
		}

		public void run() {
			for (SpatialVertex vertex : vertices) {
				Point p1 = vertex.getPoint();
				if (p1 != null) {

					TDoubleArrayList distances = new TDoubleArrayList(opportunities.size());
					for (Point p2 : opportunities) {
						if (p2 != null) {
							distances.add(distanceCalculator.distance(p1, p2));
						}
					}
					double[] distanceArray = distances.toNativeArray();
					Discretizer discretizer = FixedSampleSizeDiscretizer.create(distanceArray, 200, 300);
					TDoubleDoubleHashMap n_d = Histogram.createHistogram(distanceArray, discretizer, true);

					for (int i = 0; i < vertex.getEdges().size(); i++) {
						SpatialEdge e = vertex.getEdges().get(i);

						SpatialVertex neighbor = e.getOpposite(vertex);

						if (neighbor.getPoint() != null) {
							double d = distanceCalculator.distance(p1, neighbor.getPoint());
							if (d > 0) {
								double n = n_d.get(discretizer.discretize(d));

								if (n > 0) {
									edgeLengths.add(d);
									oppCounts.add(n);
								}
							}
						}
					}
				}

				ProgressLogger.step();
			}
		}
	}
}
