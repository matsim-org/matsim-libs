/* *********************************************************************** *
 * project: org.matsim.*
 * Wait2Link_2Acts1LinkTest.java
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
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.config.Config;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author ychen
 *
 */
public class Wait2Link_2Acts1LinkTest {
	public static class AgentLinkPair {
		private String agentId;
		private String linkId;
		private int legNr;

		public AgentLinkPair(String agentId, String linkId, int legNr) {
			this.agentId = agentId;
			this.linkId = linkId;
			this.legNr = legNr;
		}

		@Override
		public String toString() {
			return this.legNr + "\t" + this.agentId + "\t" + this.linkId + "\n";
		}
	}

	public static class SameActLoc extends PersonAlgorithm {
		private boolean actsAtSameLink;
		private int actLocCount = 0, personCount = 0;
		/**
		 * @param arg0-String
		 *            agentId;
		 * @param arg1-String
		 *            linkId, which indicates a link, where 2 following
		 *            activities happened.
		 */
		private Set<AgentLinkPair> agentLinks = new HashSet<AgentLinkPair>();

		@SuppressWarnings("unchecked")
		@Override
		public void run(Person person) {
			this.actsAtSameLink = false;
			String tmpLinkId = null;
			String nextTmpLinkId = null;
			if (person != null) {
				Plan p = person.getSelectedPlan();
				if (p != null) {
					List actsLegs = p.getActsLegs();
					int max = actsLegs.size();
					for (int i = 0; i < max; i++) {
						if (i % 2 == 0) {
							Act a = (Act) actsLegs.get(i);
							nextTmpLinkId = a.getLink().getId().toString();
							if ((tmpLinkId != null) && (nextTmpLinkId != null)) {
								if (tmpLinkId.equals(nextTmpLinkId)) {
									this.actLocCount++;
									this.actsAtSameLink = true;
									this.agentLinks.add(new AgentLinkPair(person
											.getId().toString(), tmpLinkId,
											((Leg) actsLegs.get(i - 1))
													.getNum()));
								}
							}
							tmpLinkId = nextTmpLinkId;
						}
					}
					if (this.actsAtSameLink) {
						this.personCount++;
					}
				}
			}
		}

		/**
		 * @return the agentLinks
		 */
		public Set<AgentLinkPair> getAgentLinks() {
			return this.agentLinks;
		}

		/**
		 * @return the actLocCount
		 */
		public int getActLocCount() {
			return this.actLocCount;
		}

		/**
		 * @return the personCount
		 */
		public int getPersonCount() {
			return this.personCount;
		}
	}

	public static class Wait2Link implements EventHandlerAgentWait2LinkI {
		private Set<AgentLinkPair> agentLinksPairs;
		private BufferedWriter writer;
		private int overlapCount;

		/**
		 * @param linkIds-
		 *            a map<String agentId,String linkId> of the agentId-linkId
		 *            pair from SameActLoc
		 */
		public Wait2Link(Set<AgentLinkPair> agentLinkPairs,
				String outputFilename) {
			this.agentLinksPairs = agentLinkPairs;
			try {
				this.writer = IOUtils.getBufferedWriter(outputFilename);
				this.writer.write("time\tagentId\tLinkId\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.overlapCount = 0;
		}

		public void handleEvent(EventAgentWait2Link event) {
			for (AgentLinkPair alp : this.agentLinksPairs) {
				if (alp.agentId.equals(event.agentId)
						&& alp.linkId.equals(event.linkId)
						&& (alp.legNr == event.legId)) {
					try {
						this.writer.write(alp.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					this.overlapCount++;
				}
			}
		}

		public void reset(int iteration) {
			this.agentLinksPairs.clear();
		}

		public void end() {
			try {
				this.writer.write("-->overlapCount = " + this.overlapCount);
				this.writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 */
	public Wait2Link_2Acts1LinkTest() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.plans.xml.gz";
		final String eventsFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.events.txt.gz";
		final String outputFilename = "../data/ivtch/Wait2Links_2Acts1Link.txt.gz";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);
		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		Plans population = new Plans();

		SameActLoc sal = new SameActLoc();
		population.addAlgorithm(sal);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);
		world.setPopulation(population);

		population.runAlgorithms();

		System.out.println("there is " + sal.getPersonCount() + " persons, "
				+ sal.getActLocCount() + " 2Acts1Link-s!");

		Events events = new Events();

		Wait2Link w2l = new Wait2Link(sal.getAgentLinks(), outputFilename);
		events.addHandler(w2l);

		System.out.println("-> reading eventsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		w2l.end();

		System.out.println("-> overlapCount = " + w2l.overlapCount);
		System.out.println("-> Done!");

		Gbl.printElapsedTime();
		System.exit(0);
	}
}
