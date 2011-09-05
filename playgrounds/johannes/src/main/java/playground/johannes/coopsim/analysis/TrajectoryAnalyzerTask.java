/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryAnalyzerTask.java
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
package playground.johannes.coopsim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public abstract class TrajectoryAnalyzerTask {

	private static final Logger logger = Logger.getLogger(TrajectoryAnalyzerTask.class);
	
	private String output;
	
	public void setOutputDirectory(String outputDir) {
		this.output = outputDir;
	}
	
	public String getOutputDirectory() {
		return output;
	}
	
	protected boolean outputDirectoryNotNull() {
		if(getOutputDirectory() == null) {
			logger.warn("No output directory specified.");
			return false;
		} else {
			return true;
		}
	}
	
	public abstract void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results);
		
	protected void writeHistograms(DescriptiveStatistics stats, String name, int bins, int minsize) throws IOException {
		double[] values = stats.getValues();
		if (values.length > 0) {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(values, minsize, bins), true);
			Histogram.normalize(hist);
			TXTWriter.writeMap(hist, name, "p", String.format("%1$s/%2$s.strat.txt", getOutputDirectory(), name, values.length / bins));
		} else {
			logger.warn("Cannot create histogram. No samples.");
		}
	}
	
	protected void writeHistograms(DescriptiveStatistics stats, Discretizer discretizer, String name, boolean reweight) throws IOException {
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, discretizer, reweight);
		TXTWriter.writeMap(hist, name, "n", String.format("%1$s/%2$s.txt", output, name)); 
		Histogram.normalize(hist);
		TXTWriter.writeMap(hist, name, "p", String.format("%1$s/%2$s.share.txt", output, name));
	}
}
