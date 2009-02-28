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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

/**
 * @author mrieser
 * 
 *         Counts the number of vehicles departed, arrived or got stuck per time
 *         bin based on events.
 */
public class OnRouteModalSplit implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, AgentStuckEventHandler {
	private String scenario;
	private int iteration = 0;
	private final int binSize;
	private final int[] dep, arr, stuck, onRoute;
	private final int[] carDep, carArr, carStuck, carOnRoute;
	private final int[] ptDep, ptArr, ptOnRoute;
	private final int[] wlkDep, wlkArr, wlkOnRoute;
	private int[] otherDep = null, otherArr = null, otherStuck = null,
			otherOnRoute = null;
	// private final NetworkLayer network;
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
	public OnRouteModalSplit(String scenario, final int binSize,
			final int nofBins, Population plans) {
		super();
		this.scenario = scenario;
		this.binSize = binSize;
		this.dep = new int[nofBins + 1]; // +1 for all times out of our
		// range
		// total
		this.arr = new int[nofBins + 1];
		this.stuck = new int[nofBins + 1];
		this.onRoute = new int[nofBins + 1];
		// car
		this.carArr = new int[nofBins + 1];
		this.carDep = new int[nofBins + 1];
		this.carStuck = new int[nofBins + 1];
		this.carOnRoute = new int[nofBins + 1];
		// pt
		this.ptArr = new int[nofBins + 1];
		this.ptDep = new int[nofBins + 1];
		this.ptOnRoute = new int[nofBins + 1];
		// walk
		wlkArr = new int[nofBins + 1];
		wlkDep = new int[nofBins + 1];
		wlkOnRoute = new int[nofBins + 1];

		if (this.scenario.equals("Zurich")) {
			// through traffic
			this.otherDep = new int[nofBins + 1];
			this.otherArr = new int[nofBins + 1];
			this.otherOnRoute = new int[nofBins + 1];
			this.otherStuck = new int[nofBins + 1];
		}
		reset(0);
		this.plans = plans;
	}

	/**
	 * Creates a new LegHistogram with the specified binSize and a default
	 * number of bins, such that 30 hours are analyzed.
	 * 
	 * @param binSize
	 *            The size of a time bin in seconds.
	 */
	public OnRouteModalSplit(String scenario, final int binSize,
			Population plans) {
		this(scenario, binSize, 30 * 3600 / binSize + 1, plans);
	}

	public OnRouteModalSplit(String scenario, Population plans) {
		this(scenario, 300, plans);
	}

	/* Implementation of eventhandler-Interfaces */

	public void handleEvent(final AgentDepartureEvent event) {
		internHandleEvent(event, this.dep, this.carDep, this.ptDep, wlkDep,
				this.otherDep);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		internHandleEvent(event, this.arr, this.carArr, this.ptArr, wlkArr,
				this.otherArr);
	}

	public void handleEvent(final AgentStuckEvent event) {
		internHandleEvent(event, this.stuck, this.carStuck, null, null,
				this.otherStuck);
	}

