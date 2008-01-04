/* *********************************************************************** *
 * project: org.matsim.*
 * BarChart.java
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

package org.matsim.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Creates a new XYLineChart.
 *
 * @author mrieser
 */
public class XYLineChart extends ChartUtil {

	private final XYSeriesCollection dataset;

	public XYLineChart(final String title, final String xAxisLabel, final String yAxisLabel) {
		super(title, xAxisLabel, yAxisLabel);
		this.dataset = new XYSeriesCollection();
		this.chart = createChart(title, xAxisLabel, yAxisLabel, this.dataset);
		addDefaultFormatting();
	}

	@Override
	protected JFreeChart getChart() {
		return this.chart;
	}

	private JFreeChart createChart(final String title, final String categoryAxisLabel,
			final String valueAxisLabel, final XYSeriesCollection dataset) {
		return ChartFactory.createXYLineChart(title, categoryAxisLabel, valueAxisLabel,
				dataset, PlotOrientation.VERTICAL, true, // legend?
				false, // tooltips?
				false // URLs?
				);
	}

	/**
	 * Adds a new data series to the chart with the specified title.
	 * <code>xs<code> and <code>ys</code> should have the same length. If not, only as many items
	 * are shown as the shorter array contains.
	 *
	 * @param title
	 * @param xs The x values.
	 * @param ys The y values.
	 */
	public void addSeries(final String title, final double[] xs, final double[] ys) {
		XYSeries series = new XYSeries(title, false, true);
		for (int i = 0, n = Math.min(xs.length, ys.length); i < n; i++) {
			series.add(xs[i], ys[i]);
		}
		this.dataset.addSeries(series);
	}

}
