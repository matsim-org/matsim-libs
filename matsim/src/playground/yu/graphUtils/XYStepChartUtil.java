/* *********************************************************************** *
 * project: org.matsim.*
 * XYStepChartUtil.java
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
package playground.yu.graphUtils;

import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author yu
 * 
 */
public class XYStepChartUtil extends ChartUtil{
	protected XYSeriesCollection dataSet_ = new XYSeriesCollection();

	public JFreeChart createChart(String title, String xAxisLabel,
			String yAxisLabel) {
		chart_ = ChartFactory.createXYStepChart(title, xAxisLabel, yAxisLabel,
				dataSet_, PlotOrientation.VERTICAL, true, // legend
				false, // tooltips
				false // urls
				);

		XYPlot plot = chart_.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart_;
	}

	public void addData(Object[] values) {
		XYSeries[] vs=(XYSeries[]) values;
		for (int i = 0; i < values.length; i++) {
			dataSet_.addSeries(vs[i]);
		}
	}
}
