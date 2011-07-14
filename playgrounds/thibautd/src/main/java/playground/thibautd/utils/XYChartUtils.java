/* *********************************************************************** *
 * project: org.matsim.*
 * XYChartUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.JFreeChart;

/**
 * provides static functions to perform usual XYPlot manipulation.
 * @author thibautd
 */
public class XYChartUtils {

	private XYChartUtils() {}

	/**
	 * Sets the X axis of an XYPlot chart to a {@link NumberAxis}
	 * with integer values.
	 */
	public static void integerXAxis(final JFreeChart chart) {
		ValueAxis axis = (chart.getXYPlot()).getDomainAxis();
		NumberAxis numberAxis;
		if (axis instanceof NumberAxis) {
			numberAxis = (NumberAxis) axis;
		}
		else {
			numberAxis = new NumberAxis();
			(chart.getXYPlot()).setDomainAxis(numberAxis);
		}

		numberAxis.setTickUnit((NumberTickUnit) NumberAxis.createIntegerTickUnits().getCeilingTickUnit(1d));
		numberAxis.setAutoRangeIncludesZero(false);
	}

	/**
	 * Sets the X axis of an XYPlot chart to a {@link NumberAxis}
	 * with integer values.
	 */
	public static void integerYAxis(final JFreeChart chart) {
		ValueAxis axis = (chart.getXYPlot()).getRangeAxis();
		NumberAxis numberAxis;
		if (axis instanceof NumberAxis) {
			numberAxis = (NumberAxis) axis;
		}
		else {
			numberAxis = new NumberAxis();
			(chart.getXYPlot()).setRangeAxis(numberAxis);
		}

		numberAxis.setTickUnit((NumberTickUnit) NumberAxis.createIntegerTickUnits().getCeilingTickUnit(1d));
		numberAxis.setAutoRangeIncludesZero(false);
	}

	/**
	 * Sets the X and the Y axes of an XYPlot to use integer values.
	 */
	public static void integerAxes(final JFreeChart chart) {
		integerXAxis(chart);
		integerYAxis(chart);
	}
}

