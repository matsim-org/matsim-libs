/* *********************************************************************** *
 * project: org.matsim.*
 * ModeDistanceSharesEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package herbie.running.analysis;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.log4j.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.geometry.CoordUtils;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;

/**
 * Collects and processes data on the mode shares, based on the travelled
 * (euclidean) distance.
 * Can also produce statistics on the mode shares based on the number of
 * legs.
 *
 * @author thibautd
 */
public class ModeSharesEventHandler
		extends AbstractClassifiedFrequencyAnalysis
		implements AgentDepartureEventHandler, AgentArrivalEventHandler {
	private static final Logger log =
		Logger.getLogger(ModeSharesEventHandler.class);

	// use euclidean distance rather than network distance, as linkEvents
	// are not generated for non-car modes.
	private static final String ALL_MODES = "allAvailableModes";

	private final Map<Id, AgentDepartureEvent> pendantDepartures =
			new HashMap<Id, AgentDepartureEvent>();
	private final Network network;

	private final List<Map<String, Double>> modeShares;
	private boolean toReset = false;
	private double maxDistance = 0d;
		
	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	/**
	 * @param controler the controler, used to get the network
	 */
	public ModeSharesEventHandler(final Controler controler) {
		this.network = controler.getNetwork();
		this.modeShares = new ArrayList<Map<String, Double>>(controler.getLastIteration());
	}
	
	/*
	 * =========================================================================
	 * Handling methods
	 * =========================================================================
	 */
	@Override
	public void reset(final int iteration) {
		// only reset at the first fired event, so that the order in which
		// reset and getResult methods are called at the end of the iteration
		// does not matter.
		this.toReset = true;
	}

	private void doReset() {
		if (this.toReset) {
			this.toReset = false;
			this.frequencies.clear();
			this.rawData.clear();

			if (this.pendantDepartures.size() > 0) {
				log.warn("Some arrivals were not handled!");
				this.pendantDepartures.clear();
			}
			this.maxDistance = 0d;
		}
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		this.doReset();
		// catch the previous value to check consistency of the process
		AgentDepartureEvent old =
			this.pendantDepartures.put(event.getPersonId(), event);

		if (old != null) {
			log.warn("One departure were not handled before the following one "+
					" for agent "+event.getPersonId());
		}
	}

	@Override
	public void handleEvent(final AgentArrivalEvent arrivalEvent) {
		this.doReset();
		AgentDepartureEvent departureEvent =
			this.pendantDepartures.remove(arrivalEvent.getPersonId());
		String mode = arrivalEvent.getLegMode();
		Frequency frequency;
		ResizableDoubleArray rawDataElement;
		Link departureLink;
		Link arrivalLink;
		double distance;

		// Consistency check...
		if (departureEvent == null) {
			log.warn("One arrival do not correspond to any departure for agent "+
					arrivalEvent.getPersonId());
			return;
		}
		else if (!mode.equals(departureEvent.getLegMode())) {
			log.warn("Departure and arrival have uncompatible modes!");
			return;
		}
		// consistency check... DONE
		
		if (this.frequencies.containsKey(mode)) {
			frequency = this.frequencies.get(mode);
			rawDataElement = this.rawData.get(mode);
		}
		else {
			frequency = new Frequency();
			rawDataElement = new ResizableDoubleArray();

			this.frequencies.put(mode, frequency);
			this.rawData.put(mode, rawDataElement);
		}

		// compute data
		departureLink = this.network.getLinks().get(departureEvent.getLinkId());
		arrivalLink = this.network.getLinks().get(arrivalEvent.getLinkId());

		distance = CoordUtils.calcDistance(
				departureLink.getCoord(),
				arrivalLink.getCoord());

		// remember data
		frequency.addValue(distance);
		rawDataElement.addElement(distance);
		this.maxDistance = Math.max(distance, this.maxDistance);
	}

	/*
	 * =========================================================================
	 * processing methods
	 * =========================================================================
	 */
	/**
	 * Helper method to perform any necessary data processing before clearing
	 * data structures.
	 */
	private void processEndOfIteration(final int iteration) {
		//this.modeShares.add(iteration, getModeShares());
	}

	private Map<String, Double> getModeDistances() {
		Map<String, Double> modeDistances = new HashMap<String, Double>();
		double totalDistance = 0d;
		double currentDistance;

		for (String mode : this.rawData.keySet()) {
			currentDistance = 0d;
			for (double d : this.rawData.get(mode).getElements()) {
				currentDistance += d;
			}
			totalDistance += currentDistance;
			modeDistances.put(mode, currentDistance);
		}

		modeDistances.put(ALL_MODES, totalDistance);

		return modeDistances;
	}

	/**
	 * Logs some statistics on mode shares.
	 */
	public void printInfo(final int iteration) {
		//this.processEndOfIteration(iteration);
		//Map<String, Double> currentModeShares = this.modeShares.get(iteration);
		Map<String, Double> currentModeDistances = getModeDistances();
		long nLegs = this.getNumberOfLegs();
		double totalDist = currentModeDistances.remove(ALL_MODES);

		log.info("Mode shares:");
		log.info("Cumulated traveled distance: "+(totalDist/1000d)+"km");
		for (Map.Entry<String, Double> mode : currentModeDistances.entrySet()) {
			log.info("Share of "+mode.getKey()+":\t"+
					"distance: "+(100d*mode.getValue() / totalDist)+"%"+
					"\tnumber of legs: "+
					(100d * this.getNumberOfLegs(mode.getKey()) / nLegs)+"%");
		}
	}

	/**
	 * @return a {@link org.jfree.data.xy.XYSeries} representing the
	 * distribution of traveled distances for the mode (histogramm),
	 * or null if the mode is unknown (only modes that generated events
	 * in the previous mobsim run are "known").
	 *
	 * @param step the width of the bins, in meters
	 * @param mode the mode to get the data for
	 */
	public XYSeries getTraveledDistancesHistogram(final String mode, final double step) {
		boolean autoSort = false;
		boolean allowDuplicateXValues = true;
		XYSeries output = new XYSeries(mode, autoSort, allowDuplicateXValues);
		double[] modeRawData;
		try {
			modeRawData = this.rawData.get(mode).getElements();
		} catch (NullPointerException e) {
			return null;
		}
		Arrays.sort(modeRawData);
		double currentUpperBound = step;
		int count = 0;

		for (double distValue : modeRawData) {
			if (distValue < currentUpperBound) {
				count++;
			}
			else {
				// add the previous bin to the plot
				output.add(currentUpperBound - step, count);
				//output.add(currentUpperBound, count);

				currentUpperBound += step;
				while (distValue > currentUpperBound) {
					output.add(currentUpperBound, 0d);
					currentUpperBound += step;
				}
				count = 1;
			}
		}
		//add the last count
		output.add(currentUpperBound - step, count);
		//output.add(currentUpperBound, count);
		output.add(currentUpperBound, 0);

		return output;
	}

	public HistogramDataset getHistogramDataset(final int nBins) {
		HistogramDataset output = new HistogramDataset();
		output.setType(HistogramType.RELATIVE_FREQUENCY);

		for (String mode : this.rawData.keySet()) {
			output.addSeries(mode, this.rawData.get(mode).getElements(), nBins);
		}

		return output;
	}

	// would be more readable with a "real" histogramm.
	// TODO: use matsim chartUtils
	public JFreeChart getTraveledDistancesHistogram(final int numberOfBins) {
		String title = "Traveled distances distribution by mode";
		String xLabel = "Distance (m)";
		String yLabel = "Number of trips";
		boolean legend = true;
		boolean tooltips = false;
		boolean urls = false;
		//XYSeriesCollection data = new XYSeriesCollection();
		HistogramDataset data = getHistogramDataset(numberOfBins);
		double step = this.maxDistance / numberOfBins;

		//for (String mode : this.rawData.keySet()) {
		//	data.addSeries(getTraveledDistancesHistogram(mode, step));
		//}

		JFreeChart chart = ChartFactory.createHistogram(title, xLabel, yLabel, data,
				PlotOrientation.VERTICAL, legend, tooltips, urls);
		chart.getXYPlot().setForegroundAlpha(0.5F);

		return chart;
	}

	public void writeTraveledDistancesGraphic(
			final String fileName,
			final int numberOfBins) {
		JFreeChart chart = getTraveledDistancesHistogram(numberOfBins);
		try {
			ChartUtilities.saveChartAsPNG(new File(fileName), chart, 1024, 768);
		} catch (IOException e) {
			log.error("got an error while trying to write graphics to file."+
					" Error is not fatal, but output may be incomplete.");
			e.printStackTrace();
		}
	}

	/*
	 * =========================================================================
	 * Miscelaneous
	 * =========================================================================
	 */
	@Override
	public void run(final Person person) { /*do nothing*/ }
}

