/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaActivityWriter.java
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

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

public class AgentsInEvacuationAreaActivityWriter {
	
	protected int binSize;
	protected int iteration;
	
	AgentsInEvacuationAreaActivityWriter(int binSize, int iteration) {
		this.binSize = binSize;
		this.iteration = iteration;
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename, int[] activities, int[] activitiesParticipatingAtHome,
			int[] activitiesParticipatingNotAtHome, int[] activitiesNotParticipatingAtHome,
			int[] activitiesNotParticipatingNotAtHome) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream, activities, activitiesParticipatingAtHome, activitiesParticipatingNotAtHome,
				activitiesNotParticipatingAtHome, activitiesNotParticipatingNotAtHome);
		stream.close();
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	private void write(PrintStream stream, int[] activities, int[] activitiesParticipatingAtHome,
			int[] activitiesParticipatingNotAtHome, int[] activitiesNotParticipatingAtHome,
			int[] activitiesNotParticipatingNotAtHome) {		
		
		stream.print("time\t");	// hh:mm:ss
		stream.print("time\t");	// ss
		stream.print("all activities\t");
		stream.print("home activities of participating people\t");
		stream.print("home activities of not participating people\t");
		stream.print("other activities of participating people\t");
		stream.print("other activities of not participating people");

		// end of header
		stream.print("\n");
		
		for (int i = 0; i < activities.length; i++) {
			
			stream.print(Time.writeTime(i * this.binSize));
			stream.print("\t" + i*this.binSize);
			
			stream.print("\t" + activities[i]);
			stream.print("\t" + activitiesParticipatingAtHome[i]);
			stream.print("\t" + activitiesParticipatingNotAtHome[i]);
			stream.print("\t" + activitiesNotParticipatingAtHome[i]);
			stream.print("\t" + activitiesNotParticipatingNotAtHome[i]);

			// new line
			stream.print("\n");
		}
	}

	public void writeGraphic(final String filename, int[] activities, int[] activitiesParticipatingAtHome,
			int[] activitiesParticipatingNotAtHome, int[] activitiesNotParticipatingAtHome,
			int[] activitiesNotParticipatingNotAtHome) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(activities, activitiesParticipatingAtHome,
					activitiesParticipatingNotAtHome, activitiesNotParticipatingAtHome, activitiesNotParticipatingNotAtHome), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @return a graphic showing the number of agents in the evacuated area
	 */
	private JFreeChart getGraphic(int[] activities, int[] activitiesParticipatingAtHome,
			int[] activitiesParticipatingNotAtHome, int[] activitiesNotParticipatingAtHome,
			int[] activitiesNotParticipatingNotAtHome) {
		
		final XYSeriesCollection xyData = new XYSeriesCollection();
		XYSeries dataSerie;

		dataSerie = new XYSeries("total activity performing agents in evacuated area", false, true);
		for (int i = 0; i < activities.length; i++) {
			double hour = i * this.binSize / 60.0 / 60.0;
			dataSerie.add(hour, activities[i]);
		}
		xyData.addSeries(dataSerie);

		dataSerie = new XYSeries("participating agents performing a home activity in evacuated area", false, true);
		for (int i = 0; i < activitiesParticipatingAtHome.length; i++) {
			double hour = i * this.binSize / 60.0 / 60.0;
			dataSerie.add(hour, activitiesParticipatingAtHome[i]);
		}
		xyData.addSeries(dataSerie);
		
		dataSerie = new XYSeries("participating agents performing an other activity in evacuated area", false, true);
		for (int i = 0; i < activitiesParticipatingNotAtHome.length; i++) {
			double hour = i * this.binSize / 60.0 / 60.0;
			dataSerie.add(hour, activitiesParticipatingNotAtHome[i]);
		}
		xyData.addSeries(dataSerie);
		
		dataSerie = new XYSeries("not participating agents performing a home activity in evacuated area", false, true);
		for (int i = 0; i < activitiesNotParticipatingAtHome.length; i++) {
			double hour = i * this.binSize / 60.0 / 60.0;
			dataSerie.add(hour, activitiesNotParticipatingAtHome[i]);
		}
		xyData.addSeries(dataSerie);
		
		dataSerie = new XYSeries("not participating agents performing an other activity in evacuated area", false, true);
		for (int i = 0; i < activitiesNotParticipatingNotAtHome.length; i++) {
			double hour = i * this.binSize / 60.0 / 60.0;
			dataSerie.add(hour, activitiesNotParticipatingNotAtHome[i]);
		}
		xyData.addSeries(dataSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(
	        "activity performing agents in evacuated area, it." + this.iteration,
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