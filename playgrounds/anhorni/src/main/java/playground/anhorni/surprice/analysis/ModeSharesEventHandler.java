/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.anhorni.surprice.analysis;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.utils.geometry.CoordUtils;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Collects and processes data on the mode shares, based on the traveled
 * (euclidean) distance.
 * Can also produce statistics on the mode shares based on the number of
 * legs.
 *
 * @author thibautd adapted by anhorni
 */
public class ModeSharesEventHandler extends AbstractClassifiedFrequencyAnalysis implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	private static final Logger log = Logger.getLogger(ModeSharesEventHandler.class);

	// use euclidean distance rather than network distance, as linkEvents
	// are not generated for non-car modes.
	private static final String ALL_MODES = "allAvailableModes";

	private final Map<Id, PersonDepartureEvent> pendantDepartures = new HashMap<Id, PersonDepartureEvent>();
	private final Network network;
	private boolean toReset = false;
	private double maxXYForPlotting = 100000; // [m]
	private String xy;
		

	public ModeSharesEventHandler(final MatsimServices controler, String xy) {
        this.network = controler.getScenario().getNetwork();
		this.xy = xy;
		
		if (this.xy.equals("times")) {
			maxXYForPlotting = 3600.0;
		}
	}
	
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
			this.maxXYForPlotting = 100000; // [m]
		}
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		this.doReset();
		// catch the previous value to check consistency of the process
		PersonDepartureEvent old =
			this.pendantDepartures.put(event.getPersonId(), event);

		if (old != null) {
			log.warn("One departure were not handled before the following one "+
					" for agent "+event.getPersonId());
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent arrivalEvent) {
		this.doReset();
		PersonDepartureEvent departureEvent = this.pendantDepartures.remove(arrivalEvent.getPersonId());
		String mode = arrivalEvent.getLegMode();
		Frequency frequency;
		ResizableDoubleArray rawDataElement;
		
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
		double xyVal = 0.0;
		if (this.xy.equals("times")) {
			xyVal = this.computeTimes(arrivalEvent, departureEvent);
		}
		else {
			xyVal = this.computeDistances(arrivalEvent, departureEvent);
		}
		// remember data
		frequency.addValue(xyVal);
		rawDataElement.addElement(xyVal);
	}
		
	private double computeTimes(final PersonArrivalEvent arrivalEvent, final PersonDepartureEvent departureEvent) {
		return (arrivalEvent.getTime() - departureEvent.getTime());
	}
	
	private double computeDistances(final PersonArrivalEvent arrivalEvent, final PersonDepartureEvent departureEvent) {
		Link departureLink;
		Link arrivalLink;
		double distance;
		departureLink = this.network.getLinks().get(departureEvent.getLinkId());
		arrivalLink = this.network.getLinks().get(arrivalEvent.getLinkId());

		distance = CoordUtils.calcDistance(
				departureLink.getCoord(),
				arrivalLink.getCoord());

		return distance;
	}

	private Map<String, Double> getModeXYs() {
		Map<String, Double> modeXYs = new HashMap<String, Double>();
		double totalXY = 0d;
		double currentXY;

		for (String mode : this.rawData.keySet()) {
			currentXY = 0d;
			for (double d : this.rawData.get(mode).getElements()) {
				currentXY += d;
			}
			totalXY += currentXY;
			modeXYs.put(mode, currentXY);
		}
		modeXYs.put(ALL_MODES, totalXY);
		return modeXYs;
	}

	public void printInfo(final int iteration) {
		Map<String, Double> currentModeXYs = getModeXYs();
		double totalXY = currentModeXYs.remove(ALL_MODES);

		for (Map.Entry<String, Double> mode : currentModeXYs.entrySet()) {
			if (this.xy.equals("times")) {
				log.info("Share of " + mode.getKey() + ":\t" + "time [s]: "+ (100d*mode.getValue() / totalXY) +"%");
			}
			else {
				log.info("Share of " + mode.getKey() + ":\t" + "distance [km]: " + (100d*mode.getValue() / totalXY) +"%");
			}
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
	public XYSeries getTraveledXYsHistogram(final String mode, final double step) {
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

		for (double xyValue : modeRawData) {
			if (xyValue < currentUpperBound) {
				count++;
			}
			else {
				// add the previous bin to the plot
				output.add(currentUpperBound - step, count);
				//output.add(currentUpperBound, count);

				currentUpperBound += step;
				while (xyValue > currentUpperBound) {
					output.add(currentUpperBound, 0d);
					currentUpperBound += step;
				}
				count = 1;
			}
		}
		//add the last count
		output.add(currentUpperBound - step, count);
		output.add(currentUpperBound, 0);
		return output;
	}

	public HistogramDataset getHistogramDataset(final int nBins) {
		HistogramDataset output = new HistogramDataset();
		output.setType(HistogramType.RELATIVE_FREQUENCY);
		ArrayList<Double> croppedData = new ArrayList<Double>();
		
		for (String mode : this.rawData.keySet()) {
			for (double d : this.rawData.get(mode).getElements()) {
				if (d < this.maxXYForPlotting) {
					croppedData.add(d);
				}
			}
			output.addSeries(mode, Utils.convert(croppedData), nBins);
		}
		return output;
	}

	private JFreeChart getTraveledXYsHistogram(final int numberOfBins) {
		String title = this.xy + " distribution by mode";
		String xLabel = "distance (m)";
		if (this.xy.equals("times")) {
			xLabel = "time (s)";
		}		
		String yLabel = "number of trips";
		boolean legend = true;
		boolean tooltips = false;
		boolean urls = false;
		HistogramDataset data = this.getHistogramDataset(numberOfBins);

		JFreeChart chart = ChartFactory.createHistogram(title, xLabel, yLabel, data,
				PlotOrientation.VERTICAL, legend, tooltips, urls);
		return chart;
	}

	public void writeXYsGraphic(final String fileName, final int numberOfBins) {
		JFreeChart chart = this.getTraveledXYsHistogram(numberOfBins);
		try {
			ChartUtilities.saveChartAsPNG(new File(fileName), chart, 1024, 768);
		} catch (IOException e) {
			log.error("got an error while trying to write graphics to file."+
					" Error is not fatal, but output may be incomplete.");
			e.printStackTrace();
		}
	}

	@Override
	public void run(final Person person) { /*do nothing*/ }
}

