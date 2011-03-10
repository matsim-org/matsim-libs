/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistance.java
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

/**
 *
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.TollTools;

/**
 * make time variation curve of average <code>Leg</code> distance and their sum
 * 
 * @author ychen
 * 
 */
public class LegDistance implements LinkEnterEventHandler,
		AgentArrivalEventHandler, AgentStuckEventHandler {
	private final Network network;
	private int binSize;
	private double[] legDistances;
	private int[] legCount;
	private RoadPricingScheme toll = null;
	private Population ppl = null;
	/**
	 * @param arg0
	 *            - String agentId;
	 * @param arg1
	 *            - AgentLeg agentLeg, some information about a Leg;
	 */
	private HashMap<String, Double> distances = new HashMap<String, Double>();

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.legDistances.length) {
			return this.legDistances.length - 1;
		}
		return bin;
	}

	protected void handleEventIntern(AgentEvent ae) {
		int binIdx = getBinIndex(ae.getTime());
		Double distance = this.distances.remove(ae.getPersonId().toString());
		if (distance != null) {
			this.legDistances[binIdx] += distance;
			this.legCount[binIdx]++;
		}
	}

	/**
	 * @param binSize
	 *            - the size of bin
	 * @param nofBins
	 *            - number of bins
	 * @param network
	 *            - the network, in which the simulation is located.
	 */
	public LegDistance(final int binSize, final int nofBins,
			Network network) {
		this.network = network;
		this.binSize = binSize;
		this.legDistances = new double[nofBins + 1];
		this.legCount = new int[nofBins + 1];
	}

	public LegDistance(final int binSize, Network network) {
		this(binSize, 30 * 3600 / binSize + 1, network);
	}

	public LegDistance(Network network) {
		this(300, network);
	}

	public LegDistance(Network network, RoadPricingScheme toll,
			Population ppl) {
		this(network);
		this.toll = toll;
		this.ppl = ppl;
	}

	public void handleEvent(LinkEnterEvent event) {
		Link l = this.network.getLinks().get(event.getLinkId());
		String agentId = event.getPersonId().toString();
		Double distance = this.distances.get(agentId);
		if (distance == null) {
			distance = 0.0;
		}
		if (l != null) {
			distance += l.getLength();
		} else {
			System.err.println("link with ID: \"" + event.getLinkId().toString()
					+ "\" doesn't exist in this network!");
		}
		if (toll == null)
			this.distances.put(agentId, distance);
		else {
			if (TollTools.isInRange(((PlanImpl) ppl.getPersons().get(event.getPersonId())
					.getSelectedPlan()).getFirstActivity().getLinkId(), toll)) {
				this.distances.put(agentId, distance);
			}
		}
	}

	public void handleEvent(AgentArrivalEvent event) {
		handleEventIntern(event);
	}

	public void handleEvent(AgentStuckEvent event) {
		handleEventIntern(event);
	}

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

	protected void write(final BufferedWriter bw) {
		try {
			bw
					.write("time\ttimeBin\tlegDistances (car) [m]\tn._Legs\tavg. legDistance (car) [m]\n");

			for (int i = 0; i < this.legDistances.length; i++) {
				bw.write(Time.writeTime(i * this.binSize) + "\t" + i
						* this.binSize + "\t" + this.legDistances[i] + "\t"
						+ this.legCount[i] + "\t" + this.legDistances[i]
						/ this.legCount[i] + "\n");
			}
			bw.write("----------------------------------------\n");
			double legDistSum = 0.0;
			int nLegs = 0;
			for (int i = 0; i < this.legDistances.length; i++) {
				legDistSum += this.legDistances[i];
				nLegs += this.legCount[i];
			}
			bw.write("the sum of all the legDistances (car) [m]: " + legDistSum
					+ "\n" + "the number of all the car-Legs: " + nLegs + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset(int iteration) {
		this.distances.clear();
	}

	public void writeCharts(final String filename) {
		int xsLength = this.legDistances.length + 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++) {
			xs[i] = ((double) i) * (double) this.binSize / 3600.0;
		}
		XYLineChart legDistanceSumChart = new XYLineChart("car-legDistances",
				"time", "sum of legDistances (car) [m]");
		legDistanceSumChart.addSeries(
				"sum of legDistances of all agents (car)", xs,
				this.legDistances);
		legDistanceSumChart.saveAsPng(filename + "Sum.png", 1024, 768);
		for (int i = 0; i < xsLength - 1; i++) {
			this.legDistances[i] = (this.legCount[i] == 0) ? 0.0
					: this.legDistances[i] / this.legCount[i];
		}
		XYLineChart avgLegDistanceChart = new XYLineChart(
				"average car-LegDistance", "time",
				"average legDistances (car) [m]");
		avgLegDistanceChart.addSeries(
				"average legDistance of all agents (car)", xs,
				this.legDistances);
		avgLegDistanceChart.saveAsPng(filename + "Avg.png", 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch.xml";
		final String eventsFilename = "../runs/run265/100.events.txt.gz";
		final String chartFilename = "./output/run265legDistance";
		final String outFilename = "./output/run265legDistance.txt.gz";

		// final String netFilename = "./test/yu/test/input/equil_net.xml";
		// final String eventsFilename =
		// "./test/yu/test/input/3k.100.events.txt.gz";
		// final String chartFilename = "./test/yu/test/output/3kChart.png";
		// final String outFilename = "./test/yu/test/output/3klegDist.txt.gz";

		Gbl.startMeasurement();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		// Plans population = new Plans();
		// System.out.println("-->reading plansfile: " + plansFilename);
		// new MatsimPopulationReader(population).readFile(plansFilename);
		// world.setPopulation(population);

		EventsManagerImpl events = new EventsManagerImpl();

		LegDistance legDist = new LegDistance(300, network);
		events.addHandler(legDist);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		legDist.write(outFilename);
		legDist.writeCharts(chartFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
