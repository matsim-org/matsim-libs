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
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.utils.misc.Time;

/**
 * @author mrieser
 *
 * Counts the number of vehicles departed, arrived or got stuck per time bin
 * based on events.
 */
public class LegHistogram implements AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {

	private int iteration = 0;
	private final int binSize;
	private final int nofBins;
	private final Map<String, ModeData> data = new HashMap<String, ModeData>(5, 0.85f);
	private ModeData allModesData = null;

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 *
	 * @param binSize The size of a time bin in seconds.
	 * @param nofBins The number of time bins for this analysis.
	 */
	public LegHistogram(final int binSize, final int nofBins) {
		super();
		this.binSize = binSize;
		this.nofBins = nofBins;
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

	/* Implementation of EventHandler-Interfaces */

	public void handleEvent(final AgentDepartureEvent event) {
		int index = getBinIndex(event.time);
		allModesData.countsDep[index]++;
		ModeData modeData = getDataForMode(event.leg.getMode());
		modeData.countsDep[index]++;
	}

	public void handleEvent(final AgentArrivalEvent event) {
		int index = getBinIndex(event.time);
		allModesData.countsArr[index]++;
		ModeData modeData = getDataForMode(event.leg.getMode());
		modeData.countsArr[index]++;
	}

	public void handleEvent(final AgentStuckEvent event) {
		int index = getBinIndex(event.time);
		allModesData.countsStuck[index]++;
		ModeData modeData = getDataForMode(event.leg.getMode());
		modeData.countsStuck[index]++;
	}

	public void reset(final int iter) {
		this.iteration = iter;
		this.allModesData = new ModeData(nofBins + 1);
		this.data.clear();
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
		stream.print("time\ttime\tdepartures_all\tarrivals_all\tstuck_all\ten-route_all");
		for (String legMode : this.data.keySet()) {
			stream.print("\tdepartures_" + legMode + "\tarrivals_" + legMode + "\tstuck_" + legMode + "\ten-route_" + legMode);
		}
		stream.print("\n");
		int allEnRoute = 0;
		int[] modeEnRoute = new int[this.data.size()];
		for (int i = 0; i < this.allModesData.countsDep.length; i++) {
			// data about all modes
			allEnRoute = allEnRoute + this.allModesData.countsDep[i] - this.allModesData.countsArr[i] - this.allModesData.countsStuck[i];
			stream.print(Time.writeTime(i*this.binSize) + "\t" + i*this.binSize);
			stream.print("\t" + this.allModesData.countsDep[i] + "\t" + this.allModesData.countsArr[i] + "\t" + this.allModesData.countsStuck[i] + "\t" + allEnRoute);

			// data about single modes
			int mode = 0;
			for (ModeData modeData : this.data.values()) {
				modeEnRoute[mode] = modeEnRoute[mode] + modeData.countsDep[i] - modeData.countsArr[i] - modeData.countsStuck[i];
				stream.print("\t" + modeData.countsDep[i] + "\t" + modeData.countsArr[i] + "\t" + modeData.countsStuck[i] + "\t" + modeEnRoute[mode]);
				mode++;
			}

			// new line
			stream.print("\n");
		}
	}

	public JFreeChart getGraphic() {
		return getGraphic(this.allModesData, "all");
	}

	public JFreeChart getGraphic(final String legMode) {
		return getGraphic(this.data.get(legMode), legMode);
	}

	private JFreeChart getGraphic(final ModeData modeData, final String modeName) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("on route", false, true);
		int onRoute = 0;
		for (int i = 0; i < modeData.countsDep.length; i++) {
			onRoute = onRoute + modeData.countsDep[i] - modeData.countsArr[i] - modeData.countsStuck[i];
			double hour = i*this.binSize / 60.0 / 60.0;
			departuresSerie.add(hour, modeData.countsDep[i]);
			arrivalsSerie.add(hour, modeData.countsArr[i]);
			onRouteSerie.add(hour, onRoute);
		}

		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(
        "Leg Histogram, " + modeName + ", it." + this.iteration,
        "time", "# vehicles",
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

	public int[] getDepartures() {
		return this.allModesData.countsDep.clone();
	}

	public int[] getArrivals() {
		return this.allModesData.countsArr.clone();
	}

	public int[] getStuck() {
		return this.allModesData.countsStuck.clone();
	}

	public Set<String> getLegModes() {
		return this.data.keySet();
	}

	public int[] getDepartures(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return null;
		}
		return modeData.countsDep.clone();
	}

	public int[] getArrivals(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return null;
		}
		return modeData.countsArr.clone();
	}

	public int[] getStuck(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return null;
		}
		return modeData.countsStuck.clone();
	}

	public void writeGraphic(final String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeGraphic(final String filename, final String legMode) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(legMode), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	private ModeData getDataForMode(String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			modeData = new ModeData(nofBins + 1); // +1 for all times out of our range
			this.data.put(legMode, modeData);
		}
		return modeData;
	}

	private static class ModeData {
		public final int[] countsDep;
		public final int[] countsArr;
		public final int[] countsStuck;

		public ModeData(final int nofBins) {
			this.countsDep = new int[nofBins];
			this.countsArr = new int[nofBins];
			this.countsStuck = new int[nofBins];
		}
	}

}
