/* *********************************************************************** *
 * project: org.matsim.*
 * BubbleChart.java
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import org.matsim.core.utils.charts.ChartUtil;

/**
 * @author yu
 * 
 */
public class BubbleChart extends ChartUtil {
	private final DefaultXYZDataset dataset;

	/**
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 */
	public BubbleChart(String title, String xAxisLabel, String yAxisLabel) {
		super(title, xAxisLabel, yAxisLabel);
		dataset = new DefaultXYZDataset();
		chart = createChart(title, xAxisLabel, yAxisLabel, dataset);
		addDefaultFormatting();
	}

	@Override
	public JFreeChart getChart() {
		return chart;
	}

	private JFreeChart createChart(String title, String xAxisLabel,
			String yAxisLabel, XYZDataset dataset) {
		return ChartFactory.createBubbleChart(title, xAxisLabel, yAxisLabel,
				dataset, PlotOrientation.VERTICAL, true// legend?
				, false// tooltips?
				, false// URLs?
				);
	}

	/**
	 * @param title
	 * @param data
	 *            the data (must be an array with length 3, containing three
	 *            arrays of equal length, the first containing the x-values, the
	 *            second containing the y-values and the third containing the
	 *            z-values).
	 */
	public void addSeries(final String title, final double[][] data) {
		dataset.addSeries(title, data);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BubbleChart chart = new BubbleChart("TITLE", "x-axis", "y-axis");
		chart.addSeries("serie 1", new double[][] { { 1, 3, 5 }, { 2, 4, 6 },
				{ 0.1, 0.2, 0.3 } });
		chart.saveAsPng("output/bubbleTest.png", 800, 600);
	}
}
