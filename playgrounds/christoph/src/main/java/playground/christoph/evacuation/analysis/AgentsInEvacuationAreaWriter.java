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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

public class AgentsInEvacuationAreaWriter {
	
	protected int nofPictureBins;
	protected int binSize;
	protected int iteration;
	
	AgentsInEvacuationAreaWriter(int binSize, int nofPictureBins, int iteration) {
		this.binSize = binSize;
		this.nofPictureBins = nofPictureBins;
		this.iteration = iteration;
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(String absoluteFileName, String relativeFileName, Map<String, int[]> legs) {
		
		try {
			BufferedWriter absoluteWriter = IOUtils.getBufferedWriter(absoluteFileName);
			BufferedWriter relativeWriter = IOUtils.getBufferedWriter(relativeFileName);
			
			write(absoluteWriter, relativeWriter, legs);
			
			absoluteWriter.flush();
			relativeWriter.flush();
			absoluteWriter.close();
			relativeWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	private void write(BufferedWriter absoluteWriter, BufferedWriter relativeWriter, Map<String, int[]> legs) throws IOException {		
		Set<String> modes = legs.keySet();
		
		absoluteWriter.write("time\t");	// hh:mm:ss
		relativeWriter.write("time\t");	// hh:mm:ss
		absoluteWriter.write("time");	// ss
		relativeWriter.write("time");	// ss
		
		for (String legMode : modes) {
			absoluteWriter.write("\t" + legMode);
			relativeWriter.write("\t" + legMode);
		}

		// end of header
		absoluteWriter.write("\n");
		relativeWriter.write("\n");
		
		/*
		 * Get last entry in each array. We assume that the entries never decrease, 
		 * meaning the last entry has the highest value.
		 */
		int length = 0;
		Map<String, Integer> totalCounts = new HashMap<String, Integer>();
		for (String mode : legs.keySet()) {
			int[] data = legs.get(mode);
			totalCounts.put(mode, data[data.length - 1]);
			length = data.length;
		}
		
		for (int i = 0; i < length; i++) {
			
			absoluteWriter.write(Time.writeTime(i * this.binSize));
			relativeWriter.write(Time.writeTime(i * this.binSize));
			absoluteWriter.write("\t" + i * this.binSize);
			relativeWriter.write("\t" + i * this.binSize);
			
			// modes
			for (String mode : modes) {
				absoluteWriter.write("\t" + legs.get(mode)[i]);
				
				double relativeShare = Double.valueOf(legs.get(mode)[i]) / totalCounts.get(mode);
				relativeWriter.write("\t" + relativeShare);
			}

			// new line
			absoluteWriter.write("\n");
			relativeWriter.write("\n");
		}
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename, int[] activities, Map<String, int[]> legs) {
		
		try {
			BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(filename);
			write(bufferedWriter, activities, legs);
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	private void write(BufferedWriter bufferedWriter, int[] activities, Map<String, int[]> legs) throws IOException {		
		Set<String> modes = legs.keySet();
		
		bufferedWriter.write("time\t");	// hh:mm:ss
		bufferedWriter.write("time\t");	// ss
		bufferedWriter.write("activity");
		
		for (String legMode : modes) {
			bufferedWriter.write("\t" + legMode);
		}

		// end of header
		bufferedWriter.write("\n");
		
		for (int i = 0; i < activities.length; i++) {
			
			bufferedWriter.write(Time.writeTime(i * this.binSize));
			bufferedWriter.write("\t" + i*this.binSize);
			
			bufferedWriter.write("\t" + activities[i]);
			
			// modes
			for (String mode : modes) {
				bufferedWriter.write("\t" + legs.get(mode)[i]);
			}

			// new line
			bufferedWriter.write("\n");
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
	private JFreeChart getGraphic(String title, String legend, String modeName, int inputData[]) {
		
		/*
		 * Write only the number of defined picture bins to the plot.
		 */
		int data[];
		if (inputData.length > this.nofPictureBins) {
			data = Arrays.copyOfRange(inputData, 0, this.nofPictureBins);
		} else data = inputData;
		
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
	private JFreeChart getGraphic(String[] modeNames, int inputData[][]) {
		
		/*
		 * Write only the number of defined picture bins to the plot.
		 */
		int data[][];
		data = new int[inputData.length][];
		for (int i = 0; i < inputData.length; i++) {
			if (inputData[i].length > this.nofPictureBins) {
				data[i] = Arrays.copyOfRange(inputData[i], 0, this.nofPictureBins);
			} else data[i] = inputData[i];			
		}
		
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
