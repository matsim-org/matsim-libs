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

package org.matsim.core.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Creates two-dimensional line charts with support for multiple series in one chart.
 *
 * @author mrieser
 * @author ychen
 */
public class LineChart extends ChartUtil {

	private final String[] categories;
	private final DefaultCategoryDataset dataset;

	/**
	 * Creates a new BarChart with default category-labels (numbered from 1 to the number of categories).
	 * The same as {@link #LineChart(String, String, String, String[]) BarChart(title, xAxisLabel, yAxisLabel, null)}.
	 *
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 */
	public LineChart(final String title, final String xAxisLabel, final String yAxisLabel) {
		this(title, xAxisLabel, yAxisLabel, new String[]{});
	}

	/**
	 * Creates a new BarChart with the specified category-labels.
	 *
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 * @param categories
	 */
	public LineChart(final String title, final String xAxisLabel, final String yAxisLabel, final String[] categories) {
		super(title, xAxisLabel, yAxisLabel);
		this.dataset = new DefaultCategoryDataset();
		this.chart = createChart(title, xAxisLabel, yAxisLabel, this.dataset);
		this.categories = categories.clone();
		addDefaultFormatting();
	}

	@Override
	public JFreeChart getChart() {
		return this.chart;
	}

	private JFreeChart createChart(final String title, final String categoryAxisLabel,
			final String valueAxisLabel, final CategoryDataset dataset) {
		return ChartFactory.createLineChart(title, categoryAxisLabel, valueAxisLabel,
				dataset, PlotOrientation.VERTICAL, true, // legend?
				false, // tooltips?
				false // URLs?
				);
	}

	/**
	 * Adds a new data series to the chart with the specified title.
	 *
	 * @param title
	 * @param values
	 */
	public void addSeries(final String title, final double[] values) {
		int cnt = 1;
		for (double value : values) {
			String category = (cnt > this.categories.length ? Integer.toString(cnt) : this.categories[cnt-1]);
			this.dataset.addValue(value, title, category);
			cnt++;
		}
	}

}
