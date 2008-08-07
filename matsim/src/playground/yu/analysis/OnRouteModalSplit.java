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

package playground.yu.analysis;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.events.AgentEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

/**
 * @author mrieser
 * 
 * Counts the number of vehicles departed, arrived or got stuck per time bin
 * based on events.
 */
public class OnRouteModalSplit implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, AgentStuckEventHandler {

	private int iteration = 0;
	private final int binSize;
	private final int[] dep, arr, stuck, onRoute;
	private final int[] carDep, carArr, carStuck, carOnRoute;
	private final int[] ptDep, ptArr, ptOnRoute;
	private final int[] otherDep, otherArr, otherStuck, otherOnRoute;
	private final NetworkLayer network;
	private final Population plans;

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified
	 * number of bins.
	 * 
	 * @param binSize
	 *            The size of a time bin in seconds.
	 * @param nofBins
	 *            The number of time bins for this analysis.
	 */
	public OnRouteModalSplit(final int binSize, final int nofBins,
			NetworkLayer network, Population plans) {
		super();
		this.binSize = binSize;
		this.dep = new int[nofBins + 1]; // +1 for all times out of our
		// range
		this.arr = new int[nofBins + 1];
		this.stuck = new int[nofBins + 1];
		this.carArr = new int[nofBins + 1];
		this.carDep = new int[nofBins + 1];
		this.carStuck = new int[nofBins + 1];
		this.ptArr = new int[nofBins + 1];
		this.ptDep = new int[nofBins + 1];
		this.onRoute = new int[nofBins + 1];
		this.carOnRoute = new int[nofBins + 1];
		this.ptOnRoute = new int[nofBins + 1];
		this.otherDep = new int[nofBins + 1];
		this.otherArr = new int[nofBins + 1];
		this.otherOnRoute = new int[nofBins + 1];
		this.otherStuck = new int[nofBins + 1];
		reset(0);
		this.network = network;
		this.plans = plans;
	}

	/**
	 * Creates a new LegHistogram with the specified binSize and a default
	 * number of bins, such that 30 hours are analyzed.
	 * 
	 * @param binSize
	 *            The size of a time bin in seconds.
	 */
	public OnRouteModalSplit(final int binSize, NetworkLayer network,
			Population plans) {
		this(binSize, 30 * 3600 / binSize + 1, network, plans);
	}

	public OnRouteModalSplit(NetworkLayer network, Population plans) {
		this(300, network, plans);
	}

	/* Implementation of eventhandler-Interfaces */

	public void handleEvent(final AgentDepartureEvent event) {
		internHandleEvent(event, this.dep, this.carDep, this.ptDep,
				this.otherDep);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		internHandleEvent(event, this.arr, this.carArr, this.ptArr,
				this.otherArr);
	}

	public void handleEvent(final AgentStuckEvent event) {
		internHandleEvent(event, this.stuck, this.carStuck, null,
				this.otherStuck);
	}

	private void internHandleEvent(AgentEvent ae, int[] allCount,
			int[] carCount, int[] ptCount, int[] otherCount) {
		int binIdx = getBinIndex(ae.time);
		allCount[binIdx]++;

		if (ae.agent == null) {
			// rebuild event
			ae.agent = this.plans.getPerson(ae.agentId);
		}
		
		Plan.Type planType = ae.agent.getSelectedPlan().getType();
		if (Integer.parseInt(ae.agentId) > 1000000000)
			otherCount[binIdx]++;
		if (planType.equals(Plan.Type.CAR)) {
			carCount[binIdx]++;
		} else if (planType.equals(Plan.Type.PT)) {
			if (ptCount != null)
				ptCount[binIdx]++;
		}
	}

	/* output methods */

