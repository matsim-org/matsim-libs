/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author michalm
 */
public class DensityScatterPlots {
	private static final Shape CIRCLE = new Ellipse2D.Double(-3, -3, 6, 6);

	public static JFreeChart createPlot(String title, String xAxisLabel, String yAxisLabel, XYSeries series) {
		return createPlot(title, xAxisLabel, yAxisLabel, series, null);
	}

	public static JFreeChart createPlot(String title, String xAxisLabel, String yAxisLabel, XYSeries series,
			Pair<Double, Double> lineCoeffs) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series);
		double maxValue = Math.max(series.getMaxX(), series.getMaxY());

		// y=x
		XYSeries lineXY = new XYSeries("y = x");
		lineXY.add(0, 0);
		lineXY.add(maxValue, maxValue);
		dataset.addSeries(lineXY);

		if (lineCoeffs != null) {
			// a*y+b=x
			double a = lineCoeffs.getLeft();
			double b = lineCoeffs.getRight();
			String namePrefix = a == 0 ? "" : (a + " * y + ");
			XYSeries lineABXY = new XYSeries(namePrefix + b + " = x");
			lineABXY.add(b, 0);
			if (a == 0) {
				lineABXY.add(b, maxValue);
			} else {
				lineABXY.add(maxValue, (maxValue - b) / a);
			}
			dataset.addSeries(lineABXY);
		}

		final JFreeChart chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset);
		XYPlot xyPlot = (XYPlot)chart.getPlot();

		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)xyPlot.getRenderer(0);
		renderer.setSeriesPaint(0, new Color(255, 0, 0, 50));
		renderer.setSeriesShape(0, CIRCLE);
		renderer.setSeriesLinesVisible(0, false);
		renderer.setSeriesShapesVisible(0, true);
		renderer.setSeriesVisibleInLegend(0, false);

		for (int i = 1; i < dataset.getSeriesCount(); i++) {
			renderer.setSeriesPaint(i, new Color(0, 0, 0));
			renderer.setSeriesLinesVisible(i, true);
			renderer.setSeriesShapesVisible(i, false);
			renderer.setSeriesVisibleInLegend(i, false);
		}

		xyPlot.getDomainAxis().setUpperBound(maxValue);
		xyPlot.getRangeAxis().setUpperBound(maxValue);

		return chart;
	}
}
