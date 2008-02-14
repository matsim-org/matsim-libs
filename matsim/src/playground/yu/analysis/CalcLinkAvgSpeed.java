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
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.config.Config;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
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

	/**
	 * @param network
	 */
	public CalcLinkAvgSpeed(NetworkLayer network) {
		super(network);
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

		public SpeedCounter() {
			for (int i = 0; i < 24; i++) {
				lengthSum[i] = 0.0;
				timeSum[i] = 0.0;
			}
		}

		public void lengSumAppend(int timeBin, double leng) {
			lengthSum[timeBin] += leng;
		}

		public void timeSumAppend(int timeBin, double time) {
			timeSum[timeBin] += time;
		}

		public double getSpeed(int timeBin) {
			return lengthSum[timeBin] / timeSum[timeBin] * 3.6;
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

	public double getAvgSpeed(IdI linkId) {
		return speedCounters.get(linkId).getSpeed();
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
			sc = new SpeedCounter();
		}
		sc.setTmpEnterTime(enter.agentId, enter.time);
		speedCounters.put(linkId, sc);
	}

	public void handleEvent(EventLinkLeave leave) {
		String linkId = leave.linkId;
		SpeedCounter sc = speedCounters.get(linkId);
		if (sc != null) {
			Double enterTime = sc.removeTmpEnterTime(leave.agentId);
			if (enterTime != null) {
				Link l = leave.link;
				if (l == null) {
					l = network.getLink(leave.linkId);
				}
				if (l != null) {
					sc.lengSumAppend(leave.link.getLength());
					sc.timeSumAppend(leave.time - enterTime);
				}
			}
		}
	}

	public void reset(int iteration) {
		speedCounters.clear();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		final String eventsFilename = "./test/yu/test/input/carNewPlans100.events.txt.gz";

		Config config = Gbl.createConfig(null
		// new String[] { "./test/yu/test/configTest.xml" }
				);

		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Events events = new Events();
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1,
				network);
		events.addHandler(volumes);
		CalcLinkAvgSpeed clas = new CalcLinkAvgSpeed(network);
		events.addHandler(clas);
		new MatsimEventsReader(events).readFile(eventsFilename);
		Map<IdI, QueueLink> links = (Map<IdI, QueueLink>) network.getLinks();
		try {
			BufferedWriter out = IOUtils
					.getBufferedWriter("./test/yu/test/output/carNewPlans100.eventsVolumeTest.txt.gz");
			StringBuffer head = new StringBuffer(
					"linkId\tCapacity\tSimulationFlowCapacity");
			for (int i = 0; i < 24; i++) {
				head.append("\tH" + Integer.toString(i) + "-"
						+ Integer.toString(i + 1));
			}
			head.append("\n");
			out.write(head.toString());
			out.flush();
			for (QueueLink ql : links.values()) {
				int[] v = volumes.getVolumesForLink(ql.getId().toString());
				StringBuffer line = new StringBuffer(ql.getId().toString()
						+ "\t" + ql.getCapacity() + "\t"
						+ ql.getSimulatedFlowCapacity());
				if (v != null) {
					for (int j = 0; j < 24; j++) {
						line.append("\t" + v[j]);
					}
				} else {
					for (int k = 0; k < 24; k++) {
						line.append("\t" + 0);
					}
				}
				line.append("\n");
				out.write(line.toString());
				out.flush();
			}
			out.close();
			out = IOUtils
					.getBufferedWriter("./test/yu/test/output/carNewPlans100.eventsAvgSpeedTest.txt.gz");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("-> Done!");
		System.exit(0);
	}

}
