/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaWriter.java
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

package playground.christoph.evacuation.analysis;

import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.utils.misc.Time;

public class AgentsInEvacuationAreaWriter {
	
	protected int binSize;
	protected int iteration;
	
	AgentsInEvacuationAreaWriter(int binSize, int iteration) {
		this.binSize = binSize;
		this.iteration = iteration;
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename, int[] activities, Map<String, int[]> legs) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream, activities, legs);
		stream.close();
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	private void write(PrintStream stream, int[] activities, Map<String, int[]> legs) {		
		Set<String> modes = legs.keySet();
		
		stream.print("time\t");	// hh:mm:ss
		stream.print("time\t");	// ss
		stream.print("activity");
		
		for (String legMode : modes) {
			stream.print("\t" + legMode);
		}

		// end of header
		stream.print("\n");
		
		for (int i = 0; i < activities.length; i++) {
			
			stream.print(Time.writeTime(i * this.binSize));
			stream.print("\t" + i*this.binSize);
			
			stream.print("\t" + activities[i]);
			
			// modes
			for (String mode : modes) {
				stream.print("\t" + legs.get(mode)[i]);
			}

			// new line
			stream.print("\n");
		}
	}

	public void writeGraphic(final String filename, String title, String legend, String legMode, int[] data) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(title, legend, legMode, data), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a graphic showing the number of agents in the evacuated area
	 */
	private JFreeChart getGraphic(String title, String legend, String modeName, int data[]) {
		
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries dataSerie = new XYSeries(legend, false, true);

		for (int i = 0; i < data.length; i++) {
			double hour = i * this.binSize / 60.0 / 60.0;
			dataSerie.add(hour, data[i]);
		}

		xyData.addSeries(dataSerie);
		final JFreeChart chart = ChartFactory.createXYStepChart(
	        title + ", " + modeName + ", it." + this.iteration,
	        "time", "# agents",
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

	public void writeGraphic(final String filename, String[] legModes, int[][] data) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(legModes, data), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a graphic showing the number of agents in the evacuated area
	 */
	private JFreeChart getGraphic(String[] modeNames, int data[][]) {
		
		final XYSeriesCollection xyData = new XYSeriesCollection();

		for (int j = 0; j < modeNames.length; j++) {
			String modeName = modeNames[j];
			int[] d = data[j];

			XYSeries dataSerie = new XYSeries(modeName, false, true);
			
			for (int i = 0; i < d.length; i++) {
				double hour = i * this.binSize / 60.0 / 60.0;
				dataSerie.add(hour, d[i]);
			}
			
			xyData.addSeries(dataSerie);			
		}
		
		final JFreeChart chart = ChartFactory.createXYStepChart(
				"agents in evacuated area, all modes, it." + this.iteration,
				"time", "# agents",
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
