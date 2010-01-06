/* *********************************************************************** *
 * project: org.matsim.*
 * CountsSimRealPerHourGraph.java
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

package playground.yu.counts.pt;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.CustomXYURLGenerator;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.helper.Comp;
import org.matsim.counts.algorithms.graphs.helper.MyComparator;

public class PtCountsSimRealPerHourGraph extends CountsGraph {
	/**
	 * The time at which the data was measured. A value in 1..24, 1 for 0 a.m.
	 * to 1 a.m., 2 for 1 a.m. to 2 a.m. ...
	 */
	private int hour;
	/**
	 * the String type of Counts, e.g. "board" or "alight"
	 */
	private String countsType;

	public PtCountsSimRealPerHourGraph(final List<CountSimComparison> ccl,
			final int iteration, final String filename, String countsType) {
		super(ccl, iteration, filename, filename);
		this.countsType = countsType;
	}

	/**
	 * @param hour
	 *            A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2
	 *            a.m. ...
	 */
	@Override
	public JFreeChart createChart(final int hour) {
		this.hour = hour;

		XYSeriesCollection dataset0 = new XYSeriesCollection();
		XYSeries series = new XYSeries("MATSim volumes");
		// easier to use another dataset
		XYSeriesCollection dataset_outliers = new XYSeriesCollection();
		XYSeries series_outliers = new XYSeries("MATSim outliers");

		CustomXYURLGenerator url_gen = new CustomXYURLGenerator();
		CustomXYToolTipGenerator tt_gen = new CustomXYToolTipGenerator();

		final ArrayList<String> urls = new ArrayList<String>();
		final ArrayList<String> tooltips = new ArrayList<String>();
		List<Comp> comps = new Vector<Comp>();

		Iterator<CountSimComparison> l_it = this.ccl_.iterator();
		// int elementCounter=0;
		while (l_it.hasNext()) {
			CountSimComparison cc = l_it.next();

			/*
			 * values with simVal==0.0 or countVal==0.0 are drawn on the x==1
			 * or/and y==1-line Such values are the result of a poor simulation
			 * run, but they can also represent a valid result (closing summer
			 * road during winter time)
			 */
			if (cc.getHour() == hour) {
				// elementCounter++;
				double realVal = 1.0;
				double simVal = 1.0;
				if (cc.getCountValue() > 0.0 && cc.getSimulationValue() > 0.0) {
					realVal = cc.getCountValue();
					simVal = cc.getSimulationValue();
					series.add(realVal, simVal);
					comps.add(new Comp(realVal, "link" + cc.getId() + ".html",
							"Link " + cc.getId() + "; " + "Count: " + realVal
									+ ", Sim: " + simVal));
				} else {
					realVal = Math.max(1.0, cc.getCountValue());
					simVal = Math.max(1.0, cc.getSimulationValue());
					series_outliers.add(realVal, simVal);
				}

			}// if
		}// while
		dataset0.addSeries(series);
		dataset_outliers.addSeries(series_outliers);

		/*
		 * first we have to sort the vector according to the rendering ordering
		 * (which is the x value). REALLY??? After hours of searching no better
		 * solution found! please help!
		 */

		Collections.sort(comps, new MyComparator());

		for (Iterator<Comp> iter = comps.iterator(); iter.hasNext();) {
			Comp cp = iter.next();
			urls.add(cp.getURL());
			tooltips.add(cp.getTooltip());
		}

		url_gen.addURLSeries(urls);
		tt_gen.addToolTipSeries(tooltips);

		String title = "[" + this.countsType + "ing]\tVolumes " + (hour - 1)
				+ ":00 - " + (hour) + ":00, Iteration: " + this.iteration_;
		this.setChartTitle(title);
		this.chart_ = ChartFactory.createXYLineChart(title,
				"Count Volumes [veh/h]", // x axis label
				"Sim Volumes [veh/h]", // y axis label
				dataset0, // data
				PlotOrientation.VERTICAL, false, // include legend
				true, // tooltips
				true // urls
				);
		XYPlot plot = this.chart_.getXYPlot();
		final LogarithmicAxis axis_x = new LogarithmicAxis(
				"Count Volumes [veh/h]");
		final LogarithmicAxis axis_y = new LogarithmicAxis(
				"Sim Volumes [veh/h]");
		axis_x.setAllowNegativesFlag(false);
		axis_y.setAllowNegativesFlag(false);

		// regular values
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setLinesVisible(false);
		renderer.setURLGenerator(url_gen);
		renderer.setSeriesPaint(0, Color.black);
		renderer.setSeriesToolTipGenerator(0, tt_gen);
		renderer
				.setSeriesShape(0, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));

		// outliers
		XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
		renderer2.setLinesVisible(false);
		renderer2.setSeriesPaint(0, Color.red);
		renderer2.setSeriesShape(0, new Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));

		// error band
		DefaultXYDataset dataset1 = new DefaultXYDataset();
		dataset1.addSeries("f1x", new double[][] { { 1.0, 10000.0 },
				{ 1.0, 10000.0 } });
		dataset1.addSeries("f2x", new double[][] { { 1.0, 10000.0 },
				{ 2.0, 20000.0 } });
		dataset1.addSeries("f05x", new double[][] { { 2.0, 10000.0 },
				{ 1.0, 5000.0 } });

		XYLineAndShapeRenderer renderer3 = new XYLineAndShapeRenderer();
		renderer3.setShapesVisible(false);
		renderer3.setSeriesPaint(0, Color.blue);
		renderer3.setSeriesPaint(1, Color.blue);
		renderer3.setSeriesPaint(2, Color.blue);
		renderer3.setBaseSeriesVisibleInLegend(false);
		renderer3.setSeriesItemLabelsVisible(0, true);
		renderer3.setSeriesItemLabelsVisible(1, false);
		renderer3.setSeriesItemLabelsVisible(2, false);

		XYTextAnnotation annotation0 = new XYTextAnnotation("2.0 count",
				12000.0, 15500.0);
		annotation0.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation0);
		XYTextAnnotation annotation1 = new XYTextAnnotation("count", 13000.0,
				10000.0);
		annotation1.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation1);
		XYTextAnnotation annotation2 = new XYTextAnnotation("0.5 count",
				11000.0, 3500.0);
		annotation2.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation2);

		plot.setDomainAxis(axis_x);
		plot.setRangeAxis(axis_y);
		plot.setRenderer(0, renderer);

		plot.setRenderer(1, renderer2);
		plot.setDataset(1, dataset_outliers);

		plot.setRenderer(2, renderer3);
		plot.setDataset(2, dataset1);

		plot.getRangeAxis().setRange(1.0, 19000.0);
		plot.getDomainAxis().setRange(1.0, 19000.0);

		return this.chart_;
	}// drawGraph

	/**
	 * The time at which the data was measured.
	 * 
	 * @return A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2 a.m.
	 *         ...
	 */
	public int getHour() {
		return this.hour;
	}
}
