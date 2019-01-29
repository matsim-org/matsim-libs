/* *********************************************************************** *
 * project: org.matsim.*
 * BoxPlotErrorGraph.java
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

package org.matsim.counts.algorithms.graphs;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.matsim.counts.CountSimComparison;

public final class BoxPlotErrorGraph extends CountsGraph {

	public BoxPlotErrorGraph(final List<CountSimComparison> ccl, final int iteration, final String filename,
			final String chartTitle) {
		super(ccl, iteration, filename, chartTitle);
	}

	@SuppressWarnings("unchecked")
	@Override
	public JFreeChart createChart(final int nbr) {

		DefaultBoxAndWhiskerCategoryDataset dataset0 = new DefaultBoxAndWhiskerCategoryDataset();
		DefaultBoxAndWhiskerCategoryDataset dataset1 = new DefaultBoxAndWhiskerCategoryDataset();

		final ArrayList<Double>[] listRel = new ArrayList[24];
		final ArrayList<Double>[] listAbs = new ArrayList[24];

		// init
		for (int i = 0; i < 24; i++) {
			listRel[i] = new ArrayList<Double>();
			listAbs[i] = new ArrayList<Double>();
		}

		// add the values of all counting stations to each hour
		for (CountSimComparison cc : this.ccl_) {
			int hour = cc.getHour() - 1;
			listRel[hour].add(cc.calculateRelativeError() * 100);
			listAbs[hour].add(cc.getSimulationValue() - cc.getCountValue());
		}

		// add the collected values to the graph / dataset
		for (int i = 0; i < 24; i++) {
			dataset0.add(listRel[i], "Rel Error", Integer.toString(i + 1));
			dataset1.add(listAbs[i], "Abs Error", Integer.toString(i + 1));
		}

		String title = "Iteration: " + this.iteration_;

		final CombinedDomainCategoryPlot plot = new CombinedDomainCategoryPlot();

		final CategoryAxis xAxis = new CategoryAxis("Hour");
		final NumberAxis yAxis0 = new NumberAxis("Signed Rel. Error [%]");
		final NumberAxis yAxis1 = new NumberAxis("Signed Abs. Error [veh]");
		yAxis0.setAutoRangeIncludesZero(false);
		yAxis1.setAutoRangeIncludesZero(false);

		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(false);
		renderer.setSeriesPaint(0, Color.blue);
		renderer.setSeriesToolTipGenerator(0, new BoxAndWhiskerToolTipGenerator());

		CategoryPlot subplot0 = new CategoryPlot(dataset0, xAxis, yAxis0, renderer);
		CategoryPlot subplot1 = new CategoryPlot(dataset1, xAxis, yAxis1, renderer);

		plot.add(subplot0);
		plot.add(subplot1);

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		axis1.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		plot.setDomainAxis(axis1);

		this.chart_ = new JFreeChart(title, new Font("SansSerif", Font.BOLD, 14), plot, false);
		return this.chart_;
	}
}