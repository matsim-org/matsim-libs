/* *********************************************************************** *
 * project: org.matsim.*
 * LineChartUtil.java
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

/**
 * @author ychen
 *
 */
public class LineChartUtil extends ChartUtil {

	/**
	 * @param title
	 * @param categoryAxisLabel
	 * @param valueAxisLabel
	 */
	public LineChartUtil(String title, String categoryAxisLabel,
			String valueAxisLabel) {
		super(title, categoryAxisLabel, valueAxisLabel);
	}

	/* (non-Javadoc)
	 * @see playground.yu.graphUtils.ChartUtil#createChart(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	protected JFreeChart createChart(String title, String xAxisLabel,
			String yAxisLabel) {
		chart_ = ChartFactory.createLineChart(title, xAxisLabel, yAxisLabel,
				dataset0, PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
		return chart_;
	}

}
