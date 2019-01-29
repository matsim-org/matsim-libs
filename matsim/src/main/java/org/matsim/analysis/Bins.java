/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;

public class Bins {

	protected double interval;
	protected int numberOfBins;
	protected double maxVal;
	protected String desc;
	protected List<BinEntry> entries = new Vector<BinEntry>();
	protected double [] bins;

	private final static Logger log = Logger.getLogger(Bins.class);

	public Bins(double interval, double maxVal, String desc) {
		this.interval = interval;
		this.maxVal = maxVal;
		this.numberOfBins = (int)Math.ceil(maxVal / interval);
		this.desc = desc;
		this.bins = new double[this.numberOfBins];
	}
	public void addValues(double[] values, double[] weights) {
		for (int index = 0; index < values.length; index++) {
			this.addVal(values[index], weights[index]);
		}
	}

	public void addVal(double value, double weight) {
		int index = (int)Math.floor(value / interval);
		// values > maximum value are assigned to the last bin
		if (value >= maxVal) {
			index = this.numberOfBins -1; 
		}

		// values < 0.0 value are assigned to the first bin
		if (value < 0.0) {
			log.error("Value < 0.0 received");
			index = 0;
		}
		
		this.bins[index] += weight;
		this.entries.add(new BinEntry(value, weight));
	}

	public void clear() {
		this.entries.clear();
		this.bins = new double[this.numberOfBins];
	}

	public void plotBinnedDistribution(String path, String xLabel, String xUnit) {
		String [] categories  = new String[this.numberOfBins];
		for (int i = 0; i < this.numberOfBins; i++) {
			categories[i] = Integer.toString(i);
		}
		Double[] values = new Double[this.entries.size()];
		Double[] weights = new Double[this.entries.size()];

		for (int index = 0; index < this.entries.size(); index++) {
			values[index] = this.entries.get(index).getValue();
			weights[index] = this.entries.get(index).getWeight();
		}

		DecimalFormat formatter = new DecimalFormat("0.0000");
		String s = xLabel + " " +
		"[interval = " + formatter.format(this.interval) + xUnit + "]" +
		"[number of entries = " + this.entries.size() + "]" +
		"[mean = " + formatter.format(this.weightedMean(values, weights)) + xUnit + "]" +
		"[median = " + formatter.format(this.median(values)) + xUnit + "]" +
		"[max = " + formatter.format(this.getMax(values)) + xUnit + "]";

		BarChart chart =
			new BarChart(desc, s , "#", categories);
		chart.addSeries("Bin size", this.bins);
		chart.saveAsPng(path + desc + ".png", 1600, 800);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(path + desc + ".txt");
			out.write("Bin [interval = " + this.interval + " " + xUnit  + "]\t" + "#" + "\n");
			for (int j = 0; j < bins.length;  j++) {
				out.write(j + "\t" + bins[j] + "\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public double[] getBins() {
		return bins;
	}
	public void setBins(double[] bins) {
		this.bins = bins;
	}
		
	public double getInterval() {
		return interval;
	}
	public void setInterval(double interval) {
		this.interval = interval;
	}
	// ----------------------------------------------------------------------------
	private double median(Double [] values) {
		List<Double> list = new Vector<Double>();

        Collections.addAll(list, values);
		return median(list);
	}
	
	public double median(List<Double> values) {

		if (values.size() == 0) return 0.0;

	    Collections.sort(values);
	    if (values.size() % 2 != 0) {
	    	return values.get((values.size()+1)/2-1);
	    }
	    else {
	    	double lower = values.get(values.size()/2-1);
	    	double upper = values.get(values.size()/2);
	    	return (lower + upper) / 2.0;
	    }
	}
	
	public double mean(List<Double> values) {
		double sum = 0.0;
		int cnt = 0;
		if (values.size() == 0) return 0.0;
		for (Double value : values) {
			sum += value;
			cnt++;
		}
		return sum / cnt;
	}
	
	public double weightedMean(List<Double> values, List<Double> weights) {
		return weightedMean(values.toArray(new Double[values.size()]), weights.toArray(new Double[weights.size()]));
	}
	
	public double weightedMean(Double[] values, Double[] weights) {
		double sumValues = 0.0;
		double sumWeights = 0.0;
		
		if (values.length == 0) return 0.0;
		
		if (values.length  != weights.length ) {
			log.info("size of weights and values not identical");
			return -1;
		}
		for (int index = 0; index < values.length; index++) {
			sumValues += (values[index] * weights[index]);
			sumWeights += weights[index];
		}
		return sumValues / sumWeights;
	}
	
	public double getMax(List<Double> values) {
		return getMax(values.toArray(new Double[values.size()]));
	}
	
	public double getMax(Double[] values) {
		double maxVal = Double.MIN_VALUE;
		for (Double v : values) {
			if (v > maxVal) {
				maxVal = v;
			}
		}
		return maxVal;
	}
}
