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
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.CollectionSum;
import playground.yu.utils.TollTools;

/**
 * @author mrieser
 * 
 *         Counts the number of vehicles departed, arrived or got stuck per time
 *         bin based on events.
 * @author yu This class can only be used with plansfile, in that all
 *         <code>Leg</code>s in a <code>Plan</code> muss be equiped with the
 *         same {@code Mode} {@link org.matsim.api.core.v01.TransportMode} in a
 *         day.
 */
public class EnRouteModalSplit implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, AgentStuckEventHandler {
	protected String scenario;

	protected final int binSize;
	protected Map<Id, Integer> legCounts = new HashMap<Id, Integer>();

	protected final double[] dep, arr, stuck, enRoute;

	protected final double[] carDep, carArr, carStuck, carEnRoute;

	protected final double[] ptDep, ptArr, ptEnRoute;

	protected final double[] wlkDep, wlkArr, wlkEnRoute;

	protected final double[] bikeDep, bikeArr, bikeEnRoute;

	protected double[] othersDep = null, othersArr = null, othersStuck = null,
			othersEnRoute = null;

	protected final Population plans;

	protected RoadPricingScheme toll = null;

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified
	 * number of bins.
	 * 
	 * @param binSize
	 *            The size of a time bin in seconds.
	 * @param nofBins
	 *            The number of time bins for this analysis.
	 */
	public EnRouteModalSplit(String scenario, final int binSize,
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
		// bike
		bikeArr = new double[nofBins + 1];
		bikeDep = new double[nofBins + 1];
		bikeEnRoute = new double[nofBins + 1];
		// others
		this.othersDep = new double[nofBins + 1];
		this.othersArr = new double[nofBins + 1];
		this.othersEnRoute = new double[nofBins + 1];
		this.othersStuck = new double[nofBins + 1];
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
	public EnRouteModalSplit(String scenario, final int binSize,
			Population plans) {
		this(scenario, binSize, 30 * 3600 / binSize + 1, plans);
	}

	public EnRouteModalSplit(String scenario, Population plans) {
		this(scenario, 300, plans);
	}

	public EnRouteModalSplit(String scenario, Population ppl,
			RoadPricingScheme toll) {
		this(scenario, ppl);
		this.toll = toll;
	}

	/* Implementation of eventhandler-Interfaces */

	public void handleEvent(final AgentDepartureEvent event) {
		Id id = event.getPersonId();
		Integer itg = legCounts.get(id);
		if (itg == null)
			itg = Integer.valueOf(-1);
		legCounts.put(id, itg.intValue() + 1);
		internalHandleEvent(event, this.dep, this.carDep, this.ptDep, wlkDep,
				bikeDep, this.othersDep);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		internalHandleEvent(event, this.arr, this.carArr, this.ptArr, wlkArr,
				bikeArr, this.othersArr);
	}

	public void handleEvent(final AgentStuckEvent event) {
		internalHandleEvent(event, this.stuck, this.carStuck, null, null, null,
				this.othersStuck);
	}

	protected void internalHandleEvent(AgentEvent ae, double[] allCount,
			double[] carCount, double[] ptCount, double[] wlkCount,
			double[] bikeCount, double[] othersCount) {
		int binIdx = getBinIndex(ae.getTime());
		Plan selectedPlan = plans.getPersons().get(ae.getPersonId())
				.getSelectedPlan();
		if (toll != null) {
			if (TollTools.isInRange(((PlanImpl) selectedPlan).getFirstActivity().getLinkId(),
					toll)) {
				internalCompute(binIdx, ae, selectedPlan, allCount, carCount,
						ptCount, wlkCount, bikeCount, othersCount);
			}
		} else {
			internalCompute(binIdx, ae, selectedPlan, allCount, carCount,
					ptCount, wlkCount, bikeCount, othersCount);
		}

	}

	protected void internalCompute(int binIdx, AgentEvent ae, Plan plan,
			double[] allCount, double[] carCount, double[] ptCount,
			double[] wlkCount, double[] bikeCount, double[] othersCount) {
		allCount[binIdx]++;
		Integer itg = legCounts.get(ae.getPersonId());
		if (itg != null) {
			switch (((LegImpl) plan.getPlanElements().get(2 * itg + 1)).getMode()) {
			case car:
				carCount[binIdx]++;
				break;
			case pt:
				if (ptCount != null)
					ptCount[binIdx]++;
				break;
			case walk:
				if (wlkCount != null)
					wlkCount[binIdx]++;
				break;
			case bike:
				if (bikeCount != null)
					bikeCount[binIdx]++;
				break;
			default:
				if (othersCount != null)
					othersCount[binIdx]++;
				break;
			}
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

	protected void calcOnRoute() {
		// *onRoute[0]
		this.enRoute[0] = this.dep[0] - this.arr[0] - this.stuck[0];
		this.carEnRoute[0] = this.carDep[0] - this.carArr[0] - this.carStuck[0];
		this.ptEnRoute[0] = this.ptDep[0] - this.ptArr[0];
		wlkEnRoute[0] = wlkDep[0] - wlkArr[0];
		bikeEnRoute[0] = bikeDep[0] - bikeArr[0];
		if (othersEnRoute != null)
			this.othersEnRoute[0] = this.othersDep[0] - this.othersArr[0]
					- this.othersStuck[0];
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
			this.bikeEnRoute[i] = this.bikeEnRoute[i - 1] + this.bikeDep[i]
					- this.bikeArr[i];
			if (othersEnRoute != null)
				this.othersEnRoute[i] = this.othersEnRoute[i - 1]
						+ this.othersDep[i] - this.othersArr[i]
						- this.othersStuck[i];
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
							+ "bikeDepartures\tbikeArrivals\tbikeStuck\tbikeOnRoute\t"
							+ "otherDepartures\totherArrivals\totherStuck\totherOnRoute\n");
			for (int i = 0; i < this.dep.length; i++) {
				bw
						.write(Time.writeTime(i * this.binSize)
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
								+ this.bikeDep[i]
								+ "\t"
								+ this.bikeArr[i]
								+ "\t"
								+ 0
								+ "\t"
								+ this.bikeEnRoute[i]
								+ "\t"
								+ ((othersEnRoute != null) ? (this.othersDep[i]
										+ "\t" + this.othersArr[i] + "\t"
										+ this.othersStuck[i] + "\t" + this.othersEnRoute[i])
										: (0 + "\t" + 0 + "\t" + 0 + "\t" + 0))
								+ "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	protected int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.dep.length) {
			return this.dep.length - 1;
		}
		return bin;
	}

	public void reset(final int iteration) {
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
		XYLineChart enRouteChart = new XYLineChart("Leg Histogramm - En Route",
				"time", "agents en route from " + scenario);
		enRouteChart.addSeries("drivers", xs, carEnRoute);
		if (CollectionSum.getSum(ptEnRoute) > 0)
			enRouteChart.addSeries("public transit users", xs, ptEnRoute);
		if (CollectionSum.getSum(wlkEnRoute) > 0)
			enRouteChart.addSeries("walkers", xs, wlkEnRoute);
		if (CollectionSum.getSum(bikeEnRoute) > 0)
			enRouteChart.addSeries("cyclist", xs, bikeEnRoute);
		if (CollectionSum.getSum(othersEnRoute) > 0)
			enRouteChart.addSeries("others", xs, othersEnRoute);
		enRouteChart.addSeries("all agents", xs, enRoute);
		enRouteChart.saveAsPng(filename + "enRoute.png", 1024, 768);
		// departures chart
		XYLineChart departChart = new XYLineChart(
				"Leg Histogramm - Departures", "time", "departing agents from "
						+ scenario);
		departChart.addSeries("drivers", xs, carDep);
		if (CollectionSum.getSum(ptDep) > 0)
			departChart.addSeries("public transit users", xs, ptDep);
		if (CollectionSum.getSum(wlkDep) > 0)
			departChart.addSeries("walkers", xs, wlkDep);
		if (CollectionSum.getSum(bikeDep) > 0)
			departChart.addSeries("cyclist", xs, bikeDep);
		if (CollectionSum.getSum(othersDep) > 0)
			departChart.addSeries("others", xs, othersDep);
		departChart.addSeries("all agents", xs, dep);
		departChart.saveAsPng(filename + "departures.png", 1024, 768);
		// arrivals chart
		XYLineChart arrChart = new XYLineChart("Leg Histogramm - Arrivals",
				"time", "arriving agents from " + scenario);
		arrChart.addSeries("drivers", xs, carArr);
		if (CollectionSum.getSum(ptArr) > 0)
			arrChart.addSeries("public transit users", xs, ptArr);
		if (CollectionSum.getSum(wlkArr) > 0)
			arrChart.addSeries("walkers", xs, wlkArr);
		if (CollectionSum.getSum(bikeArr) > 0)
			arrChart.addSeries("cyclist", xs, bikeArr);
		if (CollectionSum.getSum(othersArr) > 0)
			arrChart.addSeries("others", xs, othersArr);
		arrChart.addSeries("all agents", xs, arr);
		arrChart.saveAsPng(filename + "arrivals.png", 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = "../berlin data/osm/bb_osm_wip_cl.xml.gz";
		// String netFilename = "../matsim/examples/equil/network.xml";
		final String eventsFilename = "../runs-svn/run756/it.1000/1000.events.txt.gz";
		final String plansFilename = "../runs-svn/run756/it.1000/1000.plans.xml.gz";
		String outputFilename = "../matsimTests/analysis/enRoute.txt";
		String chartFilename = "../matsimTests/analysis/";
		// String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		// RoadPricingReaderXMLv1 tollReader = new
		// RoadPricingReaderXMLv1(network);
		// try {
		// tollReader.parse(tollFilename);
		// } catch (SAXException e) {
		// e.printStackTrace();
		// } catch (ParserConfigurationException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		EventsManagerImpl events = new EventsManagerImpl();
		EnRouteModalSplit orms = new EnRouteModalSplit("Berlin", population,
				null
		// tollReader.getScheme()
		);
		events.addHandler(orms);
		new MatsimEventsReader(events).readFile(eventsFilename);

		orms.write(outputFilename);
		orms.writeCharts(chartFilename);

		System.out.println("-> Done!");
		System.exit(0);
	}
}
