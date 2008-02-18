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

import org.matsim.events.AgentEvent;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

/**
 * @author ychen
 * 
 */
public class LegDistance implements EventHandlerLinkEnterI,
		EventHandlerAgentArrivalI, EventHandlerAgentStuckI {
	private final NetworkLayer network;
	private int binSize;
	private double[] legDistances;
	private int[] legCount;

	/**
	 * @param arg0 -
	 *            String agentId;
	 * @param arg1 -
	 *            AgentLeg agentLeg, some information about a Leg;
	 */
	private HashMap<String, Double> distances = new HashMap<String, Double>();

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= legDistances.length) {
			return legDistances.length - 1;
		}
		return bin;
	}

	protected void handleEventIntern(AgentEvent ae) {
		int binIdx = getBinIndex(ae.time);
		Double distance = distances.remove(ae.agentId);
		if (distance != null) {
			legDistances[binIdx] += distance;
			legCount[binIdx]++;
		}
	}

	/**
	 * @param binSize -
	 *            the size of bin
	 * @param nofBins -
	 *            number of bins
	 * @param network -
	 *            the network, in which the simulation is located.
	 */
	public LegDistance(final int binSize, final int nofBins,
			NetworkLayer network) {
		this.network = network;
		this.binSize = binSize;
		legDistances = new double[nofBins + 1];
		legCount = new int[nofBins + 1];
	}

	public LegDistance(final int binSize, NetworkLayer network) {
		this(binSize, 30 * 3600 / binSize + 1, network);
	}

	public void handleEvent(EventLinkEnter event) {
		String linkId = event.linkId;
		Link l = network.getLink(linkId);
		String agentId = event.agentId;
		Double distance = distances.get(agentId);
		if (distance == null) {
			distance = new Double(0.0);
		}
		if (l != null) {
			distance += l.getLength();
		} else {
			System.err.println("link with ID: \"" + linkId
					+ "\" doesn't exist in this network!");
		}
		distances.put(agentId, distance);
	}

	public void handleEvent(EventAgentArrival event) {
		handleEventIntern(event);
	}

	public void handleEvent(EventAgentStuck event) {
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
					.write("time\ttimeBin\tlegistances [m]\tn._Legs\tavg. legDistance [m]\n");

			for (int i = 0; i < legDistances.length; i++) {
				bw.write(Time.writeTime(i * binSize) + "\t" + i * this.binSize
						+ "\t" + legDistances[i] + "\t" + legCount[i] + "\t"
						+ legDistances[i] / (double) legCount[i] + "\n");
			}
			bw.write("----------------------------------------\n");
			double legDistSum = 0.0;
			int nLegs = 0;
			for (int i = 0; i < legDistances.length; i++) {
				legDistSum += legDistances[i];
				nLegs += legCount[i];
			}
			bw.write("the sum of all the legDistances [m]: " + legDistSum
					+ "\n" + "the number of all the Legs: " + nLegs + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset(int iteration) {
		distances.clear();
	}

	public void writeCharts(final String filename) {
		int xsLength = legDistances.length + 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++) {
			xs[i] = ((double) i) * (double) binSize / 3600.0;
		}
		XYLineChart legDistanceSumChart = new XYLineChart("legDistances",
				"time", "sum of legDistances [m]");
		legDistanceSumChart.addSeries("sum of legDistances of all agents", xs,
				legDistances);
		legDistanceSumChart.saveAsPng(filename + "sum.png", 1024, 768);
		for (int i = 0; i < xsLength - 1; i++) {
			legDistances[i] = (legCount[i] == 0) ? 0.0 : legDistances[i]
					/ (double) legCount[i];
		}
		XYLineChart avgLegDistanceChart = new XYLineChart(
				"average LegDistance", "time", "average legDistances [m]");
		avgLegDistanceChart.addSeries("average legDistance of all agents", xs,
				legDistances);
		avgLegDistanceChart.saveAsPng(filename + "avg.png", 1024, 768);
	}
}
