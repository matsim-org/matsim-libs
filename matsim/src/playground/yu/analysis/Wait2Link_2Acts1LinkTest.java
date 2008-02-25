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
import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;

/**
 * @author ychen
 * 
 */
public class Wait2Link_2Acts1LinkTest {
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
		private Map<String, String> agentLinks = new HashMap<String, String>();

		@Override
		public void run(Person person) {
			actsAtSameLink = false;
			String tmpLinkId = null;
			String nextTmpLinkId = null;
			int i = 0;
			if (person != null) {
				Plan p = person.getSelectedPlan();
				// System.out.println("person id: " + person.getId());
				if (p != null) {
					for (ActIterator ai = p.getIteratorAct(); ai.hasNext();) {
						nextTmpLinkId = ai.next().getLink().getId().toString();
						if (tmpLinkId != null && nextTmpLinkId != null) {
							if (tmpLinkId.equals(nextTmpLinkId)) {
								actLocCount++;
								actsAtSameLink = true;
								agentLinks.put(person.getId().toString(),
										tmpLinkId);
							}
						}
						tmpLinkId = nextTmpLinkId;
						// System.out.println(tmpLinkId);
						i++;
					}
					if (actsAtSameLink) {
						personCount++;
					}
				}
			}
		}

		/**
		 * @return the agentLinks
		 */
		public Map<String, String> getAgentLinks() {
			return agentLinks;
		}
	}

	public static class Wait2Link implements EventHandlerAgentWait2LinkI {
		private Map<String, String> linksMap;
		private BufferedWriter writer;
		private int overlapCount;

		/**
		 * @param linkIds-
		 *            a map<String agentId,String linkId> of the agentId-linkId
		 *            pair from SameActLoc
		 */
		public Wait2Link(Map<String, String> linkIds, BufferedWriter writer) {
			linksMap = linkIds;
			this.writer = writer;
			try {
				writer.write("agentId\tLinkId\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			overlapCount = 0;
		}

		public void handleEvent(EventAgentWait2Link event) {
			String agentId = event.agentId;
			String linkId = linksMap.get(agentId);
			if (linkId != null) {
				try {
					writer.write(agentId + "\t" + linkId + "\n");
					overlapCount++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void reset(int iteration) {
			linksMap.clear();
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
		// TODO ... System.out.println("overlapCount = "+overlapCount++);
	}
}
