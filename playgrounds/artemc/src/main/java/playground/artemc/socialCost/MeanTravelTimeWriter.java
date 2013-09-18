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

package playground.artemc.socialCost;

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
 * Writes mean travel times, split up into modes, to graphs.
 * 
 * @author cdobler
 */
public class MeanTravelTimeWriter {
	
	protected int iteration;
	
	MeanTravelTimeWriter(int iteration) {
		this.iteration = iteration;
	}
	
	public void writeGraphic(final String filename, String legMode, double[] data) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(legMode, data), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a graphic showing the number of agents in the evacuated area
	 */
	private JFreeChart getGraphic(String modeName, double data[]) {
		
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries dataSerie = new XYSeries("mean trip travel time", false, true);
		
		for (int i = 0; i < data.length; i++) {
			dataSerie.add(i, data[i]);
		}

		xyData.addSeries(dataSerie);

//		final JFreeChart chart = ChartFactory.createXYStepChart(
		final JFreeChart chart = ChartFactory.createXYLineChart(
	        "mean travel time, " + modeName,
	        "iteration", "travel time",
	        xyData,
	        PlotOrientation.VERTICAL,
	        true,   // legend
	        false,   // tooltips
	        false   // urls
	    );

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}

	public void writeGraphic(final String filename, String[] legModes, double[][] data) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(legModes, data), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a graphic showing the number of agents in the evacuated area
	 */
	private JFreeChart getGraphic(String[] modeNames, double data[][]) {
		
		final XYSeriesCollection xyData = new XYSeriesCollection();

		for (int j = 0; j < modeNames.length; j++) {
			String modeName = modeNames[j];
			double[] d = data[j];

			XYSeries dataSerie = new XYSeries(modeName, false, true);
			
			for (int i = 0; i < d.length; i++) {
				dataSerie.add(i, d[i]);
			}
			
			xyData.addSeries(dataSerie);			
		}
		
//		final JFreeChart chart = ChartFactory.createXYStepChart(
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"mean trip travel time, all transport modes modes",
				"iteration", "travel time",
				xyData,
				PlotOrientation.VERTICAL,
				true,   // legend
				false,   // tooltips
				false   // urls
		);			

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}
}