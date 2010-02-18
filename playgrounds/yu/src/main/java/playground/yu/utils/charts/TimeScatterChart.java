/* *********************************************************************** *
 * project: org.matsim.*
 * TimeScatterChart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.utils.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeTableXYDataset;
import org.matsim.core.utils.charts.ChartUtil;

/**
 * Creates a new XY-ScatterChart with a X-Axis, that shows times.
 * 
 * @author yu
 * 
 */
public class TimeScatterChart extends ChartUtil {
	private final TimeTableXYDataset dataset;

	public TimeScatterChart(String title, String timeAxisLabel,
			String yAxisLabel) {
		super(title, timeAxisLabel, yAxisLabel);
		this.dataset = new TimeTableXYDataset();
		this.chart = createChart(title, timeAxisLabel, yAxisLabel, this.dataset);
		addDefaultFormatting();
	}

	private JFreeChart createChart(String title, String timeAxisLabel,
			String valueAxisLabel, final TimeTableXYDataset dataset) {
		ValueAxis timeAxis = new DateAxis(timeAxisLabel);
		timeAxis.setLowerMargin(0.02); // reduce the default margins
		timeAxis.setUpperMargin(0.02);
		NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
		valueAxis.setAutoRangeIncludesZero(false); // override default
		XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);

		XYToolTipGenerator toolTipGenerator = null;
		boolean tooltips = false;
		if (tooltips) {
			toolTipGenerator = StandardXYToolTipGenerator
					.getTimeSeriesInstance();
		}

		XYURLGenerator urlGenerator = null;
		boolean urls = false;
		if (urls) {
			urlGenerator = new StandardXYURLGenerator();
		}

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false,
				true);
		renderer.setBaseToolTipGenerator(toolTipGenerator);
		renderer.setURLGenerator(urlGenerator);
		plot.setRenderer(renderer);

		boolean legend = true;
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
				plot, legend);
		return chart;
		// return ChartFactory.createTimeSeriesChart(title, timeAxisLabel,
		// valueAxisLabel, dataset, true, // legend?
		// false, // tooltips?
		// false // URLs?
		// );
	}

	@Override
	public JFreeChart getChart() {
		return this.chart;
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
	public void addSeries(final String title, final double[] times,
			final double[] ys) {
		// final TimeTableXYDataset series = new TimeTableXYDataset();
		for (int i = 0, n = Math.min(times.length, ys.length); i < n; i++) {
			final Hour hour = new Hour((int) times[i] / 3600, new Day());
			final Minute min = new Minute(
					(int) ((times[i] - hour.getHour() * 3600) / 60), hour);
			final Second sec = new Second((int) (times[i] - hour.getHour()
					* 3600 - min.getMinute() * 60), min);
			this.dataset.add(sec, ys[i], title);
		}
		// this.dataset.addSeries(series);
	}
}
