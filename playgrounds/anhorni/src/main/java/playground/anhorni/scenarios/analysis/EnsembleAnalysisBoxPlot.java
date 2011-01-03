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

package playground.anhorni.scenarios.analysis;

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
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

public class EnsembleAnalysisBoxPlot {
	
	private JFreeChart chart_;
	private int numberOfRandomRuns = -1;
	private TreeMap<Integer, Vector<Double>> valuesPerNumberOfSamples = new TreeMap<Integer, Vector<Double>>();
	private String runId ="";

	public EnsembleAnalysisBoxPlot(final TreeMap<Integer, Vector<Double>> valuesPerNumberOfSamples, final String chartTitle,
			int numberOfRandomRuns, int locIndex, String runId) {
		
		this.valuesPerNumberOfSamples = valuesPerNumberOfSamples;
		this.numberOfRandomRuns = numberOfRandomRuns;
		this.runId = runId;
	}

	public JFreeChart createChart() {
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
			
		ArrayList<Double>[] lists = new ArrayList[numberOfRandomRuns];
		
		// init
		for (int i = 0; i < numberOfRandomRuns; i++) {
			lists[i] = new ArrayList<Double>();
		}

		//int numberOfAnalyses = 0;
		int sampleIndex = 0;
		for (Vector<Double> el : this.valuesPerNumberOfSamples.values()) {	
			for (double val : el) {
				lists[sampleIndex].add(val);
			}
			//numberOfAnalyses = el.size();
			sampleIndex++;
		}

		// add the collected values to the graph / dataset
		for (int i = 0; i < numberOfRandomRuns; i++) {
			if ((i + 1) % 5 == 0) {
				dataset.add(lists[i], "Deviation From Ref Val", Integer.toString(i + 1));
			}
		}
		String title = this.runId;
		
		final CategoryAxis xAxis = new CategoryAxis("Number of random runs used for averaging");
		xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		
		final NumberAxis yAxis = new NumberAxis("Deviation from mean_n [%]");
		yAxis.setAutoRangeIncludesZero(true);

		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(false);
		renderer.setSeriesPaint(0, Color.blue);		
		CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);

		this.chart_ = new JFreeChart(title, new Font("SansSerif", Font.BOLD, 14), plot, false);
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