/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.pseudosimulation.trafficinfo;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.AbstractContinuousDistribution;
import org.apache.commons.math.distribution.BetaDistributionImpl;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.WeibullDistributionImpl;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import cern.jet.random.*;
import cern.jet.random.engine.MersenneTwister;

public class DistroTester {
	public static void main(String[] args) throws MathException {
		Date start;
		Date end;
		AbstractContinuousDistribution[] apaches = {
				new BetaDistributionImpl(2, 5),
				new ExponentialDistributionImpl(1),
				new GammaDistributionImpl(2, 2),
				new NormalDistributionImpl(1, 1),
				new WeibullDistributionImpl(1, 1.5) };
		MersenneTwister twister = new MersenneTwister(new java.util.Date());
		MersenneTwister rng = new MersenneTwister(10);
		AbstractContinousDistribution[] cerns = { new Beta(2, 5, twister),
				new Exponential(1, twister), new Gamma(2, 2, twister),
				new Normal(1, 1, twister) };
		for (AbstractContinuousDistribution dist : apaches) {
			double[] values = new double[1000000];
			start = new Date();
			for (int i = 0; i < 1000000; i++) {
				values[i] = dist.sample();
			}
			end = new Date();
			System.out.println(dist.getClass().getSimpleName() + ": "
					+ (end.getTime() - start.getTime()));
			int number = 100;
			HistogramDataset dataset = new HistogramDataset();
			dataset.setType(HistogramType.RELATIVE_FREQUENCY);
			dataset.addSeries("Histogram", values, number);
			String plotTitle = dist.getClass().getCanonicalName();
			String xaxis = "number";
			String yaxis = "value";
			PlotOrientation orientation = PlotOrientation.VERTICAL;
			boolean show = false;
			boolean toolTips = false;
			boolean urls = false;
			JFreeChart chart = ChartFactory.createHistogram(plotTitle, xaxis,
					yaxis, dataset, orientation, show, toolTips, urls);
			int width = 3200;
			int height = 1800;
			try {
				ChartUtilities.saveChartAsPNG(new File("f:/data/test/"
						+ dist.getClass().getName() + ".png"), chart, width,
						height);
			} catch (IOException e) {
			}
		}
		for (AbstractContinousDistribution dist : cerns) {
			double[] values = new double[1000000];
			start = new Date();
			for (int i = 0; i < 1000000; i++) {
				values[i] = dist.nextDouble();
			}
			end = new Date();
			System.out.println(dist.getClass().getSimpleName() + ": "
					+ (end.getTime() - start.getTime()));
			int number = 100;
			HistogramDataset dataset = new HistogramDataset();
			dataset.setType(HistogramType.RELATIVE_FREQUENCY);
			dataset.addSeries("Histogram", values, number);
			String plotTitle = dist.getClass().getCanonicalName();
			String xaxis = "number";
			String yaxis = "value";
			PlotOrientation orientation = PlotOrientation.VERTICAL;
			boolean show = false;
			boolean toolTips = false;
			boolean urls = false;
			JFreeChart chart = ChartFactory.createHistogram(plotTitle, xaxis,
					yaxis, dataset, orientation, show, toolTips, urls);
			int width = 3200;
			int height = 1800;
			try {
				ChartUtilities.saveChartAsPNG(new File("f:/data/test/"
						+ dist.getClass().getName() + ".png"), chart, width,
						height);
			} catch (IOException e) {
			}
		}
	}

}
