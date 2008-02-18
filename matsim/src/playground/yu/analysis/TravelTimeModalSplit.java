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

import org.matsim.events.AgentEvent;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

/**
 * @author ychen
 * 
 */
public class TravelTimeModalSplit implements EventHandlerAgentDepartureI,
		EventHandlerAgentArrivalI, EventHandlerAgentStuckI {
	private final NetworkLayer network;

	private final Plans plans;

	private int binSize;

	private double[] travelTimes, carTravelTimes, ptTravelTimes;

	private int[] arrCount, carArrCount, ptArrCount;

	/**
	 * @param arg0 -
	 *            String agentId
	 * @param arg1 -
	 *            Double departure time
	 */
	private HashMap<String, Double> tmpDptTimes = new HashMap<String, Double>();

	/**
	 * 
	 */
	public TravelTimeModalSplit(final int binSize, final int nofBins,
			NetworkLayer network, Plans plans) {
		this.network = network;
		this.plans = plans;
		this.binSize = binSize;
		travelTimes = new double[nofBins + 1];
		arrCount = new int[nofBins + 1];
		carTravelTimes = new double[nofBins + 1];
		ptTravelTimes = new double[nofBins + 1];
		carArrCount = new int[nofBins + 1];
		ptArrCount = new int[nofBins + 1];
	}

	public TravelTimeModalSplit(final int binSize, NetworkLayer network,
			Plans plans) {
		this(binSize, 30 * 3600 / binSize + 1, network, plans);
	}

	public void handleEvent(EventAgentDeparture event) {
		tmpDptTimes.put(event.agentId, event.time);
	}

	public void reset(int iteration) {
		tmpDptTimes.clear();
	}

	public void handleEvent(EventAgentArrival event) {
		handleEventIntern(event);
	}

	public void handleEvent(EventAgentStuck event) {
		handleEventIntern(event);
	}

	private void handleEventIntern(AgentEvent ae) {
		double time = ae.time;
		String agentId = ae.agentId;
		Double dptTime = tmpDptTimes.get(agentId);
		if (dptTime != null) {
			int binIdx = getBinIndex(time);
			double travelTime = time - dptTime;
			travelTimes[binIdx] += travelTime;
			arrCount[binIdx]++;
			tmpDptTimes.remove(agentId);
			ae.rebuild(plans, network);
			String planType = ae.agent.getSelectedPlan().getType();
			if (planType != null) {
				if (planType.equals("car")) {
					carTravelTimes[binIdx] += travelTime;
					carArrCount[binIdx]++;
				} else if (planType.equals("pt")) {
					ptTravelTimes[binIdx] += travelTime;
					ptArrCount[binIdx]++;
				}
			} else {
				carTravelTimes[binIdx] += travelTime;
				carArrCount[binIdx]++;
			}
		}
	}

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= travelTimes.length) {
			return travelTimes.length - 1;
		}
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
			for (int i = 0; i < travelTimes.length; i++) {
				bw.write(Time.writeTime(i * binSize) + "\t" + i * this.binSize
						+ "\t" + travelTimes[i] + "\t" + arrCount[i] + "\t"
						+ travelTimes[i] / (double) arrCount[i] + "\t"
						+ carTravelTimes[i] + "\t" + carArrCount[i] + "\t"
						+ carTravelTimes[i] / (double) carArrCount[i] + "\t"
						+ ptTravelTimes[i] + "\t" + ptArrCount[i] + "\t"
						+ ptTravelTimes[i] / (double) ptArrCount[i] + "\n");
			}
			bw.write("----------------------------------------\n");
			double ttSum = 0.0, carTtSum = 0.0, ptTtSum = 0.0;
			int nTrips = 0, nCarTrips = 0, nPtTrips = 0;
			for (int i = 0; i < travelTimes.length; i++) {
				ttSum += travelTimes[i];
				carTtSum += carTravelTimes[i];
				ptTtSum += ptTravelTimes[i];
				nTrips += arrCount[i];
				nCarTrips += carArrCount[i];
				nPtTrips += ptArrCount[i];
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
		int xsLength = travelTimes.length + 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++) {
			xs[i] = ((double) i) * (double) binSize / 3600.0;
		}
		XYLineChart travelTimeSumChart = new XYLineChart("TravelTimes", "time",
				"sum of TravelTimes [s]");
		travelTimeSumChart.addSeries("sum of traveltimes of all agents", xs,
				travelTimes);
		travelTimeSumChart.addSeries("sum of traveltimes of drivers", xs,
				carTravelTimes);
		travelTimeSumChart.addSeries(
				"sum of traveltime of public transit users", xs, ptTravelTimes);
		travelTimeSumChart.saveAsPng(filename + "sum.png", 1024, 768);
		for (int j = 0; j < xsLength - 1; j++) {
			travelTimes[j] = (arrCount[j] == 0) ? -1 : travelTimes[j]
					/ (double) arrCount[j];
			carTravelTimes[j] = (carArrCount[j] == 0) ? -1 : carTravelTimes[j]
					/ (double) carArrCount[j];
			ptTravelTimes[j] = (ptArrCount[j] == 0) ? -1 : ptTravelTimes[j]
					/ (double) ptArrCount[j];
		}
		XYLineChart avgTravelTimeChart = new XYLineChart(
				"average LegTravelTime", "time", "average TravelTimes [s]");
		avgTravelTimeChart.addSeries("average traveltime of all agents", xs,
				travelTimes);
		avgTravelTimeChart.addSeries("average traveltime of drivers", xs,
				carTravelTimes);
		avgTravelTimeChart
				.addSeries("average traveltime of public transit Users", xs,
						ptTravelTimes);
		avgTravelTimeChart.saveAsPng(filename + "avg.png", 1024, 768);
	}
}
