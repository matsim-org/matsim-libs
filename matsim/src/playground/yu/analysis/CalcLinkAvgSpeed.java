/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLinkAvgSpeed.java
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
import java.util.Set;

import org.matsim.basic.v01.Id;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class CalcLinkAvgSpeed extends CalcNetAvgSpeed {
	/**
	 * @param arg0 -
	 *            a String linkId
	 * @param arg1 -
	 *            a SpeedCounter object
	 */
	private final HashMap<String, SpeedCounter> speedCounters = new HashMap<String, SpeedCounter>();
	private Set<Link> interestLinks = null;
	private final int binSize, nofBins;
	private final double[] speeds;
	private final int[] speedsCount;

	/**
	 * @param network
	 * @param binSize -
	 *            size of a timeBin e.g. (300s, 3600s)
	 * @param nofBins -
	 *            number of bins
	 */
	public CalcLinkAvgSpeed(final NetworkLayer network, final int binSize,
			final int nofBins) {
		super(network);
		this.binSize = binSize;
		this.nofBins = nofBins;
		speeds = new double[nofBins - 1];
		speedsCount = new int[nofBins - 1];
	}

	public CalcLinkAvgSpeed(final NetworkLayer network, final int binSize) {
		this(network, binSize, 30 * 3600 / binSize + 1);
	}

	public CalcLinkAvgSpeed(final NetworkLayer network) {
		this(network, 3600);
	}

	/**
	 * support the speed calculation only for the links in a circle area
	 * 
	 * @param network
	 * @param x-abscissa
	 *            of the center of the circle area
	 * @param y-vertical
	 *            coordinates of the center of the circle area
	 * @param radius-radius
	 *            of the circle
	 */
	public CalcLinkAvgSpeed(final NetworkLayer network, final double x,
			final double y, final double radius) {
		this(network);
		interestLinks = new NetworkLinksInCircle(network)
				.getLinks(x, y, radius);
	}

	public static class SpeedCounter {
		private final double[] lengthSum;
		private final double[] timeSum;
		/**
		 * @param arg0 -
		 *            agentId;
		 * @param arg1 -
		 *            enterTime;
		 */
		private final HashMap<String, Double> enterTimes = new HashMap<String, Double>();

		/**
		 * @param nofBins -
		 *            number of bins.
		 */
		public SpeedCounter(final int nofBins) {
			lengthSum = new double[nofBins];
			timeSum = new double[nofBins];
		}

		public void lengthSumAppend(final int timeBin, final double length) {
			lengthSum[timeBin] += length;
		}

		public void timeSumAppend(final int timeBin, final double time) {
			timeSum[timeBin] += time;
		}

		public double getSpeed(final int timeBin) {
			return timeSum[timeBin] != 0.0 ? lengthSum[timeBin]
					/ timeSum[timeBin] * 3.6 : 0.0;
		}

		public void setTmpEnterTime(final String agentId,
				final double tmpEnterTime) {
			enterTimes.put(agentId, tmpEnterTime);
		}

		public Double removeTmpEnterTime(final String agentId) {
			return enterTimes.remove(agentId);
		}

		public boolean containsTmpEnterTime(final String agentId) {
			return enterTimes.containsKey(agentId);
		}
	}

	public double getAvgSpeed(final Id linkId, final double time) {
		SpeedCounter sc = speedCounters.get(linkId.toString());
		return sc != null ? sc.getSpeed(getBinIdx(time)) : 0.0;
	}

	@Override
	public void handleEvent(final EventAgentArrival arrival) {
		SpeedCounter sc = speedCounters.get(arrival.linkId);
		if (sc != null)
			sc.removeTmpEnterTime(arrival.agentId);
	}

	@Override
	public void handleEvent(final EventLinkEnter enter) {
		String linkId = enter.linkId;
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc == null)
			sc = new SpeedCounter(nofBins);
		sc.setTmpEnterTime(enter.agentId, enter.time);
		speedCounters.put(linkId, sc);
	}

	@Override
	public void handleEvent(final EventLinkLeave leave) {
		double time = leave.time;
		int timeBin = getBinIdx(time);
		String linkId = leave.linkId;
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc != null) {
			Double enterTime = sc.removeTmpEnterTime(leave.agentId);
			if (enterTime != null) {
				Link l = leave.link;
				if (l == null)
					l = network.getLink(linkId);
				if (l != null) {
					sc.lengthSumAppend(timeBin, l.getLength());
					sc.timeSumAppend(timeBin, time - enterTime);
				}
			}
		}
	}

	private int getBinIdx(final double time) {
		int bin = (int) (time / binSize);
		if (bin >= nofBins)
			return nofBins - 1;
		return bin;
	}

	@Override
	public void reset(final int iteration) {
		speedCounters.clear();
	}

	/**
	 * @param filename
	 *            outputfilename (.../*.txt)
	 */
	@SuppressWarnings("unchecked")
	public void write(final String filename) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(filename);
			StringBuffer head = new StringBuffer("avg. Speed\nlinkId\tCapacity");
			for (int i = 0; i < nofBins - 1; i++)
				head.append("\tH" + Integer.toString(i) + "-"
						+ Integer.toString(i + 1));
			head.append("\n");
			out.write(head.toString());
			out.flush();

			for (Link l : interestLinks == null ? network.getLinks().values()
					: interestLinks) {
				Id linkId = l.getId();
				StringBuffer line = new StringBuffer(
						linkId.toString()
								+ "\t"
								+ l
										.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME));
				for (int j = 0; j < nofBins - 1; j++) {
					double speed = getAvgSpeed(linkId, (double) j * 3600);
					line.append("\t" + speed);
					if (speed > 0) {
						speeds[j] += speed;
						speedsCount[j]++;
					}
				}
				line.append("\n");
				out.write(line.toString());
				out.flush();
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeChart(final String chartFilename) {
		int xsLength = nofBins - 1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++)
			xs[i] = (double) i * (double) binSize / 3600.0;
		double[] ySpeed = new double[xsLength];
		for (int i = 0; i < xsLength; i++)
			if (speedsCount[i] > 0)
				ySpeed[i] = speeds[i] / speedsCount[i];
		XYLineChart avgSpeedChart = new XYLineChart("avg. speed in cityarea",
				"time", "avg. speed [km/h]");
		avgSpeedChart.addSeries("avg. speed of all agents", xs, ySpeed);
		avgSpeedChart.saveAsPng(chartFilename, 1024, 768);
	}

	/**
	 * @param args
	 */
	@SuppressWarnings( { "unchecked", "unchecked" })
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch/network/ivtch-osm-wu-flama.xml";
		// final String eventsFilename =
		// "./test/yu/test/input/run265opt100.events.txt.gz";
		final String eventsFilename = "../runs/run467/500.events.txt.gz";
		final String outputFilename = "../runs/run467/AvgSpeed.txt.gz";
		final String chartFilename = "../runs/run467/cityAreaAvgSpeed.png";

		Gbl.createConfig(null);
		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Events events = new Events();

		CalcLinkAvgSpeed clas = new CalcLinkAvgSpeed(network, 682845.0,
				247388.0, 2000.0);
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		clas.write(outputFilename);
		clas.writeChart(chartFilename);

		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
