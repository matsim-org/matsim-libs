/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceTask.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;


/**
 * @author illenberger
 *
 */
public class DistanceTask extends ModuleAnalyzerTask<Distance> {

	private static final Logger logger = Logger.getLogger(DistanceTask.class);
	
	public static final String MEAN_EDGE_LENGTH = "d_mean";
	
	public static final String MEDIAN_EDGE_LENGTH = "d_median";
	
	public static final String MIN_EDGE_LENGTH = "d_min";
	
	public static final String MAX_EDGE_LENGTH = "d_max";
	
	public static final String MEAN_EDGE_LENGTH_I_SUM = "d_i_sum_mean";
	
	public static final String MEDIAN_EDGE_LENGTH_I_SUM = "d_i_sum_median";
	
	public static final String MIN_EDGE_LENGTH_I_SUM = "d_i_sum_min";
	
	public static final String MAX_EDGE_LENGTH_I_SUM = "d_i_sum_max";
	
	public static final String MEAN_EDGE_LENGTH_I = "d_i_mean";
	
	public static final String MEDIAN_EDGE_LENGTH_I = "d_i_median";
	
	public static final String MIN_EDGE_LENGTH_I = "d_i_min";
	
	public static final String MAX_EDGE_LENGTH_I = "d_i_max";
	
	public DistanceTask() {
		setModule(new Distance());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
			DescriptiveStatistics distr2 = module.statistics((Set<? extends SpatialVertex>) graph.getVertices());
			double d_mean = distr2.getMean();
			double d_max = distr2.getMax();
			double d_min = distr2.getMin();
			stats.put(MEAN_EDGE_LENGTH, d_mean);
			stats.put(MAX_EDGE_LENGTH, d_max);
			stats.put(MIN_EDGE_LENGTH, d_min);
			stats.put(MEDIAN_EDGE_LENGTH, StatUtils.percentile(distr2.getValues(), 50));

			logger.info(String.format("d_mean = %1$.4f, d_max = %2$.4f, d_min = %3$.4f", d_mean, d_max, d_min));
			
			Discretizer discretizer = new LinearDiscretizer(1000.0);
			if(getOutputDirectory() != null) {
				try {
					writeHistograms(distr2, discretizer, "d", false);
					writeHistograms(distr2, "d", 500, 100);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			Distribution distr = module.vertexMeanDistribution((Set<? extends SpatialVertex>) graph.getVertices());
			double d_i_mean = distr.mean();
			double d_i_max = distr.max();
			double d_i_min = distr.min();
			stats.put(MEAN_EDGE_LENGTH_I, d_i_mean);
			stats.put(MAX_EDGE_LENGTH_I, d_i_max);
			stats.put(MIN_EDGE_LENGTH_I, d_i_min);
			stats.put(MEDIAN_EDGE_LENGTH_I, StatUtils.percentile(distr.getValues(), 50));
			
			logger.info(String.format("d_i_mean = %1$.4f, d_i_max = %2$.4f, d_i_min = %3$.4f", d_i_mean, d_i_max, d_i_min));
			
			if(getOutputDirectory() != null) {
				try {
					writeHistograms(distr, 5000.0, true, "d_i");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			distr = module.vertexAccumulatedDistribution((Set<? extends SpatialVertex>) graph.getVertices());
			double d_i_sum_mean = distr.mean();
			double d_i_sum_max = distr.max();
			double d_i_sum_min = distr.min();
			stats.put(MEAN_EDGE_LENGTH_I_SUM, d_i_sum_mean);
			stats.put(MAX_EDGE_LENGTH_I_SUM, d_i_sum_max);
			stats.put(MIN_EDGE_LENGTH_I_SUM, d_i_sum_min);
			stats.put(MEDIAN_EDGE_LENGTH_I_SUM, StatUtils.percentile(distr.getValues(), 50));
			
			logger.info(String.format("d_i_sum_mean = %1$.4f, d_i_sum_max = %2$.4f, d_i_sum_min = %3$.4f", d_i_sum_mean, d_i_sum_max, d_i_sum_min));
			
			if(getOutputDirectory() != null) {
				try {
					writeHistograms(distr, 1000.0, true, "d_i_sum");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		

			distr = module.vertexMedianDistribution((Set<? extends SpatialVertex>) graph.getVertices());
			System.err.println("<d_i_median>=" + distr.mean());
			try {
				Distribution.writeHistogram(distr.absoluteDistribution(1000), getOutputDirectory()+"d_i_meadin.txt");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
