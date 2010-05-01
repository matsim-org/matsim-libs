/* *********************************************************************** *
 * project: org.matsim.*
 * BarChartTest.java
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

package playground.yu.utils.charts;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.title.ImageTitle;
import org.jfree.chart.title.Title;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.charts.BarChart;

public class DoubleBarChart {
	private final BarChart[] subCharts;
	private final CombinedDomainCategoryPlot plot;

	public DoubleBarChart(final String categoryAxisLabel,
			final String[] valueAxisLabels, final String[] categories,
			final int numberOfSubPlots) {
		this.plot = new CombinedDomainCategoryPlot(new CategoryAxis(
				categoryAxisLabel));
		this.subCharts = new BarChart[numberOfSubPlots];
		for (int i = 0; i < numberOfSubPlots; i++)
			subCharts[i] = new BarChart(null, null, valueAxisLabels[i],
					categories);
	}

	public void addSeries(int subPlotIndex, String serieTitle, double[] values) {
		subCharts[subPlotIndex].addSeries(serieTitle, values);
	}

	public void saveAsPng(final String filename, final String title,
			final int width, final int height) {
		for (int i = 0; i < subCharts.length; i++)
			plot.add((CategoryPlot) subCharts[i].getChart().getPlot(), 1);

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
				plot, true);
		// add MATSim logo
		Image image = MatsimResource
				.getAsImage("matsim_logo_transparent_small.png");
		Title subtitle = new ImageTitle(image, RectangleEdge.BOTTOM,
				HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM);
		chart.addSubtitle(subtitle);
		// add default Formatting
		chart.setBackgroundPaint(new Color(1.0f, 1.0f, 1.0f, 1.0f));
		chart.getLegend().setBorder(0.0, 0.0, 0.0, 0.0);

		try {
			ChartUtilities.saveChartAsPNG(new File(filename), chart, width,
					height, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("run1 starts!");
		run1(args);
		System.out.println("run1 ends!");
	}

	public static void run1(String[] args) {

		DoubleBarChart dbChart = new DoubleBarChart("categoryAxisLabel",
				new String[] { "valueAxisLabelUpper", "valueAxisLabelMiddle",
						"valueAxisLabelLower" }, new String[] { "category1",
						"category2", "category3", "category4", "category5" }, 3);
		// add data to upper plot
		dbChart
				.addSeries(0, "serieTitle0",
						new double[] { 0d, 0d, 0d, 0d, 0d });
		dbChart.addSeries(0, "serieTitle1", new double[] { 0.1, 0.1, 0.1, 0.1,
				0.1 });
		dbChart.addSeries(0, "serieTitle2", new double[] { 0.2, 0.2, 0.2, 0.2,
				0.2 });

		// add data to middle plot
		dbChart
				.addSeries(1, "serieTitle0",
						new double[] { 1d, 1d, 1d, 1d, 1d });
		dbChart.addSeries(1, "serieTitle1", new double[] { 1.1, 1.1, 1.1, 1.1,
				1.1 });
		dbChart.addSeries(1, "serieTitle2", new double[] { 1.2, 1.2, 1.2, 1.2,
				1.2 });

		// add data to lower plot
		dbChart
				.addSeries(2, "serieTitle0",
						new double[] { 2d, 2d, 2d, 2d, 2d });
		dbChart.addSeries(2, "serieTitle1", new double[] { 2.1, 2.1, 2.1, 2.1,
				2.1 });
		dbChart.addSeries(2, "serieTitle2", new double[] { 2.2, 2.2, 2.2, 2.2,
				2.2 });
		// save png picture
		dbChart.saveAsPng("../matsimTests/charts/doubleBarChart.png", "title",
				1024, 768);
	}

	/**
	 * @param args
	 */
	public static void run0(String[] args) {
		String chartFilename = "../matsimTests/charts/barChart.png";
		BarChart chartA = new BarChart(
				"",
				"",
				"Wegdistanz - MZ05",
				new String[] {
						"Arbeit (work)",
						"Rueckkehr nach Hause bzw. auswaertige Unterkunft (home)",
						"Freizeit (leisure)", "Einkauf (shopping)",
						"Ausbildung/Schule (education)", "Andere",
						"Geschaeftliche Taetigkeit und Dienstfahrt",
						"Service- und Begleitwege", "total" });
		chartA.addSeries("MIV (car)", new double[] { 9.04, 33.90, 12.20, 7.15,
				6.83, 6.28, 14.39, 4.48, 10.16 });
		chartA.addSeries("OeV (pt)", new double[] { 10.86, 36.80, 15.04, 5.67,
				10.06, 54.11, 33.04, 9.02, 12.19 });
		chartA.addSeries("LV (walk)", new double[] { 1.19, 0.24, 0.81, 0.70,
				2.17, 0.57, 1.93, 0.75, 1.04 });
		chartA.addSeries("Andere (others)", new double[] { 18.16, 22.23, 10.78,
				5.19, 0.84, 12.97, 44.90, 3.7, 11.76 });
		CategoryPlot subplotA = chartA.getChart().getCategoryPlot();

		BarChart chartB = new BarChart(
				"",
				"",
				"Wegdistanz - Matsim-run698",
				new String[] {
						"Arbeit (work)",
						"Rueckkehr nach Hause bzw. auswaertige Unterkunft (home)",
						"Freizeit (leisure)", "Einkauf (shopping)",
						"Ausbildung/Schule (education)", "Andere",
						"Geschaeftliche Taetigkeit und Dienstfahrt",
						"Service- und Begleitwege", "total" });
		chartB.addSeries("MIV (car)", new double[] { 10.71, 7.79, 6.28, 5.77,
				6.65, 0, 0, 0, 8.05 });
		chartB.addSeries("OeV (pt)", new double[] { 6.37, 6.10, 4.10, 4.10,
				4.64, 0, 0, 0, 5.44 });
		chartB.addSeries("LV (walk)", new double[] { 0.88, 0.85, 0.75, 0.75,
				0.81, 0, 0, 0, 0.81 });
		chartB.addSeries("Andere (others)", new double[] { 0, 0, 0, 0, 0, 0, 0,
				0, 0 });
		CategoryPlot subplotB = chartB.getChart().getCategoryPlot();

		final CombinedDomainCategoryPlot plot = new CombinedDomainCategoryPlot(
				new CategoryAxis("Verkehrszwecke/ travel destinations"));
		plot.add(subplotA, 1);
		plot.add(subplotB, 1);

		final JFreeChart result = new JFreeChart(
				"MZ05 vs MATSim - mittlere Wegdistanz",
				JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		try {
			ChartUtilities.saveChartAsPNG(new File(chartFilename), result,
					1024, 768, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// chartA.saveAsPng(chartFilename, 800, 600);
	}

}
