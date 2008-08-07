/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogram.java
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

package org.matsim.analysis;

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
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.utils.misc.Time;

/**
 * @author mrieser
 *
 * Counts the number of vehicles departed, arrived or got stuck per time bin
 * based on events.
 */
public class LegHistogram implements EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, EventHandlerAgentStuckI {

	private int iteration = 0;
	private final int binSize;
	private final int[] countsDep;
	private final int[] countsArr;
	private final int[] countsStuck;

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public LegHistogram(final int binSize, final int nofBins) {
		super();
		this.binSize = binSize;
		this.countsDep = new int[nofBins + 1]; // +1 for all times out of our range
		this.countsArr = new int[nofBins + 1];
		this.countsStuck = new int[nofBins + 1];
		reset(0);
	}

	/** Creates a new LegHistogram with the specified binSize and a default number of bins, such
	 * that 30 hours are analyzed.
	 *
	 * @param binSize The size of a time bin in seconds.
	 */
	public LegHistogram(final int binSize) {
		this(binSize, 30*3600/binSize + 1);
	}

	/* Implementation of eventhandler-Interfaces */

	public void handleEvent(final AgentDepartureEvent event) {
		this.countsDep[getBinIndex(event.time)]++;
	}

	public void handleEvent(final AgentArrivalEvent event) {
		this.countsArr[getBinIndex(event.time)]++;
	}

	public void handleEvent(final AgentStuckEvent event) {
		this.countsStuck[getBinIndex(event.time)]++;
	}

	/* output methods */

	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream);
		stream.close();
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	public void write(final PrintStream stream) {
		stream.println("time\ttime\tdepartures\tarrivals\tstuck\ton_route");
		int onRoute = 0;
		for (int i = 0; i < this.countsDep.length; i++) {
			onRoute = onRoute + this.countsDep[i] - this.countsArr[i] - this.countsStuck[i];
			stream.print(Time.writeTime(i*this.binSize) + "\t" + i*this.binSize);
			stream.println("\t" + this.countsDep[i] + "\t" + this.countsArr[i] + "\t" + this.countsStuck[i] + "\t" + onRoute);
		}
	}

	public JFreeChart getGraphic() {

		final XYSeriesCollection data = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("on route", false, true);
		int onRoute = 0;
		for (int i = 0; i < this.countsDep.length; i++) {
			onRoute = onRoute + this.countsDep[i] - this.countsArr[i] - this.countsStuck[i];
			double hour = i*this.binSize / 60.0 / 60.0;
			departuresSerie.add(hour, this.countsDep[i]);
			arrivalsSerie.add(hour, this.countsArr[i]);
			onRouteSerie.add(hour, onRoute);
		}

		data.addSeries(departuresSerie);
		data.addSeries(arrivalsSerie);
		data.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(
        "Leg Histogram, it." + this.iteration,
        "time", "# vehicles",
        data,
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

	public void writeGraphic(final String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.countsDep.length) {
			return this.countsDep.length - 1;
		}
		return bin;
	}

	public void reset(final int iteration) {
		this.iteration = iteration;
		for (int i = 0; i < this.countsDep.length; i++ ) {
			this.countsDep[i] = 0;
			this.countsArr[i] = 0;
			this.countsStuck[i] = 0;
		}
	}

}
