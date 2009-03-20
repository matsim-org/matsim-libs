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

import javax.xml.parsers.ParserConfigurationException;

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
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;
import org.xml.sax.SAXException;

import playground.yu.utils.TollTools;

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
	private final double[] dep, arr, stuck, enRoute;
	private final double[] carDep, carArr, carStuck, carEnRoute;
	private final double[] ptDep, ptArr, ptEnRoute;
	private final double[] wlkDep, wlkArr, wlkEnRoute;
	private double[] otherDep = null, otherArr = null, otherStuck = null,
			otherEnRoute = null;

	private final Population plans;
	private RoadPricingScheme toll = null;

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
		this.dep = new double[nofBins + 1]; // +1 for all times out of our
		// range
		// total
		this.arr = new double[nofBins + 1];
		this.stuck = new double[nofBins + 1];
		this.enRoute = new double[nofBins + 1];
		// car
		this.carArr = new double[nofBins + 1];
		this.carDep = new double[nofBins + 1];
		this.carStuck = new double[nofBins + 1];
		this.carEnRoute = new double[nofBins + 1];
		// pt
		this.ptArr = new double[nofBins + 1];
		this.ptDep = new double[nofBins + 1];
		this.ptEnRoute = new double[nofBins + 1];
		// walk
		wlkArr = new double[nofBins + 1];
		wlkDep = new double[nofBins + 1];
		wlkEnRoute = new double[nofBins + 1];

		if (this.scenario.equals("Zurich")
				|| this.scenario.equals("Kanton_Zurich")) {
			// through traffic
			this.otherDep = new double[nofBins + 1];
			this.otherArr = new double[nofBins + 1];
			this.otherEnRoute = new double[nofBins + 1];
			this.otherStuck = new double[nofBins + 1];
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

	public OnRouteModalSplit(String scenario, Population ppl,
			RoadPricingScheme toll) {
		this(scenario, ppl);
		this.toll = toll;
	}

	/* Implementation of eventhandler-Interfaces */

	public void handleEvent(final AgentDepartureEvent event) {
		internalHandleEvent(event, this.dep, this.carDep, this.ptDep, wlkDep,
				this.otherDep);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		internalHandleEvent(event, this.arr, this.carArr, this.ptArr, wlkArr,
				this.otherArr);
	}

	public void handleEvent(final AgentStuckEvent event) {
		internalHandleEvent(event, this.stuck, this.carStuck, null, null,
				this.otherStuck);
	}

	private void internalHandleEvent(AgentEvent ae, double[] allCount,
			double[] carCount, double[] ptCount, double[] wlkCount,
			double[] otherCount) {
		int binIdx = getBinIndex(ae.getTime());
		Plan selectedPlan = plans.getPersons().get(ae.getPersonId())
				.getSelectedPlan();
		if (toll != null) {
			if (TollTools.isInRange(selectedPlan.getFirstActivity().getLink(),
					toll)) {
				internalCompute(binIdx, ae, selectedPlan, allCount, carCount,
						ptCount, wlkCount, otherCount);
			}
		} else {
			internalCompute(binIdx, ae, selectedPlan, allCount, carCount,
					ptCount, wlkCount, otherCount);
		}

	}

	private void internalCompute(int binIdx, AgentEvent ae, Plan plan,
			double[] allCount, double[] carCount, double[] ptCount,
			double[] wlkCount, double[] otherCount) {
		allCount[binIdx]++;
		if (otherCount != null)
			if (Integer.parseInt(ae.getPersonId().toString()) > 1000000000)
				otherCount[binIdx]++;
			else {
				if (PlanModeJudger.useCar(plan))
					carCount[binIdx]++;
				else if (PlanModeJudger.usePt(plan)) {
					if (ptCount != null)
						ptCount[binIdx]++;
				} else if (PlanModeJudger.useWalk(plan))
					if (wlkCount != null)
						wlkCount[binIdx]++;
			}
		else {
			if (PlanModeJudger.useCar(plan))
				carCount[binIdx]++;
			else if (PlanModeJudger.usePt(plan)) {
				if (ptCount != null)
					ptCount[binIdx]++;
			} else if (PlanModeJudger.useWalk(plan))
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
		this.enRoute[0] = this.dep[0] - this.arr[0] - this.stuck[0];
		this.carEnRoute[0] = this.carDep[0] - this.carArr[0] - this.carStuck[0];
		this.ptEnRoute[0] = this.ptDep[0] - this.ptArr[0];
		wlkEnRoute[0] = wlkDep[0] - wlkArr[0];
		if (otherEnRoute != null)
			this.otherEnRoute[0] = this.otherDep[0] - this.otherArr[0]
					- this.otherStuck[0];
		// *onRoute[i]
		for (int i = 1; i < this.dep.length; i++) {
			this.enRoute[i] = this.enRoute[i - 1] + this.dep[i] - this.arr[i]
					- this.stuck[i];
			this.carEnRoute[i] = this.carEnRoute[i - 1] + this.carDep[i]
					- this.carArr[i] - this.carStuck[i];
			this.ptEnRoute[i] = this.ptEnRoute[i - 1] + this.ptDep[i]
					- this.ptArr[i];
			this.wlkEnRoute[i] = this.wlkEnRoute[i - 1] + this.wlkDep[i]
					- this.wlkArr[i];
			if (otherEnRoute != null)
				this.otherEnRoute[i] = this.otherEnRoute[i - 1]
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
						+ this.enRoute[i]
						+ "\t"
						+ this.carDep[i]
						+ "\t"
						+ this.carArr[i]
						+ "\t"
						+ this.carStuck[i]
						+ "\t"
						+ this.carEnRoute[i]
						+ "\t"
						+ this.ptDep[i]
						+ "\t"
						+ this.ptArr[i]
						+ "\t"
						+ 0
						+ "\t"
						+ this.ptEnRoute[i]
						+ "\t"
						+ this.wlkDep[i]
						+ "\t"
						+ this.wlkArr[i]
						+ "\t"
						+ 0
						+ "\t"
						+ this.wlkEnRoute[i]
						+ "\t"
						+ ((otherEnRoute != null) ? (this.otherDep[i] + "\t"
								+ this.otherArr[i] + "\t" + this.otherStuck[i]
								+ "\t" + this.otherEnRoute[i]) : (0 + "\t" + 0
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
		int length = enRoute.length;
		double[] xs = new double[length];
		for (int j = 0; j < xs.length; j++) {
			xs[j] = ((double) j) * (double) this.binSize / 3600.0;
		}
		// enRoute chart
		XYLineChart enRouteChart = new XYLineChart("Leg Histogramm", "time",
				"agents en route from " + scenario);
		enRouteChart.addSeries("drivers", xs, carEnRoute);
		enRouteChart.addSeries("public transit users", xs, ptEnRoute);
		enRouteChart.addSeries("walkers", xs, wlkEnRoute);
		if (otherEnRoute != null)
			enRouteChart.addSeries("others", xs, otherEnRoute);
		enRouteChart.addSeries("all agents", xs, enRoute);
		enRouteChart.saveAsPng(filename + "enRoute.png", 1024, 768);
		// departures chart
		XYLineChart departChart = new XYLineChart("Leg Histogramm", "time",
				"departing agents from " + scenario);
		departChart.addSeries("drivers", xs, carDep);
		departChart.addSeries("public transit users", xs, ptDep);
		departChart.addSeries("walkers", xs, wlkDep);
		if (otherDep != null)
			departChart.addSeries("others", xs, otherDep);
		departChart.addSeries("all agents", xs, dep);
		departChart.saveAsPng(filename + "departures.png", 1024, 768);
		// arrivals chart
		XYLineChart arrChart = new XYLineChart("Leg Histogramm", "time",
				"arriving agents from " + scenario);
		arrChart.addSeries("drivers", xs, carArr);
		arrChart.addSeries("public transit users", xs, ptArr);
		arrChart.addSeries("walkers", xs, wlkArr);
		if (otherArr != null)
			arrChart.addSeries("others", xs, otherArr);
		arrChart.addSeries("all agents", xs, arr);
		arrChart.saveAsPng(filename + "arrivals.png", 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../runs_SVN/run684/it.1000/1000.events.txt.gz";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/analysis/enRoute.txt";
		String chartFilename = "../matsimTests/analysis/";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(network);
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Events events = new Events();
		OnRouteModalSplit orms = new OnRouteModalSplit("Kanton_Zurich",
				population
		// ,tollReader.getScheme()
		);
		events.addHandler(orms);
		new MatsimEventsReader(events).readFile(eventsFilename);

		orms.write(outputFilename);
		orms.writeCharts(chartFilename);

		System.out.println("-> Done!");
		System.exit(0);
	}
}