	private void internHandleEvent(AgentEvent ae, int[] allCount,
			int[] carCount, int[] ptCount, int[] wlkCount, int[] otherCount) {
		int binIdx = getBinIndex(ae.time);
		allCount[binIdx]++;

		if (ae.agent == null) {
			ae.agent = this.plans.getPerson(new IdImpl(ae.agentId));
		}
		Plan selectedPlan = ae.agent.getSelectedPlan();

		if (otherCount != null)
			if (Integer.parseInt(ae.agentId) > 1000000000)
				otherCount[binIdx]++;
			else {
				if (PlanModeJudger.useCar(selectedPlan))
					carCount[binIdx]++;
				else if (PlanModeJudger.usePt(selectedPlan)) {
					if (ptCount != null)
						ptCount[binIdx]++;
				} else if (PlanModeJudger.useWalk(selectedPlan))
					if (wlkCount != null)
						wlkCount[binIdx]++;

			}
		else {
			if (PlanModeJudger.useCar(selectedPlan))
				carCount[binIdx]++;
			else if (PlanModeJudger.usePt(selectedPlan)) {
				if (ptCount != null)
					ptCount[binIdx]++;
			} else if (PlanModeJudger.useWalk(selectedPlan))
				if (wlkCount != null)
					wlkCount[binIdx]++;
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
		// *onRoute[0]
		this.onRoute[0] = this.dep[0] - this.arr[0] - this.stuck[0];
		this.carOnRoute[0] = this.carDep[0] - this.carArr[0] - this.carStuck[0];
		this.ptOnRoute[0] = this.ptDep[0] - this.ptArr[0];
		wlkOnRoute[0] = wlkDep[0] - wlkArr[0];
		if (otherOnRoute != null)
			this.otherOnRoute[0] = this.otherDep[0] - this.otherArr[0]
					- this.otherStuck[0];
		// *onRoute[i]
		for (int i = 1; i < this.dep.length; i++) {
			this.onRoute[i] = this.onRoute[i - 1] + this.dep[i] - this.arr[i]
					- this.stuck[i];
			this.carOnRoute[i] = this.carOnRoute[i - 1] + this.carDep[i]
					- this.carArr[i] - this.carStuck[i];
			this.ptOnRoute[i] = this.ptOnRoute[i - 1] + this.ptDep[i]
					- this.ptArr[i];
			this.wlkOnRoute[i] = this.wlkOnRoute[i - 1] + this.wlkDep[i]
					- this.wlkArr[i];
			if (otherOnRoute != null)
				this.otherOnRoute[i] = this.otherOnRoute[i - 1]
						+ this.otherDep[i] - this.otherArr[i]
						- this.otherStuck[i];
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
					.write("time\ttimeBin\t"
							+ "departures\tarrivals\tstuck\ton_route\t"
							+ "carDepartures\tcarArrivals\tcarStuck\tcarOnRoute\t"
							+ "ptDepartures\tptArrivals\tptStuck\tptOnRoute\t"
							+ "walkDepartures\twalkArrivals\twalkStuck\twalkOnRoute\t"
							+ "otherDepartures\totherArrivals\totherStuck\totherOnRoute\n");
			for (int i = 0; i < this.dep.length; i++) {
				bw.write(Time.writeTime(i * this.binSize)
						+ "\t"
						+ i * this.binSize
						+ "\t"
						+ this.dep[i]
						+ "\t"
						+ this.arr[i]
						+ "\t"
						+ this.stuck[i]
						+ "\t"
						+ this.onRoute[i]
						+ "\t"
						+ this.carDep[i]
						+ "\t"
						+ this.carArr[i]
						+ "\t"
						+ this.carStuck[i]
						+ "\t"
						+ this.carOnRoute[i]
						+ "\t"
						+ this.ptDep[i]
						+ "\t"
						+ this.ptArr[i]
						+ "\t"
						+ 0
						+ "\t"
						+ this.ptOnRoute[i]
						+ "\t"
						+ this.wlkDep[i]
						+ "\t"
						+ this.wlkArr[i]
						+ "\t"
						+ 0
						+ "\t"
						+ this.wlkOnRoute[i]
						+ "\t"
						+ ((otherOnRoute != null) ? (this.otherDep[i] + "\t"
								+ this.otherArr[i] + "\t" + this.otherStuck[i]
								+ "\t" + this.otherOnRoute[i]) : (0 + "\t" + 0
								+ "\t" + 0 + "\t" + 0)) + "\n");
			}
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
		double[] wlkOnRoute = new double[length];
		double[] otherOnRoute = new double[length];
		for (int i = 0; i < length; i++) {
			onRoute[i] = this.onRoute[i];
			carOnRoute[i] = this.carOnRoute[i];
			ptOnRoute[i] = this.ptOnRoute[i];
			wlkOnRoute[i] = this.wlkOnRoute[i];
			otherOnRoute[i] = (this.otherOnRoute != null) ? this.otherOnRoute[i]
					: 0;
		}
		onRouteChart.addSeries("all agents on route", category, onRoute);
		onRouteChart.addSeries("drivers on route", category, carOnRoute);
		onRouteChart.addSeries("public transit users on route", category,
				ptOnRoute);
		onRouteChart.addSeries("walkers on route", category, wlkOnRoute);
		onRouteChart.addSeries("others on route", category, otherOnRoute);
		onRouteChart.saveAsPng(filename, 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "D:/tmp/it.100/100.events.txt.gz";
		String plansFilename = "D:/tmp/it.100/100.plans.xml.gz";
		String outputFilename = "D:/tmp/it.100/onRoute.txt";
		String chartFilename = "D:/tmp/it.100/onRoute.png";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		Events events = new Events();
		OnRouteModalSplit orms = new OnRouteModalSplit("Zurich", population);
		events.addHandler(orms);
		new MatsimEventsReader(events).readFile(eventsFilename);

		orms.write(outputFilename);
		orms.writeCharts(chartFilename);

		System.out.println("-> Done!");
		System.exit(0);
	}
}
