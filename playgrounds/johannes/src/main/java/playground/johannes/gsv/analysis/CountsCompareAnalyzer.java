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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

import playground.johannes.gsv.gis.CountsCompare2GeoJSON;
import playground.johannes.gsv.gis.NetworkLoad2GeoJSON;
import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.sna.math.DescriptivePiStatistics;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 * 
 */
public class CountsCompareAnalyzer implements IterationEndsListener {

	private static final Logger logger = Logger.getLogger(CountsCompareAnalyzer.class);

	private final LinkOccupancyCalculator calculator;

	private final double factor;

	private final Counts counts;

	public CountsCompareAnalyzer(LinkOccupancyCalculator calculator, String countsFile, double factor) {
		this.calculator = calculator;
		this.factor = factor;

		counts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.parse(countsFile);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
        Network network = event.getControler().getScenario().getNetwork();
		DescriptiveStatistics error = new DescriptiveStatistics();
		DescriptiveStatistics errorAbs = new DescriptiveStatistics();
		DescriptivePiStatistics errorWeighted = new WSMStatsFactory().newInstance();
		
		TDoubleArrayList errorVals = new TDoubleArrayList();
		TDoubleArrayList caps = new TDoubleArrayList();
		TDoubleArrayList speeds = new TDoubleArrayList();

		for (Count count : counts.getCounts().values()) {
			double obsVal = 0;
			for(int i = 1; i < 25; i++) {
				obsVal += count.getVolume(i).getValue();
			}
			
			if (obsVal > 0) {
				double simVal = calculator.getOccupancy(count.getLocId());
				simVal *= factor;

				double err = (simVal - obsVal) / obsVal;
				
				error.addValue(err);
				errorAbs.addValue(Math.abs(err));
				errorWeighted.addValue(Math.abs(err), 1/obsVal);

				Link link = network.getLinks().get(count.getLocId());
				errorVals.add(Math.abs(err));
				caps.add(link.getCapacity());
				speeds.add(link.getFreespeed());
			}
		}

		logger.info(String.format("Relative counts error: mean = %s, var = %s, stderr = %s, min = %s, max = %s", error.getMean(), error.getVariance(), error.getStandardDeviation(), error.getMin(),
				error.getMax()));
		logger.info(String.format("Absolute relative counts error: mean = %s, var = %s, stderr = %s, min = %s, max = %s", errorAbs.getMean(), errorAbs.getVariance(), errorAbs.getStandardDeviation(), errorAbs.getMin(),
				errorAbs.getMax()));
		logger.info(String.format("Absolute weigthed relative counts error: mean = %s, var = %s, stderr = %s, min = %s, max = %s", errorWeighted.getMean(), errorWeighted.getVariance(), errorWeighted.getStandardDeviation(), errorWeighted.getMin(),
				errorWeighted.getMax()));

		String outdir = event.getControler().getControlerIO().getIterationPath(event.getIteration());

		try {
			TDoubleDoubleHashMap map = Correlations.mean(caps.toNativeArray(), errorVals.toNativeArray());
			TXTWriter.writeMap(map, "capacity", "counts", String.format("%s/countsError.capacity.txt", outdir));
			
			map = Correlations.mean(speeds.toNativeArray(), errorVals.toNativeArray());
			TXTWriter.writeMap(map, "speed", "counts", String.format("%s/countsError.speed.txt", outdir));
			
			TXTWriter.writeMap(Histogram.createHistogram(error, new LinearDiscretizer(0.1), false), "Error", "Frequency", String.format("%s/countsError.hist.txt", outdir));
			TXTWriter.writeMap(Histogram.createHistogram(errorAbs, new LinearDiscretizer(0.1), false), "Error (absolute)", "Frequency", String.format("%s/countsErrorAbs.hist.txt", outdir));
			TXTWriter.writeMap(Histogram.createHistogram(errorWeighted, new LinearDiscretizer(0.1), true), "Error (weighted)", "Frequency", String.format("%s/countsErrorWeighted.hist.txt", outdir));
			
			CountsCompare2GeoJSON.write(calculator, counts, factor, network, outdir);
			NetworkLoad2GeoJSON.write(event.getControler().getScenario().getNetwork(), calculator, factor, outdir + "/network.json");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String rootOutDir = event.getControler().getControlerIO().getOutputPath();
		boolean append = false;
		if(event.getIteration() > 0) {
			append = true;
		}
		writeErrorFile(error, String.format("%s/countsError.txt", rootOutDir), append);
		writeErrorFile(errorAbs, String.format("%s/countsAbsError.txt", rootOutDir), append);
	}
	
	private void writeErrorFile(DescriptiveStatistics error, String file, boolean append) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, append));
			if(!append) {
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
