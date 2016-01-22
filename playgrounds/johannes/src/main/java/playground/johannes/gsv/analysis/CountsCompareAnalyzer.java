/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.analysis;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.stats.*;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.WSMStatsFactory;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import playground.johannes.gsv.gis.CountsCompare2GeoJSON;
import playground.johannes.gsv.gis.NetworkLoad2GeoJSON;
import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.gsv.sim.cadyts.ODCalibrator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 * 
 */
public class CountsCompareAnalyzer implements AfterMobsimListener {

	private static final Logger logger = Logger.getLogger(CountsCompareAnalyzer.class);

	private final LinkOccupancyCalculator calculator;

	private final double factor;

	private final Counts<Link> counts;

	public CountsCompareAnalyzer(LinkOccupancyCalculator calculator, String countsFile, double factor) {
		this.calculator = calculator;
		this.factor = factor;

		counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.parse(countsFile);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Network network = event.getServices().getScenario().getNetwork();
		DescriptiveStatistics error = new DescriptiveStatistics();
		DescriptiveStatistics errorAbs = new DescriptiveStatistics();
		DescriptivePiStatistics errorWeighted = new WSMStatsFactory().newInstance();

		TDoubleArrayList errorVals = new TDoubleArrayList();
		TDoubleArrayList caps = new TDoubleArrayList();
		TDoubleArrayList speeds = new TDoubleArrayList();

		for (Count count : counts.getCounts().values()) {
			if (!count.getLocId().toString().startsWith(ODCalibrator.VIRTUAL_ID_PREFIX)) {
				double obsVal = 0;
				for (int i = 1; i < 25; i++) {
					obsVal += count.getVolume(i).getValue();
				}

				if (obsVal > 0) {
					double simVal = calculator.getOccupancy(count.getLocId());
					simVal *= factor;

					double err = (simVal - obsVal) / obsVal;

					error.addValue(err);
					errorAbs.addValue(Math.abs(err));
					errorWeighted.addValue(Math.abs(err), 1 / obsVal);

					Link link = network.getLinks().get(count.getLocId());
					errorVals.add(Math.abs(err));
					caps.add(link.getCapacity());
					speeds.add(link.getFreespeed());
				}
			}
		}

		logger.info(String.format("Relative counts error: mean = %s, var = %s, stderr = %s, min = %s, max = %s", error.getMean(),
				error.getVariance(), error.getStandardDeviation(), error.getMin(), error.getMax()));
		logger.info(String.format("Absolute relative counts error: mean = %s, var = %s, stderr = %s, min = %s, max = %s", errorAbs.getMean(),
				errorAbs.getVariance(), errorAbs.getStandardDeviation(), errorAbs.getMin(), errorAbs.getMax()));
		logger.info(String.format("Absolute weigthed relative counts error: mean = %s, var = %s, stderr = %s, min = %s, max = %s",
				errorWeighted.getMean(), errorWeighted.getVariance(), errorWeighted.getStandardDeviation(), errorWeighted.getMin(),
				errorWeighted.getMax()));

		String outdir = event.getServices().getControlerIO().getIterationPath(event.getIteration());

		try {
			TDoubleDoubleHashMap map = Correlations.mean(caps.toArray(), errorVals.toArray());
			StatsWriter.writeHistogram(map, "capacity", "counts", String.format("%s/countsError.capacity.txt", outdir));

			map = Correlations.mean(speeds.toArray(), errorVals.toArray());
			StatsWriter.writeHistogram(map, "speed", "counts", String.format("%s/countsError.speed.txt", outdir));

			StatsWriter.writeHistogram(Histogram.createHistogram(error, new LinearDiscretizer(0.1), false), "Error", "Frequency",
					String.format("%s/countsError.hist.txt", outdir));
			StatsWriter.writeHistogram(Histogram.createHistogram(errorAbs, new LinearDiscretizer(0.1), false), "Error (absolute)", "Frequency",
					String.format("%s/countsErrorAbs.hist.txt", outdir));
			StatsWriter.writeHistogram(Histogram.createHistogram(errorWeighted, new LinearDiscretizer(0.1), true), "Error (weighted)", "Frequency",
					String.format("%s/countsErrorWeighted.hist.txt", outdir));

			CountsCompare2GeoJSON.write(calculator, counts, factor, network, outdir);
			NetworkLoad2GeoJSON.write(event.getServices().getScenario().getNetwork(), calculator, factor, outdir + "/network.json");
		} catch (Exception e) {
			e.printStackTrace();
		}

		String rootOutDir = event.getServices().getControlerIO().getOutputPath();
		boolean append = false;
		if (event.getIteration() > 0) {
			append = true;
		}
		writeErrorFile(error, String.format("%s/countsError.txt", rootOutDir), append);
		writeErrorFile(errorAbs, String.format("%s/countsAbsError.txt", rootOutDir), append);
	}

	private void writeErrorFile(DescriptiveStatistics error, String file, boolean append) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
			if (!append) {
				// write header
				writer.write("mean\tvar\tstderr\tmin\tmax");
				writer.newLine();
			}

			writer.write(String.valueOf(error.getMean()));
			writer.write("\t");
			writer.write(String.valueOf(error.getVariance()));
			writer.write("\t");
			writer.write(String.valueOf(error.getStandardDeviation()));
			writer.write("\t");
			writer.write(String.valueOf(error.getMin()));
			writer.write("\t");
			writer.write(String.valueOf(error.getMax()));
			writer.newLine();

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
