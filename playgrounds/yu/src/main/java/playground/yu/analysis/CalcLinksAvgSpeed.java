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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author ychen
 *
 */
public class CalcLinksAvgSpeed extends CalcNetAvgSpeed {
	private final static Logger log = Logger.getLogger(CalcLinksAvgSpeed.class);

	/**
	 * @param arg0
	 *            - a String linkId
	 * @param arg1
	 *            - a SpeedCounter object
	 */
	private final HashMap<String, SpeedCounter> speedCounters = new HashMap<String, SpeedCounter>();
	private Set<Id> interestLinks = null;
	private final int binSize, nofBins;
	private final double[] speeds;
	private final int[] speedsCount;
	private RoadPricingScheme toll = null;

	/**
	 * @param network
	 * @param binSize
	 *            - size of a timeBin e.g. (300s, 3600s)
	 * @param nofBins
	 *            - number of bins
	 */
	public CalcLinksAvgSpeed(final Network network, final int binSize,
			final int nofBins) {
		super(network);
		this.binSize = binSize;
		this.nofBins = nofBins;
		speeds = new double[nofBins - 1];
		speedsCount = new int[nofBins - 1];
	}

	public CalcLinksAvgSpeed(final Network network, final int binSize) {
		this(network, binSize, 30 * 3600 / binSize + 1);
	}

	public CalcLinksAvgSpeed(final Network network) {
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
	public CalcLinksAvgSpeed(final Network network, final double x,
			final double y, final double radius) {
		this(network);
		interestLinks = new NetworkLinksInCircle(network)
				.getLinks(x, y, radius);
	}

	public CalcLinksAvgSpeed(final Network network, int binSize,
			final double x, final double y, final double radius) {
		this(network, binSize);
		interestLinks = new NetworkLinksInCircle(network)
				.getLinks(x, y, radius);
	}

	public CalcLinksAvgSpeed(final Network network, final RoadPricingScheme toll) {
		this(network);
		this.toll = toll;
		if (toll != null) {
			interestLinks = new HashSet<Id>(toll.getLinkIdSet());
		}
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
		public SpeedCounter(final double[] freeSpeeds) {
			this.freeSpeeds = freeSpeeds.clone();
			int length = this.freeSpeeds.length;
			lengthSum = new double[length];
			timeSum = new double[length];
		}

		public void lengthSumAppend(final int timeBin, final double length) {
			lengthSum[timeBin] += length;
		}

		public void timeSumAppend(final int timeBin, final double time) {
			timeSum[timeBin] += time;
		}

		/**
		 * @param timeBin
		 * @return speed [km/h], if there was not traffic on a link, returns
		 *         directly the free speed
		 */
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

	/**
	 * @param linkId
	 * @param time
	 * @return avg. speed [km/h]
	 */
	public double getAvgSpeed(final Id linkId, final double time) {
		SpeedCounter sc = speedCounters.get(linkId.toString());
		return sc != null ? sc.getSpeed(getBinIdx(time)) : network.getLinks()
				.get(linkId).getFreespeed(time) * 3.6;
	}

	@Override
	public void handleEvent(final AgentArrivalEvent arrival) {
		SpeedCounter sc = speedCounters.get(arrival.getLinkId().toString());
		if (sc != null) {
			sc.removeTmpEnterTime(arrival.getPersonId().toString());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent enter) {
		String linkId = enter.getLinkId().toString();
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc == null) {
			double[] freeSpeeds = new double[nofBins];
			for (int i = 0; i < nofBins - 1; i++) {
				freeSpeeds[i] = network.getLinks().get(new IdImpl(linkId))
						.getFreespeed(i * 86400.0 / nofBins);
			}
			sc = new SpeedCounter(freeSpeeds);
		}
		sc.setTmpEnterTime(enter.getPersonId().toString(), enter.getTime());
		speedCounters.put(linkId, sc);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent leave) {
		double time = leave.getTime();
		int timeBin = getBinIdx(time);
		String linkId = leave.getLinkId().toString();
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc != null) {
			Double enterTime = sc.removeTmpEnterTime(leave.getPersonId()
					.toString());
			if (enterTime != null) {
				Link l = network.getLinks().get(leave.getLinkId());
				if (l != null) {
					sc.lengthSumAppend(timeBin, l.getLength());
					sc.timeSumAppend(timeBin, time - enterTime);
				}
			}
		}
	}

	private int getBinIdx(final double time) {
		int bin = (int) time / binSize;
		if (bin >= nofBins) {
			return nofBins - 1;
		}
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
			StringBuffer head = new StringBuffer(
					"avg. Speed (car)\nlinkId\tCapacity");
			for (int i = 0; i < nofBins - 1; i++) {
				head.append("\tH" + Integer.toString(i) + "-"
						+ Integer.toString(i + 1));
			}
			head.append('\n');
			out.write(head.toString());
			out.flush();

			for (Id linkId : interestLinks == null ? network.getLinks()
					.keySet() : interestLinks) {
				StringBuffer line = new StringBuffer(linkId.toString());
				line.append('\t');
				Link link = network.getLinks().get(linkId);
				if (link != null) {
					line.append(link.getCapacity());
				} else {
					log.info("NULLPOINT ERROR:\tlink\t" + linkId
							+ "\tdoes not exist in network!");
				}

				for (int j = 0; j < nofBins - 1; j++) {
					double speed = getAvgSpeed(linkId, (double) j * binSize);
					line.append('\t');
					line.append(speed);
					if (speed > 0) {
						speeds[j] += speed;
						speedsCount[j]++;
					}
				}
				line.append('\n');
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
		for (int i = 0; i < xsLength; i++) {
			xs[i] = (double) i * (double) binSize / 3600.0;
		}
		double[] ySpeed = new double[xsLength];
		for (int i = 0; i < xsLength; i++) {
			if (speedsCount[i] > 0) {
				ySpeed[i] = speeds[i] / speedsCount[i];
			}
		}
		XYLineChart avgSpeedChart = new XYLineChart("avg. speed (car) in "
				+ (toll == null ? "cityarea" : "toll range"), "time",
				"avg. speed (car) [km/h]");
		avgSpeedChart.addSeries("avg. speed of all agents (car)", xs, ySpeed);
		avgSpeedChart.saveAsPng(chartFilename, 1024, 768);

		SimpleWriter writer = new SimpleWriter(chartFilename.replace(".png",
				".txt"));
		writer.writeln("time\tavg. speed (car) in "
				+ (toll == null ? "cityarea" : "toll range") + " [km/h]");

		for (int i = 0; i < xs.length; i++) {
			writer.writeln(Time.writeTime(xs[i] * 3600.0) + "\t" + ySpeed[i]);
		}
		writer.close();
	}

	public Set<Id> getInterestLinkIds() {
		Set<Id> interestLinkIds = new HashSet<Id>();
		for (Id linkId : interestLinks == null ? network.getLinks().keySet()
				: interestLinks) {
			interestLinkIds.add(linkId);
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

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		EventsManager events = EventsUtils.createEventsManager();
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(network, 900);
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		clas.write(outputPath + "gauteng_avgSpeed.txt.gz");
		clas.writeChart(outputPath + "gauteng_avgSpeed.png");

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

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		EventsManager events = EventsUtils.createEventsManager();

		scenario.getConfig().scenario().setUseRoadpricing(true);
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(
				scenario.getRoadPricingScheme());
		tollReader.parse(roadPricingFilename);
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(network,
				scenario.getRoadPricingScheme());
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		clas.write(outputPath + "roadpricing_avgSpeed.txt.gz");
		clas.writeChart(outputPath + "roadpricing_avgSpeed.png");

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
