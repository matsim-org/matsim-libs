/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsReturnHomeWriter.java
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

package playground.christoph.evacuation.analysis;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.christoph.evacuation.analysis.AgentsReturnHomeCounter.ActivityInfo;

public class AgentsReturnHomeWriter {
	
	protected int nofPictureBins;
	protected int binSize;
	protected int iteration;
	
	AgentsReturnHomeWriter(int binSize, int nofPictureBins, int iteration) {
		this.binSize = binSize;
		this.nofPictureBins = nofPictureBins;
		this.iteration = iteration;
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(String fileName, Map<Id, ActivityInfo> activityInfo, Map<Id, String> legModes) {
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			
			write(writer, activityInfo, legModes);
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	private void write(BufferedWriter writer, Map<Id, ActivityInfo> activityInfo, Map<Id, String> legModes) throws IOException {		
		
		// write header
		writer.write("personId\t");
		writer.write("start time\t");
		writer.write("type\t");
		writer.write("location\t");
		writer.write("last leg mode");
		writer.write("\n");
		
		for (Entry<Id, ActivityInfo> entry : activityInfo.entrySet()) {
			writer.write(entry.getKey().toString() + "\t");
			writer.write(entry.getValue().startTime + "\t");
			writer.write(entry.getValue().type + "\t");
			writer.write(entry.getValue().facilityId.toString() + "\t");
			writer.write(legModes.get(entry.getKey()) + "\n");
		}
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename, Map<String, AtomicInteger[]> data) {
		
		try {
			BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(filename);
			write(bufferedWriter, data);
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
	private void write(BufferedWriter bufferedWriter, Map<String, AtomicInteger[]> data) throws IOException {
		Set<String> modes = data.keySet();
		
		bufferedWriter.write("time\t");	// hh:mm:ss
		bufferedWriter.write("time");	// ss
		
		Map<String, Integer> totals = new HashMap<String, Integer>();
		int entries = 0;
		for (String mode : modes) {
			bufferedWriter.write("\t" + mode + "_absolute");
			bufferedWriter.write("\t" + mode + "_relative");
			entries = data.get(mode).length;
			totals.put(mode, data.get(mode)[entries - 1].get());
		}

		// end of header
		bufferedWriter.write("\n");
		
		for (int i = 0; i < entries; i++) {
			
			bufferedWriter.write(Time.writeTime(i * this.binSize));
			bufferedWriter.write("\t" + i * this.binSize);
	
			// modes
			for (String mode : modes) {
				int value = data.get(mode)[i].get();
				bufferedWriter.write("\t" + value);
				bufferedWriter.write("\t" + ((value * 1.0) / (totals.get(mode) * 1.0)));
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
}