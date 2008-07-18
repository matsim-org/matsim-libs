/* *********************************************************************** *
 * project: org.matsim.*
 * Histogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.snowball;

import gnu.trove.TDoubleDoubleHashMap;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.utils.io.IOUtils;

import cern.colt.list.DoubleArrayList;

/**
 * @author illenberger
 *
 */
public class Histogram {

	private DoubleArrayList values = new DoubleArrayList();
	
	private DoubleArrayList weights = new DoubleArrayList();
	
	private DoubleArrayList bins = new DoubleArrayList();
	
	private boolean modified;
	
	private final double[] bounds;
	
	private final double binWidth;
	
	private final int bincount;
	
	public Histogram(double binWidth) {
		this.binWidth = binWidth;
		bounds = null;
		bincount = -1;
	}
	
	public Histogram(double binWidth, double min, double max) {
		this.binWidth = binWidth;
		bounds = new double[]{min, max};
		bincount = -1;
	}
	
	public Histogram(int nBins) {
		bincount = nBins;
		bounds = null;
		binWidth = -1;
	}
	
	public Histogram(int nBins, double min, double max) {
		bincount = nBins;
		bounds = new double[]{min, max};
		binWidth = -1;
	}
	
	public void add(double value) {
		add(value, 1.0);
	}
	
	public void add(double value, double weight) {
		values.add(value);
		weights.add(weight);
		modified = true;
	}
	
	public void addAll(double[] values) {
		for(double value : values)
			add(value, 1.0);
	}
	
	public void addAll(double[] values, double[] weights) {
		if(values.length != weights.length)
			throw new IllegalArgumentException(String.format(
					"The size of both arrays must be equal! (values=%1$s, weigths=%2$s)",
					values.length, weights.length));
		
		int size = values.length;
		for(int i = 0; i < size; i++)
			add(values[i], weights[i]);
	}
	
	public double[] getMinMax() {
		if (values.isEmpty()) {
			return new double[] { 0, 0 };
		} else {
			DoubleArrayList valuesCopy = new DoubleArrayList();
			valuesCopy.addAllOf(values);
			valuesCopy.sort();
			double[] minmax = new double[2];
			minmax[0] = valuesCopy.get(0);
			minmax[1] = valuesCopy.get(valuesCopy.size() - 1);
			return minmax;
		}
	}
	
	private void fillBins() {
		if(modified) {
			double min, max, width;
			int size;
			
			if(bounds != null) {
				min = bounds[0];
				max = bounds[1];					
			} else {
				double minmax[] = getMinMax();
				min = minmax[0];
				max = minmax[1];
			}
			if(binWidth > 0) {
				size = (int)Math.ceil((max - min)/(double)binWidth);
				width = binWidth;
			} else {
				size = bincount;
				width = (max - min)/(double)bincount;
			}
			
			bins = new DoubleArrayList();
			bins.setSize(size+1);
			for(int i = 0; i < values.size(); i++) {
				double value = values.get(i);
				if(value >= min && value <= max) {
					int idx = (int) Math.floor((values.get(i) - min)/width);
					bins.set(idx, bins.get(idx) + weights.get(i));
				}
			}
			
			modified = false;
		}
	}
	
	public double getMax() {
		return getMinMax()[1];
	}
	
	public double getMean() {
		double sum = 0;
		double wsum = 0;
		int size = values.size();
		for(int i = 0; i < size; i++) {
			sum += values.get(i) * weights.get(i);
			wsum += weights.get(i);
		}
		return sum/wsum;
	}
	
	public int getMaxBin() {
		fillBins();
		double maxVal = Double.MIN_VALUE;
		int maxBin = -1;
		for(int i = 0; i < bins.size(); i++) {
			if(bins.get(i) > maxVal) {
				maxVal = bins.get(i);
				maxBin = i;
			}
		}
		
		return maxBin;
	}
	
	public double getValue(int binIdx) {
		fillBins(); // Does this make sense?
		return bins.get(binIdx);
	}
	
	public double getBinLowerBound(int binIdx) {
		double min;
		if(bounds != null) {
			min = bounds[0];
		} else {
			min = getMinMax()[0];
		}
		return (binIdx * binWidth) + min;
	}
	public DoubleArrayList getValues() {
		return values;
	}
	
	public DoubleArrayList getWeights() {
		return weights;
	}
	
	public void plot(String filename, String title) throws IOException {
		fillBins();
		final XYSeriesCollection data = new XYSeriesCollection();
		final XYSeries wave = new XYSeries(title, false, true);
		
		double min, max, width;
//		int size;
		
		if(bounds != null) {
			min = bounds[0];
			max = bounds[1];					
		} else {
			double minmax[] = getMinMax();
			min = minmax[0];
			max = minmax[1];
		}
		if(binWidth > 0) {
//			size = (int)Math.ceil((max - min)/(double)binWidth);
			width = binWidth;
		} else {
//			size = bincount;
			width = (max - min)/(double)bincount;
		}
		
		int cnt = bins.size();
		for (int i = 0; i < cnt; i++) {
			wave.add(i * width + min, bins.get(i));
		}

		data.addSeries(wave);

		final JFreeChart chart = ChartFactory.createXYStepChart(
        "title", "x", "y", data,
        PlotOrientation.VERTICAL,
        true,   // legend
        false,   // tooltips
        false   // urls
		);

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("x");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("y"));
		ChartUtilities.saveChartAsPNG(new File(filename), chart, 1024, 768);
	}
	
	public void dumpRawData(String filename) throws IOException {
		fillBins();
		double min, max, width;
		
		if(bounds != null) {
			min = bounds[0];
			max = bounds[1];					
		} else {
			double minmax[] = getMinMax();
			min = minmax[0];
			max = minmax[1];
		}
		if(binWidth > 0) {
			width = binWidth;
		} else {
			width = (max - min)/(double)bincount;
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("bin\tvalue");
		writer.newLine();
		
		int cnt = bins.size();
		for (int i = 0; i < cnt; i++) {
			writer.write(String.format(Locale.US, "%1$s\t%2$s", i * width + min, bins.get(i)));
			writer.newLine();
		}
		writer.close();
	}
}
