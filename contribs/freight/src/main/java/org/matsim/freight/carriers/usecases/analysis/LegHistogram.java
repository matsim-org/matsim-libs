/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.analysis;

import jakarta.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Time;

/**
 * It is a copy of {@link org.matsim.analysis.LegHistogram}. It is modified to include or exclude persons.
 *
 * @author mrieser
 *
 * Counts the number of vehicles departed, arrived or got stuck per time bin
 * based on events.
 */
public class LegHistogram implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private int iteration = 0;
	private final int binSize;
	private final int nofBins;
	private final Map<String, ModeData> data = new TreeMap<>();
	private ModeData allModesData = null;

	private boolean inclPopulation = true;

	private Population population;

	/**
	 * If true, it observes persons of population. Otherwise, it excludes them from observation.
	 *
	 * @param inclPop
	 */
	public LegHistogram setInclPop(boolean inclPop) {
		this.inclPopulation = inclPop;
		return this;
	}



	/**
	 * Sets the population.
	 *
	 * @param population the population to set
	 */
	@Inject
	public void setPopulation(Population population) {
		this.population = population;
	}



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

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		if(personIsUnderObservation(event.getPersonId())) {
			int index = getBinIndex(event.getTime());
			this.allModesData.countsDep[index]++;
			if (event.getLegMode() != null) {
				ModeData modeData = getDataForMode(event.getLegMode());
				modeData.countsDep[index]++;
			}
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		if(personIsUnderObservation(event.getPersonId())) {
			int index = getBinIndex(event.getTime());
			this.allModesData.countsArr[index]++;
			if (event.getLegMode() != null) {
				ModeData modeData = getDataForMode(event.getLegMode());
				modeData.countsArr[index]++;
			}
		}
	}

	@Override
	public void handleEvent(final PersonStuckEvent event) {
		if(personIsUnderObservation(event.getPersonId())) {
			int index = getBinIndex(event.getTime());
			this.allModesData.countsStuck[index]++;
			if (event.getLegMode() != null) {
				ModeData modeData = getDataForMode(event.getLegMode());
				modeData.countsStuck[index]++;
			}
		}
	}

	private boolean personIsUnderObservation(Id<Person> personId) {
		if(inclPopulation){
			if(population == null) return false;
			return population.getPersons().containsKey(personId);
		}
		else {
			if(population == null) return true;
			return !population.getPersons().containsKey(personId);
		}
	}

	@Override
	public void reset(final int iter) {
		this.iteration = iter;
		this.allModesData = new ModeData(this.nofBins + 1);
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
			stream = new PrintStream(filename);
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

	/**
	 * @return a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips
	 */
	public JFreeChart getGraphic() {
		return getGraphic(this.allModesData, "all");
	}

	/**
	 * @param legMode
	 * @return a graphic showing the number of departures, arrivals and vehicles
	 * en route for all legs with the specified transportation mode
	 */
	public JFreeChart getGraphic(final String legMode) {
		return getGraphic(this.data.get(legMode), legMode);
	}

	private JFreeChart getGraphic(final ModeData modeData, final String modeName) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("en route", false, true);
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

		plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
		plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.gray);
		plot.setDomainGridlinePaint(Color.gray);

		return chart;
	}

	/**
	 * @return number(s) of departures per time-bin, for all legs
	 */
	public int[] getDepartures() {
		return this.allModesData.countsDep.clone();
	}

	/**
	 * @return number(s) of all arrivals per time-bin, for all legs
	 */
	public int[] getArrivals() {
		return this.allModesData.countsArr.clone();
	}

	/**
	 * @return number(s) of all vehicles that got stuck in a time-bin, for all legs
	 */
	public int[] getStuck() {
		return this.allModesData.countsStuck.clone();
	}

	/**
	 * @return Set of all transportation modes data is available for
	 */
	public Set<String> getLegModes() {
		return this.data.keySet();
	}

	/**
	 * @param legMode transport mode
	 * @return number(s) of departures per time-bin, for all legs with the specified mode
	 */
	public int[] getDepartures(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return new int[0];
		}
		return modeData.countsDep.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number(s) of all arrivals per time-bin, for all legs with the specified mode
	 */
	public int[] getArrivals(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return new int[0];
		}
		return modeData.countsArr.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number(s) of vehicles that got stuck in a time-bin, for all legs with the specified mode
	 */
	public int[] getStuck(final String legMode) {
		ModeData modeData = this.data.get(legMode);
		if (modeData == null) {
			return new int[0];
		}
		return modeData.countsStuck.clone();
	}

	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips to the specified file.
	 *
	 * @param filename
	 *
	 * @see #getGraphic()
	 */
	public void writeGraphic(final String filename) {
		try {
			ChartUtils.saveChartAsPNG(new File(filename), getGraphic(), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips with the specified transportation mode to the
	 * specified file.
	 *
	 * @param filename
	 * @param legMode
	 *
	 * @see #getGraphic(String)
	 */
	public void writeGraphic(final String filename, final String legMode) {
		try {
			ChartUtils.saveChartAsPNG(new File(filename), getGraphic(legMode), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		return Math.min(bin, this.nofBins);
	}

	private ModeData getDataForMode(final String legMode) {
		// +1 for all times out of our range
		return this.data.computeIfAbsent(legMode, k -> new ModeData(this.nofBins + 1));
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
