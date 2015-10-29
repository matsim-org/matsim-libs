/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.cadyts;

import cadyts.measurements.SingleLinkMeasurement.TYPE;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 * 
 */
public class ODCountsAnalyzer implements AfterMobsimListener {

	private static final Logger logger = Logger.getLogger(ODCountsAnalyzer.class);

	private final Counts<Link> counts;

	private final SimResultsAdaptor simResults;

	public ODCountsAnalyzer(Counts counts, SimResultsAdaptor simResults) {
		this.counts = counts;
		this.simResults = simResults;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Network network = event.getControler().getScenario().getNetwork();
		DescriptiveStatistics diff = new DescriptiveStatistics();
		DescriptiveStatistics absDiff = new DescriptiveStatistics();
		DescriptiveStatistics error = new DescriptiveStatistics();
		DescriptiveStatistics absError = new DescriptiveStatistics();

		try {
			String file = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "odCounts.txt");
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));

			writer.write("id\tobs\tsim");

			writer.newLine();

			for (Count count : counts.getCounts().values()) {
				if (count.getLocId().toString().startsWith(ODCalibrator.VIRTUAL_ID_PREFIX)) {
					Link link = network.getLinks().get(count.getLocId());
					double refVal = count.getMaxVolume().getValue() * 24;
					double simVal = simResults.getSimValue(link, 0, 86400, TYPE.COUNT_VEH);

					double err = (simVal - refVal) / refVal;
					error.addValue(err);
					absError.addValue(Math.abs(err));

					double delta = simVal - refVal;
					diff.addValue(delta);
					absDiff.addValue(Math.abs(delta));
					
					writer.write(link.getId().toString());
					writer.write("\t");
					writer.write(String.valueOf(refVal));
					writer.write("\t");
					writer.write(String.valueOf(simVal));
					writer.newLine();
				}
			}
			writer.close();

			logger.info(String.format("OD-relations diff: avr = %s, median = %s, var = %s, min = %s, max = %s", diff.getMean(),
					diff.getPercentile(50), diff.getVariance(), diff.getMin(), diff.getMax()));
			logger.info(String.format("OD-relations absolute diff: avr = %s, median = %s, var = %s, min = %s, max = %s", absDiff.getMean(),
					absDiff.getPercentile(50), absDiff.getVariance(), absDiff.getMin(), absDiff.getMax()));

			
			logger.info(String.format("Relative OD-relations error: avr = %s, median = %s, var = %s, min = %s, max = %s", error.getMean(),
					error.getPercentile(50), error.getVariance(), error.getMin(), error.getMax()));
			logger.info(String.format("Absolute relative OD-relations error: avr = %s, median = %s, var = %s, min = %s, max = %s",
					absError.getMean(), absError.getPercentile(50), absError.getVariance(), absError.getMin(), absError.getMax()));
			
			
			file = event.getControler().getControlerIO().getOutputFilename("odCountsDiff.txt");
			writeStats(file, diff, event.getIteration());
			
			file = event.getControler().getControlerIO().getOutputFilename("odCountsAbsDiff.txt");
			writeStats(file, absDiff, event.getIteration());
			
			file = event.getControler().getControlerIO().getOutputFilename("odCountsError.txt");
			writeStats(file, error, event.getIteration());
			
			file = event.getControler().getControlerIO().getOutputFilename("odCountsAbsError.txt");
			writeStats(file, absError, event.getIteration());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeStats(String file, DescriptiveStatistics stats, int iteration) throws IOException {
		boolean append = true;
		if(iteration == 0) append = false;
		
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
		if(!append) {
			writer.write("avr\tmedian\tvar\tmin\tmax");
			writer.newLine();
		}
		
		writer.write(String.valueOf(stats.getMean()));
		writer.write("\t");
		writer.write(String.valueOf(stats.getPercentile(50)));
		writer.write("\t");
		writer.write(String.valueOf(stats.getVariance()));
		writer.write("\t");
		writer.write(String.valueOf(stats.getMin()));
		writer.write("\t");
		writer.write(String.valueOf(stats.getMax()));
		writer.newLine();
		writer.close();
	}

}
