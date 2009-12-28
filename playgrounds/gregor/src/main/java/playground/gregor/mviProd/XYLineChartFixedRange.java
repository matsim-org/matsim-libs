/* *********************************************************************** *
 * project: org.matsim.*
 * XYLineChartFixedRange.java
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

package playground.gregor.mviProd;

import java.awt.Color;

import org.matsim.core.utils.charts.XYLineChart;

public class XYLineChartFixedRange extends XYLineChart {

	public XYLineChartFixedRange(final String title, final String axisLabel,
			final String axisLabel2, final double maxX, final double maxY) {
		super(title, axisLabel, axisLabel2);
		setRange(maxX,maxY);
	}
	


	protected void setRange(final double maxX, final double maxY) {

		this.chart.getXYPlot().getRangeAxis().setRange(0, maxY);
		this.chart.getXYPlot().getDomainAxis().setRange(0, maxX);
		Color paint = new Color(1.0f, 1.0f, 1.0f, 0.0f);
		this.chart.setBackgroundPaint(paint);
		this.chart.setBorderPaint(paint);
		this.chart.getXYPlot().setBackgroundPaint(paint);
		this.chart.getLegend().setBackgroundPaint(paint);
		this.chart.getPlot().setBackgroundPaint(paint);
		this.chart.getTitle().setBackgroundPaint(paint);
		this.chart.removeLegend();
	}

}
