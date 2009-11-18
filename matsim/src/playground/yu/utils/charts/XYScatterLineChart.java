/* *********************************************************************** *
 * project: org.matsim.*
 * XYScatterChart.java
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

package playground.yu.utils.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.utils.charts.XYScatterChart;

/**
 * Creates a new XY-ScatterChart.
 * 
 * @author mrieser
 */
public class XYScatterLineChart extends XYScatterChart {

	public XYScatterLineChart(String title, String xAxisLabel, String yAxisLabel) {
		super(title, xAxisLabel, yAxisLabel);
		this.dataset = new XYSeriesCollection();
		this.chart = createChart(title, xAxisLabel, yAxisLabel, this.dataset);
		addDefaultFormatting();
	}

	private final XYSeriesCollection dataset;

	private JFreeChart createChart(final String title,
			final String categoryAxisLabel, final String valueAxisLabel,
			final XYSeriesCollection dataset) {
		// return ChartFactory.createScatterPlot(title, categoryAxisLabel,
		// valueAxisLabel, dataset, PlotOrientation.VERTICAL, true, // legend?
		// false, // tooltips?
		// false // URLs?
		// );
		NumberAxis xAxis = new NumberAxis(categoryAxisLabel);
		xAxis.setAutoRangeIncludesZero(false);
		xAxis.setRange(0, 24);
		NumberAxis yAxis = new NumberAxis(valueAxisLabel);
		yAxis.setAutoRangeIncludesZero(true);
		yAxis.setRange(0, 100);

		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);

		XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
		renderer.setBaseToolTipGenerator(null/* XYToolTipGenerator */);
		renderer.setURLGenerator(null/* urlGenerator */);
		plot.setRenderer(renderer);
		plot.setOrientation(PlotOrientation.VERTICAL);

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
				plot, true/* legend */);
		return chart;
	}

	/**
	 * Adds a new data series to the chart with the specified title.
	 * <code>xs<code> and <code>ys</code> should have the same length. If not,
	 * only as many items are shown as the shorter array contains.
	 * 
	 * @param title
	 * @param xs
	 *            The x values.
	 * @param ys
	 *            The y values.
	 */
	@Override
	public void addSeries(final String title, final double[] xs,
			final double[] ys) {
		XYSeries series = new XYSeries(title, false, true);
		for (int i = 0, n = Math.min(xs.length, ys.length); i < n; i++) {
			series.add(xs[i], ys[i]);
		}
		this.dataset.addSeries(series);
	}
}
