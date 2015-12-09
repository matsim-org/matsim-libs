/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypesAnalysis.java
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
package playground.thibautd.analysis.listeners;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
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
 * @author thibautd
 */
public class ActivityTypesAnalysis implements
			ActivityStartEventHandler, ShutdownListener, StartupListener, IterationEndsListener {
	private static final String X_TITLE = "iteration";
	private static final String Y_TITLE = "n activities";

	private final static int NO_VALUE = Integer.MIN_VALUE;
	private final static String FILE_NAME = "activityEvolution";
	private final static String title = "Number of activities";
	private final static int height = 600;
	private final static int width = 800;

	private final boolean outputAtEachIteration;

	private Map<String, Counter> typeCount = null;
	private final List<Map<String, Counter>> iterationsTypeCounts =
			new ArrayList<Map<String, Counter>>();
	private int firstIter = NO_VALUE;

	public ActivityTypesAnalysis(final boolean outputAtEachIteration) {
		this.outputAtEachIteration = outputAtEachIteration;
	}

	// /////////////////////////////////////////////////////////////////////////
	// Handling methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void notifyStartup(final StartupEvent event) {
		event.getControler().getEvents().addHandler(this);
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		this.handleType(event.getActType());
	}

	@Override
	public void reset(final int iteration) {
		if (firstIter == NO_VALUE) {
			firstIter = iteration;
		}
		typeCount = new HashMap<String, Counter>();
		iterationsTypeCounts.add(typeCount);
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
		List<String> modes = getExistingTypes();
		double[] xAxis = getXAxis();
		Map<String, double[]> yAxes = getYAxes(modes);
		String fileName = event.getControler().getControlerIO().getOutputFilename(FILE_NAME);
		XYLineChart globalChart = new XYLineChart(title+", all types", X_TITLE, Y_TITLE);
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
	private void handleType(final String type) {
		Counter counter = this.typeCount.get( type );

		if (counter == null) {
			counter = new Counter();
			this.typeCount.put(type, counter);
		}

		counter.increment();
	}

	private List<String> getExistingTypes() {
		List<String> types = new ArrayList<String>();

		for (Map<String, ? extends Object> iterTypes : this.iterationsTypeCounts) {
			for (String type : iterTypes.keySet()) {
				if (!types.contains( type )) {
					types.add( type );
				}
			}
		}

		return types;
	}

	private double[] getXAxis() {
		double[] output = new double[this.iterationsTypeCounts.size()];

		for (int i = 0; i < output.length; i++) {
			output[i] = firstIter + i;
		}

		return output;
	}

	private Map<String, double[]> getYAxes(final List<String> types) {
		Map<String, double[]> output = new HashMap<String, double[]>();
		Counter currentCount;
		int i = 0;

		for (String type : types) {
			output.put(type, new double[this.iterationsTypeCounts.size()]);
		}

		for (Map<String, Counter> iterData : this.iterationsTypeCounts) {
			for (String type : types) {
				currentCount = iterData.get(type);

				if (currentCount == null) {
					output.get(type)[i] = 0;
				}
				else {
					output.get(type)[i] = currentCount.getValue();
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
