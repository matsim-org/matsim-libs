/* *********************************************************************** *
 * project: org.matsim.*
 * TimeSamplerTest.java
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
package playground.johannes.studies.coopsim;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.math.GaussDistribution;
import org.matsim.contrib.socnetgen.sna.math.LogNormalDistribution;

import java.io.IOException;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class TimeSamplerTest {

	/**
	 * @param args
	 * @throws FunctionEvaluationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws FunctionEvaluationException, IOException {
		Random random = new Random();
		
		AdditiveDistribution arrTimePDF = new AdditiveDistribution();
		arrTimePDF.addComponent(new GaussDistribution(2151, 43030, 267));
		arrTimePDF.addComponent(new GaussDistribution(14227, 58036, 1382));
		
		TimeSampler arrivalSampler = new TimeSampler(arrTimePDF, 86400, random);

		AdditiveDistribution arrDurPDF = new AdditiveDistribution();
		arrDurPDF.addComponent(new GaussDistribution(7210, 41513, 133759479));
		arrDurPDF.addComponent(new GaussDistribution(15660, 73033, 277912890));
		
//		TimeSampler arrDurSampler = new TimeSampler(arrDurPDF, 86400, random);
//		
//		LogNormalDistribution durPDF = new LogNormalDistribution(0.6883, 8.4954, 522.9869);
//		TimeSampler durSampler = new TimeSampler(durPDF, 86400, random);
		DescriptiveStatistics durations = new DescriptiveStatistics();
		DescriptiveStatistics arrivals = new DescriptiveStatistics();
		
		ProgressLogger.init(10000, 1, 5);
		double sigma = 0.6883;
		for(int i = 0; i < 10000; i++) {
			int t_arr = arrivalSampler.nextSample();
			int dur_mean = (int) arrDurPDF.value(t_arr);
			if(dur_mean > 0) {
			double mu = Math.log(dur_mean) - Math.pow(sigma, 2)/2.0;
			
			TimeSampler sampler = new TimeSampler(new LogNormalDistribution(sigma, mu, 522), 86400, random);
			double dur = sampler.nextSample();
			durations.addValue(dur);
			arrivals.addValue(t_arr);
			ProgressLogger.step();
			}
		}
		
		
		TDoubleDoubleHashMap hist = Histogram.createHistogram(durations, FixedSampleSizeDiscretizer.create(durations.getValues(), 1, 30), true);
		Histogram.normalize(hist);
		StatsWriter.writeHistogram(hist, "t", "n", "/Users/jillenberger/Work/socialnets/locationChoice/output/durations.txt");

		TDoubleDoubleHashMap correl = Correlations.mean(arrivals.getValues(), durations.getValues(), FixedSampleSizeDiscretizer.create(arrivals.getValues(), 1, 24));
		StatsWriter.writeHistogram(correl, "arr", "dur", "/Users/jillenberger/Work/socialnets/locationChoice/output/dur_arr.txt");
	}

}
