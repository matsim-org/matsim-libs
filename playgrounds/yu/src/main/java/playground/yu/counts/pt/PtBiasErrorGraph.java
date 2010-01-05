/* *********************************************************************** *
 * project: org.matsim.*
 * PtBiasErrorGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.counts.pt;

import java.awt.Font;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.CountsGraph;

/**
 * @author yu
 * 
 */
public class PtBiasErrorGraph extends CountsGraph {
	private ComparisonErrorStatsCalculator errorStats;
	private String chartTitle;

	/**
	 * @param ccl
	 * @param iteration
	 * @param filename
	 * @param chartTitle
	 */
	public PtBiasErrorGraph(List<CountSimComparison> ccl, int iteration,
			String filename, String chartTitle) {
		super(ccl, iteration, filename, chartTitle);
		this.chartTitle = chartTitle;
	}

	public JFreeChart createChart(final int nbr) {
		DefaultCategoryDataset dataset0 = new DefaultCategoryDataset();
		DefaultCategoryDataset dataset1 = new DefaultCategoryDataset();

		this.errorStats = new ComparisonErrorStatsCalculator(this.ccl_);

		double[] meanRelError = errorStats.getMeanRelError();
		// double[] meanAbsError = errorStats.getMeanAbsError();
		double[] meanAbsBias = errorStats.getMeanAbsBias();

		for (int h = 0; h < 24; h++) {
			dataset0.addValue(meanRelError[h], "Mean rel error", Integer
					.toString(h + 1));
			// dataset1.addValue(meanAbsError[h], "Mean abs error",
			// Integer.toString(h + 1));
			dataset1.addValue(meanAbsBias[h], "Mean abs bias", Integer
					.toString(h + 1));
		}

		this.chart_ = ChartFactory.createLineChart(this.chartTitle, "Hour",
				"Mean rel error [%]", dataset0, PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
		CategoryPlot plot = this.chart_.getCategoryPlot();
		plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
		plot.setDataset(1, dataset1);
		plot.mapDatasetToRangeAxis(1, 1);

		final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
		renderer.setSeriesToolTipGenerator(0,
				new StandardCategoryToolTipGenerator());
		plot.setRenderer(0, renderer);

		final CategoryAxis axis1 = new CategoryAxis("Hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(axis1);

		// final ValueAxis axis2 = new
		// NumberAxis("Mean abs {bias, error} [veh/h]");
		final ValueAxis axis2 = new NumberAxis("Mean abs bias [veh/h]");
		plot.setRangeAxis(1, axis2);

		final ValueAxis axis3 = plot.getRangeAxis(0);
		axis3.setRange(0.0, 100.0);

		final LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
		renderer2.setSeriesToolTipGenerator(0,
				new StandardCategoryToolTipGenerator());
		renderer2.setSeriesToolTipGenerator(1,
				new StandardCategoryToolTipGenerator());
		// renderer2.setSeriesPaint(0, Color.black);
		plot.setRenderer(1, renderer2);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);

		return this.chart_;
	}

	public double[] getMeanRelError() {
		if (this.errorStats == null) {
			throw new RuntimeException(
					"Object not initialized correctly. Call createChart(..) first!");
		}
		return this.errorStats.getMeanRelError();
	}

	public double[] getMeanAbsBias() {
		if (this.errorStats == null) {
			throw new RuntimeException(
					"Object not initialized correctly. Call createChart(..) first!");
		}
		return this.errorStats.getMeanAbsBias();
	}
}
