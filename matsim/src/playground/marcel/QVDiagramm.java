/* *********************************************************************** *
 * project: org.matsim.*
 * QVDiagramm.java
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

package playground.marcel;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.charts.XYScatterChart;

/**
 * Generates a "Q-V-Diagramm" (traffic flow versus speed) for one link, based
 * on data from the simulation. The average speed of vehicles within a certain
 * time bin (default: 5min) and the average flow rate within the same time bin
 * are plotted into a XY Scatter Chart.
 *
 * @author mrieser
 */
public class QVDiagramm implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler {

	/** The id of the link we're interesetd in. */
	private final String linkId;

	/** The length of the link. */
	private final double linkLength;

	/** Stores the times agents entered the link (TreeMap<AgentId, EnterTime>). */
	private final TreeMap<String, Double> agents = new TreeMap<String, Double>(); // agentId, enterTime

	/** Stores the traffic flow values to be plotted. */
	private final ArrayList<Double> qValues = new ArrayList<Double>();

	/** Stores the speed values to be plotted. */
	private final ArrayList<Double> vValues = new ArrayList<Double>();

	/** The size of the time window over which we aggregate data. For each time bin, there will be one point in the chart. */
	private int binSize = 300;

	/** The number of vehicles leaving the link in the current time bin, used to calculate the traffic flow. */
	private int flowCount = 0;

	/** The index of the current time bin. Used to recognize when a new time bin starts, so we can plot the "old" data. */
	private int timeBinIndex = 0;

	/** The number of vehicles in this time bin whose speed we know. This may be different from {@link #flowCount}
	 * because we may not know the speed of all vehicles leaving the link -- it is missing for example for vehicles
	 * starting at this link. */
	private int speedCnt = 0;

	/** The sum of all speeds of vehicles in this time bin. Used to calculate the average speed in this time bin. */
	private double speedSum = 0;

	public QVDiagramm(final NetworkLayer network, final String linkId) {
		this.linkId = linkId;
		Link link = network.getLink(new IdImpl(linkId));
		this.linkLength = link.getLength();
	}

	public void handleEvent(final LinkEnterEvent event) {
		// Store the enter time of this agent if it's on the link we're interested in.
		if (this.linkId.equals(event.linkId)) {
			this.agents.put(event.agentId, Double.valueOf(event.time));
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// delete the enter time, because the agent won't leave this link for a while now...
		if (this.linkId.equals(event.linkId)) {
			this.agents.remove(event.agentId);
		}
	}

	public void handleEvent(final LinkLeaveEvent event) {
		if (this.linkId.equals(event.linkId)) {
			if ((int)event.time / this.binSize != this.timeBinIndex) {
				// the event is from a new time bin, finish the old one.
				this.updateGraphValues();
				this.timeBinIndex = (int)event.time / this.binSize;
			}
			// we have one more vehicle leaving this link
			this.flowCount++;
			Double enterTime = this.agents.remove(event.agentId);
			if (enterTime == null) return;
			// we have the enter time of this vehicle, calculate its speed.
			double traveltime = event.time - enterTime.doubleValue();
			double speed = this.linkLength / traveltime * 3.6; // the speed in km/h
			this.speedCnt++;
			this.speedSum += speed;
		}
	}

	public void reset(final int iteration) {
		this.agents.clear();
		this.qValues.clear();
		this.vValues.clear();
		this.flowCount = 0;
		this.speedCnt = 0;
		this.speedSum = 0.0;
		this.timeBinIndex = 0;
	}

	private void updateGraphValues() {
		if (this.speedCnt > 0) {
//			System.out.println("timebin: " + this.timeBinIndex);
//			System.out.println("speedSum: " + this.speedSum);
//			System.out.println("speedCnt: " + this.speedCnt);
//			System.out.println("flowCount: " + this.flowCount);
			this.vValues.add(this.speedSum / this.speedCnt);
			this.qValues.add(this.flowCount * (3600.0 / this.binSize));
			this.speedCnt = 0;
			this.speedSum = 0.0;
			this.flowCount = 0;
		}
		this.timeBinIndex++;
	}

	public void writeGraph(final String filename) {
		double[] speeds = new double[this.vValues.size()];
		double[] flows = new double[this.qValues.size()];
		for (int i = 0; i < speeds.length; i++) {
			speeds[i] = this.vValues.get(i).doubleValue();
			flows[i] = this.qValues.get(i).doubleValue();
		}
		XYScatterChart chart = new XYScatterChart("link " + this.linkId, "q", "v");
		chart.addSeries("link " + this.linkId, flows, speeds);
		speeds = new double[]{ 0.0, 150.0 };
		flows = new double[] {0.0, 2000.0 };
		chart.addSeries("boundaries", flows, speeds);
		chart.saveAsPng(filename, 800, 600);
	}

}
