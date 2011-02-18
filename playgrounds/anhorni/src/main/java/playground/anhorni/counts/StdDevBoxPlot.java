/* *********************************************************************** *
 * project: org.matsim.*
 * BoxPlotErrorGraph.java
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

package playground.anhorni.counts;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Vector;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
//import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

public class StdDevBoxPlot {
	
	private JFreeChart chart_;
	private TreeMap<Integer, Vector<Double>> values = new TreeMap<Integer, Vector<Double>>();
	private String chartTitle = "";
	private String xlabel = "";
	private String ylabel = "";

	public StdDevBoxPlot(final String chartTitle, String xlabel, String ylabel) {
		this.chartTitle = chartTitle;
		this.xlabel = xlabel;
		this.ylabel = ylabel;
		
		for (int hour = 0; hour < 24; hour++) {
			values.put(hour, new Vector<Double>());
		}
	}
	
	public void addHourlyData(Integer hour, Vector<Double> vals) {
		this.values.put(hour, vals);
	}
	
	public void addHourlyData(Integer hour, double val) {
		this.values.get(hour).add(val);
	}
	
	
	private ArrayList<Double>[] createArrayLists() {
		ArrayList<Double>[] lists = new ArrayList[24];
		// init
		for (int i = 0; i < 24; i++) {
			lists[i] = new ArrayList<Double>();
		}
		int index = 0;
		for (Vector<Double> el : this.values.values()) {	
			for (double val : el) {
				lists[index].add(val);
			}
			index++;
		}
		return lists;
	}

	public JFreeChart createChart() {
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
			
		ArrayList<Double>[] lists = this.createArrayLists();

		// add the collected values to the graph / dataset
		for (int i = 0; i < 24; i++) {
			dataset.add(lists[i], "hour", Integer.toString(i + 1));
		}		
		final CategoryAxis xAxis = new CategoryAxis(xlabel);
		xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
		//xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		
		final NumberAxis yAxis = new NumberAxis(ylabel);
		yAxis.setAutoRangeIncludesZero(true);

		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(false);
		renderer.setSeriesPaint(0, Color.blue);		
		CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

		this.chart_ = new JFreeChart(chartTitle, new Font("SansSerif", Font.BOLD, 14), plot, false);
		return this.chart_;
	}
	
	public void saveAsPng(final String filename, final int width, final int height) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), createChart(), width, height, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}