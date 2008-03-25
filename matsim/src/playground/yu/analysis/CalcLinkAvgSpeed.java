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

import org.matsim.config.Config;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.identifiers.IdI;
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
	private HashMap<String, SpeedCounter> speedCounters = new HashMap<String, SpeedCounter>();
	private Set<Link> interestLinks = null;
	private final int binSize, nofBins;
	private double[] speeds;
	private int[] speedsCount;

	/**
	 * @param network
	 * @param binSize -
	 *            size of a timeBin e.g. (300s, 3600s)
	 * @param nofBins -
	 *            number of bins
	 */
	public CalcLinkAvgSpeed(NetworkLayer network, final int binSize,
			final int nofBins) {
		super(network);
		this.binSize = binSize;
		this.nofBins = nofBins;
		this.speeds = new double[nofBins - 1];
		this.speedsCount = new int[nofBins - 1];
	}

	public CalcLinkAvgSpeed(NetworkLayer network, final int binSize) {
		this(network, binSize, 30 * 3600 / binSize + 1);
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
	public CalcLinkAvgSpeed(NetworkLayer network, double x, double y,
			double radius) {
		this(network, 3600);
		this.interestLinks = new NetworkLinksInCircle(network)
				.getLinks(x, y, radius);
	}

	public static class SpeedCounter {
		private double[] lengthSum;
		private double[] timeSum;
		/**
		 * @param arg0 -
		 *            agentId;
		 * @param arg1 -
		 *            enterTime;
		 */
		private HashMap<String, Double> enterTimes = new HashMap<String, Double>();

		/**
		 * @param nofBins -
		 *            number of bins.
		 */
		public SpeedCounter(final int nofBins) {
			this.lengthSum = new double[nofBins];
			this.timeSum = new double[nofBins];
		}

		public void lengthSumAppend(int timeBin, double length) {
			this.lengthSum[timeBin] += length;
		}

		public void timeSumAppend(int timeBin, double time) {
			this.timeSum[timeBin] += time;
		}

		public double getSpeed(int timeBin) {
			return (this.timeSum[timeBin] != 0.0) ? this.lengthSum[timeBin]
					/ this.timeSum[timeBin] * 3.6 : 0.0;
		}

		public void setTmpEnterTime(String agentId, double tmpEnterTime) {
			this.enterTimes.put(agentId, tmpEnterTime);
		}

		public Double removeTmpEnterTime(String agentId) {
			return this.enterTimes.remove(agentId);
		}

		public boolean containsTmpEnterTime(String agentId) {
			return this.enterTimes.containsKey(agentId);
		}
	}

	public double getAvgSpeed(IdI linkId, double time) {
		SpeedCounter sc = this.speedCounters.get(linkId.toString());
		return (sc != null) ? sc.getSpeed(getBinIdx(time)) : 0.0;
	}

	@Override
	public void handleEvent(EventAgentArrival arrival) {
		SpeedCounter sc = this.speedCounters.get(arrival.linkId);
		if (sc != null) {
			sc.removeTmpEnterTime(arrival.agentId);
		}
	}

	@Override
	public void handleEvent(EventLinkEnter enter) {
		String linkId = enter.linkId;
		SpeedCounter sc = this.speedCounters.get(linkId);
		if (sc == null) {
			sc = new SpeedCounter(this.nofBins);
		}
		sc.setTmpEnterTime(enter.agentId, enter.time);
		this.speedCounters.put(linkId, sc);
	}

	@Override
	public void handleEvent(EventLinkLeave leave) {
		double time = leave.time;
		int timeBin = getBinIdx(time);
		String linkId = leave.linkId;
		SpeedCounter sc = this.speedCounters.get(linkId);
		if (sc != null) {
			Double enterTime = sc.removeTmpEnterTime(leave.agentId);
			if (enterTime != null) {
				Link l = leave.link;
				if (l == null) {
					l = this.network.getLink(linkId);
				}
				if (l != null) {
					sc.lengthSumAppend(timeBin, l.getLength());
					sc.timeSumAppend(timeBin, time - enterTime);
				}
			}
		}
	}

	private int getBinIdx(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins - 1;
		}
		return bin;
	}

	@Override
	public void reset(int iteration) {
		this.speedCounters.clear();
	}

	/**
	 * @param filename
	 *            outputfilename (.../*.txt)
	 */
	@SuppressWarnings("unchecked")
	public void write(String filename) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(filename);
			StringBuffer head = new StringBuffer("avg. Speed\nlinkId\tCapacity");
			for (int i = 0; i < this.nofBins - 1; i++) {
				head.append("\tH" + Integer.toString(i) + "-"
						+ Integer.toString(i + 1));
			}
			head.append("\n");
			out.write(head.toString());
			out.flush();

			for (Link l : ((this.interestLinks == null) ? (this.network.getLinks())
					.values() : this.interestLinks)) {
				IdI linkId = l.getId();
				StringBuffer line = new StringBuffer(linkId.toString() + "\t"
						+ l.getCapacity());
				for (int j = 0; j < this.nofBins - 1; j++) {
					double speed = getAvgSpeed(linkId, (double) j * 3600);
					line.append("\t" + speed);
					if (speed > 0) {
						this.speeds[j] += speed;
						this.speedsCount[j]++;
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

	public void writeChart(String chartFilename) {
		int xsLength =this.nofBins-1;
		double[] xs = new double[xsLength];
		for (int i = 0; i < xsLength; i++) {
			xs[i] = ((double) i) * (double) this.binSize / 3600.0;
		}
		double[] ySpeed = new double[xsLength];
		for (int i = 0; i <xsLength; i++) {
			if (this.speedsCount[i] > 0) {
				ySpeed[i] = this.speeds[i] / this.speedsCount[i];
			}
		}
		XYLineChart avgSpeedChart = new XYLineChart("avg. speed in cityarea",
				"time", "avg. speed [km/h]");
		avgSpeedChart
				.addSeries("sum of legDistances of all agents", xs, ySpeed);
		avgSpeedChart.saveAsPng(chartFilename, 1024, 768);
	}

	/**
	 * @param args
	 */
	@SuppressWarnings( { "unchecked", "unchecked" })
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "./test/yu/ivtch/input/network.xml";
		// final String eventsFilename =
		// "./test/yu/test/input/run265opt100.events.txt.gz";
		final String eventsFilename = "../runs/run275/100.events.txt.gz";
		final String outputFilename = "./test/yu/test/output/run275AvgSpeed.txt.gz";
		final String chartFilename = "./test/yu/test/output/run275avgSpeed.png";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);
		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
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
