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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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

import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;

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
		Network network = event.getControler().getNetwork();
		DescriptiveStatistics stats = new DescriptiveStatistics();
		TDoubleArrayList vals = new TDoubleArrayList();
		TDoubleArrayList caps = new TDoubleArrayList();

		for (Count count : counts.getCounts().values()) {
			double obsVal = 0;
			for(int i = 1; i < 25; i++) {
				obsVal += count.getVolume(i).getValue();
			}
			
			if (obsVal > 0) {
				double simVal = calculator.getOccupancy(count.getLocId());
				simVal *= factor;

				double err = (simVal - obsVal) / obsVal;

				stats.addValue(err);

				Link link = network.getLinks().get(count.getLocId());
				vals.add(Math.abs(err));
				caps.add(link.getCapacity());
			}
		}

		TDoubleDoubleHashMap map = Correlations.mean(caps.toNativeArray(), vals.toNativeArray());

		logger.info(String.format("mean = %s, var = %s, stderr = %s, min = %s, max = %s", stats.getMean(), stats.getVariance(), stats.getStandardDeviation(), stats.getMin(),
				stats.getMax()));

		String outdir = event.getControler().getControlerIO().getIterationPath(event.getIteration());

		try {
			TXTWriter.writeMap(map, "capacity", "counts", outdir + "/counterr-cap.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String rootOutDir = event.getControler().getControlerIO().getOutputPath();
		boolean append = false;
		if(event.getIteration() > 0) {
			append = true;
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s/counts-err.stats.txt", rootOutDir), append));
			if(!append) {
				// write header
				writer.write("mean\tvar\tstderr\tmin\tmax");
				writer.newLine();
			}
			
			writer.write(String.valueOf(stats.getMean()));
			writer.write("\t");
			writer.write(String.valueOf(stats.getVariance()));
			writer.write("\t");
			writer.write(String.valueOf(stats.getStandardDeviation()));
			writer.write("\t");
			writer.write(String.valueOf(stats.getMin()));
			writer.write("\t");
			writer.write(String.valueOf(stats.getMax()));
			writer.newLine();
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
