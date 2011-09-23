/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixAPL.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.matrix;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.util.MultiThreading;
import org.matsim.contrib.sna.util.ProgressLogger;

/**
 * @author illenberger
 * 
 */
public class MatrixAPL {

	private static final Logger logger = Logger.getLogger(MatrixAPL.class);
	
	private int numThreads;
	
	private boolean calcDistr = true;
	
	public MatrixAPL() {
		this.numThreads = MultiThreading.getNumAllowedThreads();
	}
	
	public MatrixAPL(int numThreads) {
		this.numThreads = numThreads;
	}
	
	public void setCalcAPLDistribution(boolean flag) {
		this.calcDistr = flag;
	}
	
	public DescriptiveStatistics apl(AdjacencyMatrix<?> y) {
		List<APLThread> threads = new ArrayList<APLThread>();
		int size = (int) Math.floor(y.getVertexCount() / (double) numThreads);
		int i_start = 0;
		int i_stop = size;
		for (int i = 0; i < numThreads - 1; i++) {
			threads.add(new APLThread(y, new SingelPathDijkstra(y, new CostFunction()), i_start, i_stop, calcDistr));
			i_start = i_stop;
			i_stop += size;
		}
		threads.add(new APLThread(y, new SingelPathDijkstra(y, new CostFunction()), i_start, y.getVertexCount(), calcDistr));
		/*
		 * start threads
		 */
		logger.info(String.format("Calculating average path lenth on %1$s threads.", numThreads));
		ProgressLogger.init(y.getVertexCount(), 1, 5);
		for (APLThread thread : threads) {
			thread.start();
		}
		/*
		 * wait for threads
		 */
		for (APLThread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		/*
		 * merge results
		 */
		DescriptiveStatistics stats = new DescriptiveStatistics();
		if(calcDistr) {
			for (APLThread thread : threads) {
				TDoubleArrayList vals = thread.getValues();
				for(int i = 0; i < vals.size(); i++) {
					stats.addValue(vals.get(i));
				}
			}
		} else {
			long lengthSum = 0;
			long pathCount = 0;
			for (APLThread thread : threads) {
				lengthSum += thread.lengthSum;
				pathCount += thread.pathCount;
			}
			stats.addValue(lengthSum/(double)pathCount);
		}
		
		return stats;
	}


	private class APLThread extends Thread {

		private final AdjacencyMatrix<?> y;

		private final SingelPathDijkstra dijkstra;

		private final int startIdx;

		private final int endIdx;

		private final TDoubleArrayList stats;

		private final boolean calcDistr;
		
		private long lengthSum;
		
		private long pathCount;

		APLThread(AdjacencyMatrix<?> y, SingelPathDijkstra dijkstra, int startIdx, int endIdx, boolean calcDistr) {
			this.y = y;
			this.dijkstra = dijkstra;
			this.startIdx = startIdx;
			this.endIdx = endIdx;
			this.calcDistr = calcDistr;
			if(calcDistr)
				this.stats = new TDoubleArrayList((int) ((endIdx - startIdx) * y.getVertexCount() / 2.0));
			else
				this.stats = new TDoubleArrayList();
		}
		
		TDoubleArrayList getValues() {
			return stats;
		}
		
		@Override
		public void run() {
			for (int i = startIdx; i < endIdx; i++) {
				dijkstra.run(i, -1);
				for (int j = i + 1; j < y.getVertexCount(); j++) {
					TIntArrayList path = dijkstra.getPath(i, j);
					if (path != null) {
						if (calcDistr)
							stats.add(path.size());
						else {
							lengthSum += path.size();
							pathCount++;
						}
					}
				}
				ProgressLogger.step();
			}
		}

	}
	
	private class CostFunction implements EdgeCostFunction {

		@Override
		public double edgeCost(int i, int j) {
			return 1;
		}
		
	}
}
