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
	 *            linkId
	 * @param arg1 -
	 *            a SpeedCounter
	 */
	private HashMap<String, SpeedCounter> speedCounters = new HashMap<String, SpeedCounter>();
	private Set<Link> interestLinks = null;
	private final int binSize, nofBins;

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
	}

	public CalcLinkAvgSpeed(NetworkLayer network, final int binSize) {
		this(network, binSize, 30 * 3600 / binSize + 1);
	}

	public CalcLinkAvgSpeed(NetworkLayer network, double x, double y,
			double radius) {
		this(network, 3600);
		interestLinks = new NetworkLinksInCircle(network)
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
			lengthSum = new double[nofBins];
			timeSum = new double[nofBins];
		}

		public void lengthSumAppend(int timeBin, double length) {
			lengthSum[timeBin] += length;
		}

		public void timeSumAppend(int timeBin, double time) {
			timeSum[timeBin] += time;
		}

		public double getSpeed(int timeBin) {
			return (timeSum[timeBin] != 0.0) ? lengthSum[timeBin]
					/ timeSum[timeBin] * 3.6 : 0.0;
		}

		public void setTmpEnterTime(String agentId, double tmpEnterTime) {
			enterTimes.put(agentId, tmpEnterTime);
		}

		public Double removeTmpEnterTime(String agentId) {
			return enterTimes.remove(agentId);
		}

		public boolean containsTmpEnterTime(String agentId) {
			return enterTimes.containsKey(agentId);
		}
	}

	public double getAvgSpeed(IdI linkId, double time) {
		SpeedCounter sc = speedCounters.get(linkId.toString());
		return (sc != null) ? sc.getSpeed(getBinIdx(time)) : 0.0;
	}

	public void handleEvent(EventAgentArrival arrival) {
		SpeedCounter sc = speedCounters.get(arrival.linkId);
		if (sc != null) {
			sc.removeTmpEnterTime(arrival.agentId);
		}
	}

	public void handleEvent(EventLinkEnter enter) {
		String linkId = enter.linkId;
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc == null) {
			sc = new SpeedCounter(nofBins);
		}
		sc.setTmpEnterTime(enter.agentId, enter.time);
		speedCounters.put(linkId, sc);
	}

	public void handleEvent(EventLinkLeave leave) {
		double time = leave.time;
		int timeBin = getBinIdx(time);
		String linkId = leave.linkId;
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc != null) {
			Double enterTime = sc.removeTmpEnterTime(leave.agentId);
			if (enterTime != null) {
				Link l = leave.link;
				if (l == null) {
					l = network.getLink(linkId);
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
		if (bin >= nofBins) {
			return nofBins - 1;
		}
		return bin;
	}

	public void reset(int iteration) {
		speedCounters.clear();
	}

	/**
	 * @param filename
	 *            outputfilename (.../*.txt)
	 */
	@SuppressWarnings("unchecked")
	public void write(String filename) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(filename);
			StringBuffer head = new StringBuffer(
					"avg. Speed\nlinkId\tCapacity\tSimulationFlowCapacity");
			for (int i = 0; i < 30; i++) {
				head.append("\tH" + Integer.toString(i) + "-"
						+ Integer.toString(i + 1));
			}
			head.append("\n");
			out.write(head.toString());
			out.flush();

			for (Link l : ((interestLinks == null) ? (network.getLinks())
					.values() : interestLinks)) {
				IdI linkId = l.getId();
				StringBuffer line = new StringBuffer(linkId.toString() + "\t"
						+ l.getCapacity());
				for (int j = 0; j < 30; j++) {
					line.append("\t" + getAvgSpeed(linkId, (double) j * 3600));
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

	/**
	 * @param args
	 */
	@SuppressWarnings( { "unchecked", "unchecked" })
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "./test/yu/ivtch/input/network.xml";
//		final String eventsFilename = "./test/yu/test/input/run265opt100.events.txt.gz";
		final String eventsFilename = "../runs/run263/100.events.txt.gz";		
		final String outputFilename = "./test/yu/test/output/run263AvgSpeed.txt.gz";

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

		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
