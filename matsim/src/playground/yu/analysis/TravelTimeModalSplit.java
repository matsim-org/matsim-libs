/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTest.java
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
 * @author ychen
 *
 */
public class TravelTimeModalSplit implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, AgentStuckEventHandler {
	// private final NetworkLayer network;

	private final Population plans;

	private final int binSize;

	private final double[] travelTimes, carTravelTimes, ptTravelTimes;

	private final int[] arrCount, carArrCount, ptArrCount;

	/**
	 * @param arg0
	 *            - String agentId
	 * @param arg1
	 *            - Double departure time
	 */
	private final HashMap<String, Double> tmpDptTimes = new HashMap<String, Double>();

	/**
	 *
	 */
	public TravelTimeModalSplit(final int binSize, final int nofBins,
	// final NetworkLayer network,
			final Population plans) {
		// this.network = network;
		this.plans = plans;
		this.binSize = binSize;
		this.travelTimes = new double[nofBins + 1];
		this.arrCount = new int[nofBins + 1];
		this.carTravelTimes = new double[nofBins + 1];
		this.ptTravelTimes = new double[nofBins + 1];
		this.carArrCount = new int[nofBins + 1];
		this.ptArrCount = new int[nofBins + 1];
	}

	public TravelTimeModalSplit(final int binSize, final NetworkLayer network,
			final Population plans) {
		this(binSize, 30 * 3600 / binSize + 1,
		// network,
				plans);
	}

	public TravelTimeModalSplit(final NetworkLayer network,
			final Population plans) {
		this(300, network, plans);
	}

	public void handleEvent(final AgentDepartureEvent event) {
		this.tmpDptTimes.put(event.agentId, event.time);
	}

	public void reset(final int iteration) {
		this.tmpDptTimes.clear();
	}

	public void handleEvent(final AgentArrivalEvent event) {
		handleEventIntern(event);
	}

	public void handleEvent(final AgentStuckEvent event) {
		handleEventIntern(event);
	}

