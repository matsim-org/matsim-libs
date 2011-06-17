/* *********************************************************************** *
 * project: org.matsim.*
 * ModeAnalysis.java
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
package playground.thibautd.analysis.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * Event handler that computes statistics about the evolution of chosen
 * modes across the MATSim iterations.
 *
 * It counts only the trips <u>really</u> realised (ie, for which the agent
 * arrived at destination).
 *
 * This handler must be added as a constroler listener: it takes care about adding
 * itself as an event handler. This allows it to use the {@link ShutdownEvent} to
 * output all necessary files.
 *
 * @author thibautd
 */
public class ModeAnalysis implements
			AgentArrivalEventHandler, ShutdownListener, StartupListener {
	private static final Logger log =
		Logger.getLogger(ModeAnalysis.class);


	private final static int NO_VALUE = Integer.MIN_VALUE;
	private final static String fileName = "modeEvolution";
	private final static String title = "Number of legs";
	private final static int height = 600;
	private final static int width = 800;

	private Map<String, Counter> modeCount = null;
	private final List<Map<String, Counter>> iterationsModeCounts =
			new ArrayList<Map<String, Counter>>();
	private int firstIter = NO_VALUE;

	// /////////////////////////////////////////////////////////////////////////
	// Handling methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void notifyStartup(final StartupEvent event) {
		event.getControler().getEvents().addHandler(this);
	}

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		this.handleMode(event.getLegMode());
	}

	@Override
	public void reset(final int iteration) {
		if (firstIter == NO_VALUE) {
			firstIter = iteration;
		}
		modeCount = new HashMap<String, Counter>();
		iterationsModeCounts.add(modeCount);
	}

	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		List<String> modes = getUsedModes();
		double[] xAxis = getXAxis();
		Map<String, double[]> yAxes = getYAxes(modes);
		String fileName = event.getControler().getControlerIO().getOutputFilename(this.fileName);
		XYLineChart globalChart = new XYLineChart(title+", all modes", "iteration", "n legs");

		for (String mode : modes) {
			globalChart.addSeries(mode, xAxis, yAxes.get(mode));
		}

		globalChart.saveAsPng(fileName+"_all.png", height, width);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private void handleMode(final String mode) {
		Counter counter = this.modeCount.get(mode);

		if (counter == null) {
			counter = new Counter();
			this.modeCount.put(mode, counter);
		}

		counter.increment();
	}

	private List<String> getUsedModes() {
		List<String> modes = new ArrayList<String>();

		for (Map<String, ? extends Object> iterModes : this.iterationsModeCounts) {
			for (String mode : iterModes.keySet()) {
				if (!modes.contains(mode)) {
					modes.add(mode);
				}
			}
		}

		return modes;
	}

	private double[] getXAxis() {
		double[] output = new double[this.iterationsModeCounts.size()];

		for (int i = 0; i < output.length; i++) {
			output[i] = firstIter + i;
		}

		return output;
	}

	private Map<String, double[]> getYAxes(final List<String> modes) {
		Map<String, double[]> output = new HashMap<String, double[]>();
		Counter currentCount;
		int i = 0;

		for (String mode : modes) {
			output.put(mode, new double[this.iterationsModeCounts.size()]);
		}

		for (Map<String, Counter> iterData : this.iterationsModeCounts) {
			for (String mode : modes) {
				currentCount = iterData.get(mode);

				if (currentCount == null) {
					output.get(mode)[i] = 0;
				}
				else {
					output.get(mode)[i] = currentCount.getValue();
				}
			}
			i++;
		}

		return output;
	}
	
	private class Counter {
		private int counter = 0;

		public void increment() {
			counter++;
		}

		public int getValue() {
			return counter;
		}
	}
}
