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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author ychen
 * 
 */
public class Wait2Link_2Acts1LinkTest {
	public static class AgentLinkPair {
		private final String agentId;
		private final String linkId;

		public AgentLinkPair(final String agentId, final String linkId) {
			this.agentId = agentId;
			this.linkId = linkId;
		}

		@Override
		public String toString() {
			return this.agentId + "\t" + this.linkId + "\n";
		}
	}

	public static class SameActLoc extends AbstractPersonAlgorithm {
		private boolean actsAtSameLink;
		private int actLocCount = 0, personCount = 0;
		/**
		 * @param arg0
		 *            -String agentId;
		 * @param arg1
		 *            -String linkId, which indicates a link, where 2 following
		 *            activities happened.
		 */
		private final Set<AgentLinkPair> agentLinks = new HashSet<AgentLinkPair>();

		@Override
		public void run(final Person person) {
			this.actsAtSameLink = false;
			String tmpLinkId = null;
			String nextTmpLinkId = null;
			if (person != null) {
				Plan p = person.getSelectedPlan();
				if (p != null) {
					List<PlanElement> actsLegs = p.getPlanElements();
					int max = actsLegs.size();
					for (int i = 0; i < max; i++)
						if (i % 2 == 0) {
							ActivityImpl a = (ActivityImpl) actsLegs.get(i);
							nextTmpLinkId = a.getLinkId().toString();
							if (tmpLinkId != null && nextTmpLinkId != null)
								if (tmpLinkId.equals(nextTmpLinkId)) {
									this.actLocCount++;
									this.actsAtSameLink = true;
									this.agentLinks.add(new AgentLinkPair(
											person.getId().toString(),
											tmpLinkId));
								}
							tmpLinkId = nextTmpLinkId;
						}
					if (this.actsAtSameLink)
						this.personCount++;
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

	public static class Wait2Link implements AgentWait2LinkEventHandler {
		private final Set<AgentLinkPair> agentLinksPairs;
		private BufferedWriter writer;
		private int overlapCount;

		/**
		 * @param linkIds
		 *            - a map<String agentId,String linkId> of the
		 *            agentId-linkId pair from SameActLoc
		 */
		public Wait2Link(final Set<AgentLinkPair> agentLinkPairs,
				final String outputFilename) {
			this.agentLinksPairs = agentLinkPairs;
			try {
				this.writer = IOUtils.getBufferedWriter(outputFilename);
				this.writer.write("time\tagentId\tLinkId\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.overlapCount = 0;
		}

		public void handleEvent(final AgentWait2LinkEvent event) {
			for (AgentLinkPair alp : this.agentLinksPairs)
				if (alp.agentId.equals(event.getPersonId().toString())
						&& alp.linkId.equals(event.getLinkId().toString())) {
					try {
						this.writer.write(alp.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					this.overlapCount++;
				}
		}

		public void reset(final int iteration) {
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
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.plans.xml.gz";
		final String eventsFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.events.txt.gz";
		final String outputFilename = "../data/ivtch/Wait2Links_2Acts1Link.txt.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		SameActLoc sal = new SameActLoc();
		sal.run(population);

		System.out.println("there is " + sal.getPersonCount() + " persons, "
				+ sal.getActLocCount() + " 2Acts1Link-s!");

		EventsManagerImpl events = new EventsManagerImpl();

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
