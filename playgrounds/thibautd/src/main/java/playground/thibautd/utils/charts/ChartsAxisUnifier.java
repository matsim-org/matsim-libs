/* *********************************************************************** *
 * project: org.matsim.*
 * ChartsAxisUnifier.java
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
package playground.thibautd.utils.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.matsim.core.utils.charts.ChartUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a list of charts, set all Y and/or X axis to the same range.
 * the Axis must be of type ValueAxis (if not, they are ignored).
 * the charts must contain plots of type XYPlot or CategoryPlot
 * @author thibautd
 */
public class ChartsAxisUnifier {
	final boolean unifyX;
	final boolean unifyY;

	double lowerX = Double.POSITIVE_INFINITY;
	double lowerY = Double.POSITIVE_INFINITY;
	double upperX = Double.NEGATIVE_INFINITY;
	double upperY = Double.NEGATIVE_INFINITY;

	private final List<JFreeChart> charts = new ArrayList<JFreeChart>();
	public ChartsAxisUnifier(
			final boolean unifyX,
			final boolean unifyY) {
		this.unifyX = unifyX;
		this.unifyY = unifyY;
	}

	public void addChart(final ChartUtil chart) {
		addChart( chart.getChart() );
	}

	public void addChart(final JFreeChart chart) {
		charts.add( chart );

		if (unifyX) {
			Plot plot = chart.getPlot();
			if (plot instanceof XYPlot) {
				ValueAxis axis = chart.getXYPlot().getDomainAxis();

				Range range = axis.getRange();
				lowerX = Math.min( lowerX , range.getLowerBound() );
				upperX = Math.max( upperX , range.getUpperBound() );
			}
		}

		if (unifyY) {
			Plot plot = chart.getPlot();
			ValueAxis axis;
			if (plot instanceof XYPlot) {
				axis = chart.getXYPlot().getRangeAxis();
			}
			else if (plot instanceof CategoryPlot) {
				axis = chart.getCategoryPlot().getRangeAxis();
			}
			else {
				return;
			}

			Range range = axis.getRange();
			lowerY = Math.min( lowerY , range.getLowerBound() );
			upperY = Math.max( upperY , range.getUpperBound() );
		}
	}

	public void applyUniformisation() {
		for (JFreeChart chart : charts) {
			Plot plot = chart.getPlot();
			ValueAxis xAxis = null;
			ValueAxis yAxis = null;

			if (plot instanceof XYPlot) {
				xAxis = unifyX ? chart.getXYPlot().getDomainAxis() : null;
				yAxis = unifyY ? chart.getXYPlot().getRangeAxis() : null;
			}
			else if (plot instanceof CategoryPlot) {
				yAxis = unifyY ? chart.getCategoryPlot().getRangeAxis() : null;
			}

			if (xAxis != null) xAxis.setRange( lowerX , upperX );
			if (yAxis != null) yAxis.setRange( lowerY , upperY );
		}
	}
}

