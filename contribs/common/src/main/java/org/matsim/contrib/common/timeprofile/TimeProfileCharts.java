/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.common.timeprofile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import com.google.common.collect.ImmutableMap;

public class TimeProfileCharts {
	public enum ChartType {
		Line, StackedArea
	}

	public static JFreeChart chartProfile(List<String> series, List<Double> times,
			List<ImmutableMap<String, Double>> timeProfile, ChartType type) {
		return chartProfile(createXYDataset(series, times, timeProfile), type);
	}

	public static JFreeChart chartProfile(DefaultTableXYDataset dataset, ChartType type) {
		JFreeChart chart;
		switch (type) {
			case Line:
				chart = ChartFactory.createXYLineChart("TimeProfile", "Time [h]", "Values", dataset,
						PlotOrientation.VERTICAL, true, false, false);
				break;

			case StackedArea:
				chart = ChartFactory.createStackedXYAreaChart("TimeProfile", "Time [h]", "Values", dataset,
						PlotOrientation.VERTICAL, true, false, false);
				break;

			default:
				throw new IllegalArgumentException();
		}

		XYPlot plot = chart.getXYPlot();
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setBackgroundPaint(Color.white);

		NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
		yAxis.setAutoRange(true);

		XYItemRenderer renderer = plot.getRenderer();
		for (int s = 0; s < dataset.getSeriesCount(); s++) {
			renderer.setSeriesStroke(s, new BasicStroke(2));
		}

		return chart;
	}

	public static DefaultTableXYDataset createXYDataset(List<String> series, List<Double> times,
			List<ImmutableMap<String, Double>> timeProfile) {
		XYSeries[] seriesArray = new XYSeries[series.size()];
		for (int s = 0; s < series.size(); s++) {
			seriesArray[s] = new XYSeries(series.get(s), false, false);
		}

		for (int t = 0; t < timeProfile.size(); t++) {
			ImmutableMap<String, Double> timePoint = timeProfile.get(t);
			double hour = times.get(t) / 3600;
			for (int s = 0; s < series.size(); s++) {
				seriesArray[s].add(hour, timePoint.getOrDefault(series.get(s), 0.));
			}
		}

		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		for (int s = 0; s < series.size(); s++) {
			dataset.addSeries(seriesArray[s]);
		}
		return dataset;
	}

	public static void changeSeriesColors(JFreeChart chart, Paint... paints) {
		XYItemRenderer renderer = chart.getXYPlot().getRenderer();
		for (int i = 0; i < paints.length; i++) {
			renderer.setSeriesPaint(i, paints[i]);
		}
	}
}
