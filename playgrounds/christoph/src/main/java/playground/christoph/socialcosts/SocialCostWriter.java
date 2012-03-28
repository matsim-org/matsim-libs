/* *********************************************************************** *
 * project: org.matsim.*
 * MeanTravelTimeWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.socialcosts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Writes social costs to a graph.
 * 
 * @author cdobler
 */
public class SocialCostWriter {
	
	protected int iteration;
	
	SocialCostWriter(int iteration) {
		this.iteration = iteration;
	}
	
	public void writeGraphic(final String filename, double[] meanData, double[] medianData, double[] quantil25Data, double[] quantil75Data) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(meanData, medianData, quantil25Data, quantil75Data), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a graphic showing the number of agents in the evacuated area
	 */
	private JFreeChart getGraphic(double[] meanData, double[] medianData, double[] quantil25Data, double[] quantil75Data) {
		
		final XYSeriesCollection xyData = new XYSeriesCollection();
		
		final XYSeries meanSerie = new XYSeries("mean", false, true);
		final XYSeries medianSerie = new XYSeries("median", false, true);
		final XYSeries quantil25SigmaSerie = new XYSeries("25% quantil", false, true);
		final XYSeries quantil75SigmaSerie = new XYSeries("75% qunatil", false, true);
		
		for (int i = 0; i < meanData.length; i++) {
			meanSerie.add(i, meanData[i]);
			medianSerie.add(i, medianData[i]);
			quantil25SigmaSerie.add(i, quantil25Data[i]);
			quantil75SigmaSerie.add(i, quantil75Data[i]);
		}
		
		xyData.addSeries(meanSerie);
		xyData.addSeries(medianSerie);
		xyData.addSeries(quantil25SigmaSerie);
		xyData.addSeries(quantil75SigmaSerie);

		final JFreeChart chart = ChartFactory.createXYLineChart(
	        "social costs (per leg)",
	        "iteration", "social costs (per leg)",
	        xyData,
	        PlotOrientation.VERTICAL,
	        true,   // legend
	        false,   // tooltips
	        false   // urls
	    );

		XYPlot plot = chart.getXYPlot();
		plot.getRenderer().setSeriesPaint(0, Color.red);
		plot.getRenderer().setSeriesPaint(1, Color.blue);
		plot.getRenderer().setSeriesPaint(2, Color.blue);
		plot.getRenderer().setSeriesPaint(3, Color.blue);
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
				BasicStroke.JOIN_ROUND, 1.0f, new float[] {8.0f, 4.0f}, 0.0f));
		plot.getRenderer().setSeriesStroke(3, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
				BasicStroke.JOIN_ROUND, 1.0f, new float[] {2.0f, 4.0f}, 0.0f));
		
		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}
	
}