	/**
	 * Writes the gathered data tab-separated into a text file.
	 * 
	 * @param filename
	 *            The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		BufferedWriter bw;
		try {
			bw = IOUtils.getBufferedWriter(filename);
			write(bw);
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void calcOnRoute() {
		this.onRoute[0] = this.dep[0] - this.arr[0] - this.stuck[0];
		this.carOnRoute[0] = this.carDep[0] - this.carArr[0] - this.carStuck[0];
		this.ptOnRoute[0] = this.ptDep[0] - this.ptArr[0];
		this.otherOnRoute[0] = this.otherDep[0] - this.otherArr[0]
				- this.otherStuck[0];
		for (int i = 1; i < this.dep.length; i++) {
			this.onRoute[i] = this.onRoute[i - 1] + this.dep[i] - this.arr[i]
					- this.stuck[i];
			this.carOnRoute[i] = this.carOnRoute[i - 1] + this.carDep[i]
					- this.carArr[i] - this.carStuck[i];
			this.ptOnRoute[i] = this.ptOnRoute[i - 1] + this.ptDep[i]
					- this.ptArr[i];
			this.otherOnRoute[i] = this.otherOnRoute[i - 1] + this.otherDep[i]
					- this.otherArr[i] - this.otherStuck[i];
		}
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 * 
	 * @param bw
	 *            The data stream where to write the gathered data.
	 */
	public void write(final BufferedWriter bw) {
		calcOnRoute();
		try {
			bw
					.write("time\ttimeBin\tdepartures\tarrivals\tstuck\ton_route"
							+ "\tcarDepartures\tcarArrivals\tcarStuck\tcarOnRoute"
							+ "\tptDepartures\tptArrivals\tptStuck\tptOnRoute"
							+ "\totherDepartures\totherArrivals\totherStuck\totherOnRoute"
							+ "\n");
			for (int i = 0; i < this.dep.length; i++) {
				bw.write(Time.writeTime(i * this.binSize) + "\t"
						+ i * this.binSize + "\t" + this.dep[i] + "\t"
						+ this.arr[i] + "\t" + this.stuck[i] + "\t"
						+ this.onRoute[i] + "\t" + this.carDep[i] + "\t"
						+ this.carArr[i] + "\t" + this.carStuck[i] + "\t"
						+ this.carOnRoute[i] + "\t" + this.ptDep[i] + "\t"
						+ this.ptArr[i] + "\t" + 0 + "\t" + this.ptOnRoute[i]
						+ "\t" + this.otherDep[i] + "\t" + this.otherArr[i]
						+ "\t" + this.otherStuck[i] + "\t"
						+ this.otherOnRoute[i] + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public JFreeChart getGraphic() {

		final XYSeriesCollection data = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("on route", false, true);
		int onRoute = 0;
		for (int i = 0; i < this.dep.length; i++) {
			onRoute = onRoute + this.dep[i] - this.arr[i] - this.stuck[i];
			double hour = i * this.binSize / 60.0 / 60.0;
			departuresSerie.add(hour, this.dep[i]);
			arrivalsSerie.add(hour, this.arr[i]);
			onRouteSerie.add(hour, onRoute);
		}

		data.addSeries(departuresSerie);
		data.addSeries(arrivalsSerie);
		data.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(
				"Leg Histogram, it." + this.iteration, "time", "# vehicles",
				data, PlotOrientation.VERTICAL, true, // legend
				false, // tooltips
				false // urls
				);

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}

	public void writeGraphic(final String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(),
					1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.dep.length) {
			return this.dep.length - 1;
		}
		return bin;
	}

	public void reset(final int iteration) {
		this.iteration = iteration;
		for (int i = 0; i < this.dep.length; i++) {
			this.dep[i] = 0;
			this.arr[i] = 0;
			this.stuck[i] = 0;
		}
	}

	public void writeCharts(final String filename) {
		double[] category = new double[this.dep.length + 1];
		for (int j = 0; j < category.length; j++) {
			category[j] = ((double) j) * (double) this.binSize / 3600.0;
		}
		XYLineChart onRouteChart = new XYLineChart("Leg Histogramm", "time",
				"vehicles");
		int length = this.onRoute.length;
		double[] onRoute = new double[length];
		double[] carOnRoute = new double[length];
		double[] ptOnRoute = new double[length];
		double[] otherOnRoute = new double[length];
		for (int i = 0; i < length; i++) {
			onRoute[i] = this.onRoute[i];
			carOnRoute[i] = this.carOnRoute[i];
			ptOnRoute[i] = this.ptOnRoute[i];
			otherOnRoute[i] = this.otherOnRoute[i];
		}
		onRouteChart.addSeries("all agents on route", category, onRoute);
		onRouteChart.addSeries("drivers on route", category, carOnRoute);
		onRouteChart.addSeries("public transit users on route", category,
				ptOnRoute);
		onRouteChart.addSeries("others on route", category, otherOnRoute);
		onRouteChart.saveAsPng(filename, 1024, 768);
	}
}
