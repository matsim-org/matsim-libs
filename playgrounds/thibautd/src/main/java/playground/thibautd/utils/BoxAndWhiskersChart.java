/* *********************************************************************** *
 * project: org.matsim.*
 * BoxAndWhiskersChart.java
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

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.collections.Tuple;

/**
 * Allows to plot easily box and whishers charts against an X axis
 * @author thibautd
 */
public class BoxAndWhiskersChart extends ChartUtil {

	//private final DefaultBoxAndWhiskerCategoryDataset boxes =
	//	new DefaultBoxAndWhiskerCategoryDataset();
	private final BoxAndWhiskerXYNumberDataset boxes =
		new BoxAndWhiskerXYNumberDataset();
	private final double binWidth;
	private final List<Tuple<Double,Double>> values = new ArrayList<Tuple<Double,Double>>();
	private double maxX = Double.NEGATIVE_INFINITY;
	private double minX = Double.POSITIVE_INFINITY;

	public BoxAndWhiskersChart(
			final String title,
			final String xAxisLabel,
			final String yAxisLabel,
			double binWidth) {
		super(title, xAxisLabel, yAxisLabel);
		this.binWidth = binWidth;
	}

	/**
	 * Creates the chart if it does not exits, and returns it. Once called,
	 * no modifications can be made to the dataset.
	 * @return the chart
	 */
	@Override
	public JFreeChart getChart() {
		if (this.chart == null) createChart();
		return this.chart;
	}

	/**
	 * adds a value to the dataset.
	 *
	 * During chart creation, y values of all point of x values belonging to a
	 * given bin will be used to compute the statistics
	 */
	public void add(final double x, final double y) {
		maxX = Math.max(maxX, x);
		minX = Math.min(minX, x);
		this.values.add(new Tuple<Double, Double>(x,y));
	}

	private void createChart() {
		boolean legend = false;

		this.createDataSet();

		this.chart =  ChartFactory.createBoxAndWhiskerChart(
				this.chartTitle, this.xAxisLabel, this.yAxisLabel,
				this.boxes, legend);
		this.formatChart();
	}

	private void formatChart() {
		this.chart.getXYPlot().setDomainAxis(new NumberAxis(this.xAxisLabel));
		this.chart.getXYPlot().getDomainAxis().configure();

		//this.addMatsimLogo();

		//try {
		//	this.addDefaultFormatting();
		//} catch (NullPointerException e) {
		//	// occurs if no legend
		//}

		//this.chart.setBackgroundPaint(Color.white);
		this.chart.getXYPlot().setBackgroundPaint(Color.white);
		XYBoxAndWhiskerRenderer renderer = (XYBoxAndWhiskerRenderer) this.chart.getXYPlot().getRenderer();
		//renderer.setFillBox(false);
		//renderer.setSeriesOutlinePaint(0, Color.black);
		//renderer.setSeriesPaint(0, Color.black);
		renderer.setBoxPaint(renderer.getSeriesPaint(0));
		//auto-adjust
		renderer.setBoxWidth(-1);
	}

	private void createDataSet() {
		Collections.sort(this.values, new TupleComparator());
		List<Double> currentBox = new ArrayList<Double>();;
		double currentUpperBound = minX + binWidth;

		for (Tuple<Double, Double> tuple : this.values) {
			if (tuple.getFirst().doubleValue() < currentUpperBound) {
				currentBox.add(tuple.getSecond());
			}
			else {
				//this.boxes.add(currentBox, "", currentUpperBound - (binWidth/2d));
				this.boxes.add(currentUpperBound - (binWidth/2d), currentBox);
				currentBox = new ArrayList<Double>();
				currentUpperBound += binWidth;
			}
		}
		//this.boxes.add(currentBox, "", currentUpperBound - (binWidth/2d));
		this.boxes.add(currentUpperBound - (binWidth/2d), currentBox);
	}

	private class TupleComparator implements Comparator<Tuple<Double, ? extends Object>> {

		@Override
		public int compare(final Tuple<Double, ? extends Object> arg0,
				final Tuple<Double, ? extends Object> arg1) {
			return arg0.getFirst().compareTo(arg1.getFirst());
		}
	}
}

