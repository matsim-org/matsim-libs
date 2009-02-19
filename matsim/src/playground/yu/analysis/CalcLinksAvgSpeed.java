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
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;
import org.xml.sax.SAXException;

/**
 * @author ychen
 * 
 */
public class CalcLinksAvgSpeed extends CalcNetAvgSpeed {
	/**
	 * @param arg0
	 *            - a String linkId
	 * @param arg1
	 *            - a SpeedCounter object
	 */
	private final HashMap<String, SpeedCounter> speedCounters = new HashMap<String, SpeedCounter>();
	private Set<Link> interestLinks = null;
	private final int binSize, nofBins;
	private final double[] speeds;
	private final int[] speedsCount;

	/**
	 * @param network
	 * @param binSize
	 *            - size of a timeBin e.g. (300s, 3600s)
	 * @param nofBins
	 *            - number of bins
	 */
	public CalcLinksAvgSpeed(final NetworkLayer network, final int binSize,
			final int nofBins) {
		super(network);
		this.binSize = binSize;
		this.nofBins = nofBins;
		speeds = new double[nofBins - 1];
		speedsCount = new int[nofBins - 1];
	}

	public CalcLinksAvgSpeed(final NetworkLayer network, final int binSize) {
		this(network, binSize, 30 * 3600 / binSize + 1);
	}

	public CalcLinksAvgSpeed(final NetworkLayer network) {
		this(network, 300);
	}

	/**
	 * support the speed calculation only for the links in a circle area
	 * 
	 * @param network
	 * @param x
	 *            -abscissa of the center of the circle area
	 * @param y
	 *            -vertical coordinates of the center of the circle area
	 * @param radius
	 *            -radius of the circle
	 */
	public CalcLinksAvgSpeed(final NetworkLayer network, final double x,
			final double y, final double radius) {
		this(network);
		interestLinks = new NetworkLinksInCircle(network)
				.getLinks(x, y, radius);
	}

	public CalcLinksAvgSpeed(final NetworkLayer network, int binSize,
			final double x, final double y, final double radius) {
		this(network, binSize);
		interestLinks = new NetworkLinksInCircle(network)
				.getLinks(x, y, radius);
	}

	public CalcLinksAvgSpeed(final NetworkLayer network,
			final RoadPricingScheme toll) {
		this(network);
		interestLinks = new HashSet<Link>(toll.getLinks());
	}

	public static class SpeedCounter {
		private final double[] lengthSum;
		private final double[] timeSum;
		private final double[] freeSpeeds;
		/**
		 * @param arg0
		 *            - agentId;
		 * @param arg1
		 *            - enterTime;
		 */
		private final HashMap<String, Double> enterTimes = new HashMap<String, Double>();

		/**
		 * @param nofBins
		 *            - number of bins.
		 */
		public SpeedCounter(final int nofBins, final double[] freeSpeeds) {
			lengthSum = new double[nofBins];
			timeSum = new double[nofBins];
			this.freeSpeeds = freeSpeeds;
		}

		public void lengthSumAppend(final int timeBin, final double length) {
			lengthSum[timeBin] += length;
		}

		public void timeSumAppend(final int timeBin, final double time) {
			timeSum[timeBin] += time;
		}

		public double getSpeed(final int timeBin) {
			return timeSum[timeBin] != 0.0 ? lengthSum[timeBin]
					/ timeSum[timeBin] * 3.6 : freeSpeeds[timeBin] * 3.6;
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
		return sc != null ? sc.getSpeed(getBinIdx(time)) : network.getLink(
				linkId).getFreespeed(time) * 3.6;
	}

	@Override
	public void handleEvent(final AgentArrivalEvent arrival) {
		SpeedCounter sc = speedCounters.get(arrival.linkId);
		if (sc != null)
			sc.removeTmpEnterTime(arrival.agentId);
	}

	@Override
	public void handleEvent(final LinkEnterEvent enter) {
		String linkId = enter.linkId;
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc == null) {
			double[] freeSpeeds = new double[nofBins];
			for (int i = 0; i < nofBins - 1; i++)
				freeSpeeds[i] = network.getLink(linkId).getFreespeed(
						i * 86400.0 / nofBins);
			sc = new SpeedCounter(nofBins, freeSpeeds);
		}
		sc.setTmpEnterTime(enter.agentId, enter.time);
		speedCounters.put(linkId, sc);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent leave) {
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
					double speed = getAvgSpeed(linkId, (double) j * binSize);
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

	public Set<String> getInterestLinkIds() {
		Set<String> interestLinkIds = new HashSet<String>();
		for (Link link : interestLinks == null ? network.getLinks().values()
				: interestLinks) {
			interestLinkIds.add(link.getId().toString());
		}
		return interestLinkIds;
	}

	/**
	 * @param arg0
	 *            networkFilename;
	 * @param arg1
	 *            eventsFilename;
	 * @param arg2
	 *            outputpath;
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */

	public static void run_Gauteng(String[] args) throws SAXException,
			ParserConfigurationException, IOException {
		Gbl.startMeasurement();
		System.out.println("-> begin run_Gauteng!");
		final String netFilename = args[0];
		final String eventsFilename = args[1];
		final String outputPath = args[2];

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Events events = new Events();
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(network, 900);
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		clas.write(outputPath + "avgSpeed.txt.gz");
		clas.writeChart(outputPath + "avgSpeed.png");

		System.out.println("-> Done run_Gauteng!");
		Gbl.printElapsedTime();

	}

	/**
	 * @param arg0
	 *            networkFilename;
	 * @param arg1
	 *            eventsFilename;
	 * @param arg2
	 *            roadpricingFilename;
	 * @param arg3
	 *            outpath;
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void run_roadpricing(String[] args) throws SAXException,
			ParserConfigurationException, IOException {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String eventsFilename = args[1];
		final String roadPricingFilename = args[2];
		final String outputPath = args[3];

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Events events = new Events();

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(network);
		tollReader.parse(roadPricingFilename);
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(network, tollReader
				.getScheme());
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		clas.write(outputPath + "avgSpeed.txt.gz");
		clas.writeChart(outputPath + "avgSpeed.png");

		System.out.println("-> Done!");
		Gbl.printElapsedTime();
	}

	public static void main(final String[] args) throws SAXException,
			ParserConfigurationException, IOException {
		if (args.length == 4) {
			run_roadpricing(args);
		} else {
			run_Gauteng(args);
		}
		System.out.println(0);
	}
}