	private void handleEventIntern(final AgentEvent ae) {
		double time = ae.time;
		String agentId = ae.agentId;
		Double dptTime = this.tmpDptTimes.get(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(time);
			double travelTime = time - dptTime;
			this.travelTimes[binIdx] += travelTime;
			this.arrCount[binIdx]++;
			this.tmpDptTimes.remove(agentId);

			if (ae.agent == null) {
				// rebuild event
				ae.agent = this.plans.getPerson(new IdImpl(ae.agentId));
			}

			// Plan.Type planType = ae.agent.getSelectedPlan().getType();
			Plan selectedplan = ae.agent.getSelectedPlan();
			if (
			// planType != null && Plan.Type.UNDEFINED != planType
			!PlanModeJudger.useUndefined(selectedplan)) {
				if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedplan)) {
					this.carTravelTimes[binIdx] += travelTime;
					this.carArrCount[binIdx]++;
				} else if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedplan)) {
					this.ptTravelTimes[binIdx] += travelTime;
					this.ptArrCount[binIdx]++;
				}
			} else {
				this.carTravelTimes[binIdx] += travelTime;
				this.carArrCount[binIdx]++;
			}
		}
	}

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.travelTimes.length)
			return this.travelTimes.length - 1;
		return bin;
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
					.write("time\ttimeBin"
							+ "\tall_traveltimes [s]\tn._arrivals/stucks\tavg. traveltimes [s]"
							+ "\tcar_traveltimes [s]\tcar_n._arrivals/stucks\tcar_avg. traveltimes [s]"
							+ "\tpt_traveltimes [s]\tpt_n._arrivals/stucks\tpt_avg. traveltimes [s]\n");
			for (int i = 0; i < this.travelTimes.length; i++)
				bw.write(Time.writeTime(i * this.binSize) + "\t" + i * this.binSize
						+ "\t" + this.travelTimes[i] + "\t" + this.arrCount[i] + "\t"
						+ this.travelTimes[i] / this.arrCount[i] + "\t"
						+ this.carTravelTimes[i] + "\t" + this.carArrCount[i] + "\t"
						+ this.carTravelTimes[i] / this.carArrCount[i] + "\t"
						+ this.ptTravelTimes[i] + "\t" + this.ptArrCount[i] + "\t"
						+ this.ptTravelTimes[i] / this.ptArrCount[i] + "\n");
			bw.write("----------------------------------------\n");
			double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0;
			int nTrips = 0, nCarTrips = 0, nPtTrips = 0;
			for (int i = 0; i < this.travelTimes.length; i++) {
				ttSum += this.travelTimes[i];
				carTtSum += this.carTravelTimes[i];
				ptTtSum += this.ptTravelTimes[i];
				nTrips += this.arrCount[i];
				nCarTrips += this.carArrCount[i];
				nPtTrips += this.ptArrCount[i];
			}

			bw
					.write("the sum of all the traveltimes [s]: "
							+ ttSum
							+ "\n"
							+ "the number of all the Trips: "
							+ nTrips
							+ "\n"
							+ "the sum of all the drivers traveltimes [s]: "
							+ carTtSum
							+ "\n"
							+ "the number of all the drivers Trips: "
							+ nCarTrips
							+ "\n"
							+ "the sum of all the public transit unsers traveltimes [s]: "
							+ ptTtSum + "\n"
							+ "the number of all the public users Trips: "
							+ nPtTrips + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeCharts(final String filename) {
		int xsLength = this.travelTimes.length + 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++)
			xs[i] = (double) i * (double) this.binSize / 3600.0;
		XYLineChart travelTimeSumChart = new XYLineChart("TravelTimes", "time",
				"sum of TravelTimes [s]");
		travelTimeSumChart.addSeries("sum of traveltimes of all agents", xs,
				this.travelTimes);
		travelTimeSumChart.addSeries("sum of traveltimes of drivers", xs,
				this.carTravelTimes);
		travelTimeSumChart.addSeries(
				"sum of traveltime of public transit users", xs, this.ptTravelTimes);
		travelTimeSumChart.saveAsPng(filename + "Sum.png", 1024, 768);
		for (int j = 0; j < xsLength - 1; j++) {
			this.travelTimes[j] = this.arrCount[j] == 0 ? -1 : this.travelTimes[j]
					/ this.arrCount[j];
			this.carTravelTimes[j] = this.carArrCount[j] == 0 ? -1 : this.carTravelTimes[j]
					/ this.carArrCount[j];
			this.ptTravelTimes[j] = this.ptArrCount[j] == 0 ? -1 : this.ptTravelTimes[j]
					/ this.ptArrCount[j];
		}
		XYLineChart avgTravelTimeChart = new XYLineChart(
				"average LegTravelTime", "time", "average TravelTimes [s]");
		avgTravelTimeChart.addSeries("average traveltime of all agents", xs,
				this.travelTimes);
		avgTravelTimeChart.addSeries("average traveltime of drivers", xs,
				this.carTravelTimes);
		avgTravelTimeChart
				.addSeries("average traveltime of public transit Users", xs,
						this.ptTravelTimes);
		avgTravelTimeChart.saveAsPng(filename + "Avg.png", 1024, 768);
	}

	public static void main(final String[] args) {
		final String netFilename = "../psrc/network/psrc-wo-3212.xml.gz";
		final String plansFilename = "../runs/run668/it.1500/1500.plans.xml.gz";
		final String eventsFilename = "../runs/run668/it.1500/1500.analysis/6760.txt";
		final String chartFilename = "../runs/run668/it.1500/1500.analysis/6760.travelTime";
		final String outFilename = "../runs/run668/it.1500/1500.analysis/6760.travelTime.txt.gz";

		// final String netFilename = "./test/yu/test/input/equil_net.xml";
		// final String plansFilename =
		// "./test/yu/test/input/3k.100.plans.xml.gz";
		// final String eventsFilename =
		// "./test/yu/test/input/3k.100.events.txt.gz";
		// // final String volumeTestFilename =
		// "./test/yu/test/output/3kVolumeTest.txt.gz";
		// final String chartFilename = "./test/yu/test/output/3kChart.png";
		// final String outFilename = "./test/yu/test/output/3ktt.txt.gz";

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		Events events = new Events();

		TravelTimeModalSplit ttms = new TravelTimeModalSplit(300, network,
				population);
		events.addHandler(ttms);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		ttms.write(outFilename);
		ttms.writeCharts(chartFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
