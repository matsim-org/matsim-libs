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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			PersonArrivalEventHandler, ShutdownListener, StartupListener, IterationEndsListener {
	private static final Logger log =
		Logger.getLogger(ModeAnalysis.class);

	private static final String X_TITLE = "iteration";
	private static final String Y_TITLE = "n legs";

	private final static int NO_VALUE = Integer.MIN_VALUE;
	private final static String FILE_NAME = "modeEvolution";
	private final static String title = "Number of legs";
	private final static int height = 600;
	private final static int width = 800;

	private final boolean outputAtEachIteration;

	private Map<String, Counter> modeCount = null;
	private final List<Map<String, Counter>> iterationsModeCounts =
			new ArrayList<Map<String, Counter>>();
	private int firstIter = NO_VALUE;

	public ModeAnalysis(final boolean outputAtEachIteration) {
		this.outputAtEachIteration = outputAtEachIteration;
	}

	// /////////////////////////////////////////////////////////////////////////
	// Handling methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler c = event.getControler();
		
		if (c != null) {
			c.getEvents().addHandler(this);
		}
		else {
			log.warn( "no Controler returned by StartupEvent. The listener will not add itself to the events manager." );
		}
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
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
		if (!outputAtEachIteration) outputGraphs( event );
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (outputAtEachIteration) outputGraphs( event );
	}

	private void outputGraphs(final ControlerEvent event) {
		List<String> modes = getUsedModes();
		double[] xAxis = getXAxis();
		Map<String, double[]> yAxes = getYAxes(modes);
		String fileName = event.getControler().getControlerIO().getOutputFilename(FILE_NAME);
		XYLineChart globalChart = new XYLineChart(title+", all modes", X_TITLE, Y_TITLE);
		XYLineChart particularChart;

		for (String mode : modes) {
			particularChart =  new XYLineChart(title+", "+mode, X_TITLE, Y_TITLE);
			globalChart.addSeries(mode, xAxis, yAxes.get(mode));
			particularChart.addSeries(mode, xAxis, yAxes.get(mode));
			writeChart(particularChart, fileName+"_"+mode+".png");
		}

		writeChart(globalChart, fileName+"_all.png");
	}

	private void writeChart(final XYLineChart chart, final String fileName) {
		// XYChartUtils.integerXAxis(chart.getChart());
		chart.addMatsimLogo();
		chart.saveAsPng(fileName, width, height);
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
	
	private static class Counter {
		private int counter = 0;

		public void increment() {
			counter++;
		}

		public int getValue() {
			return counter;
		}
	}
}
