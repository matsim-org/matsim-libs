/* *********************************************************************** *
 * project: org.matsim.*
 * CompressRoute.java
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

package playground.yu.compressRoute;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.matsim.network.Link;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;

/**
 * this Class counts and writes the amount of "new" filtered links as well as
 * that of "old" links by reading "old" plansfile, gets compression ratio of the
 * sparely new network with sparely linkroute.
 *
 * @author ychen
 *
 */
public class CompressRoute extends PersonAlgorithm {
	/**
	 * (arg0) - ssLinkId: the id of the "default next" link of the current link
	 * (arg1) - linkId: the id of a current link
	 */
	private Map<String, String> ssLinks = new TreeMap<String, String>();

	private DataOutputStream out;

	/** Counter of "old" links */
	private int oldLinksNr;

	/** Counter of "new" filtered links */
	private int newLinksNr;

	public CompressRoute(Map<String, String> ssLinks, Plans plans, String fileName)
			throws IOException {
		this.ssLinks = ssLinks;
		out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(new File(fileName))));
		System.out.println("  begins to write txt-file");
		out.writeBytes("oldLinkRoute\tnewLinkRoute\n");
		oldLinksNr = 0;
		newLinksNr = 0;
	}

	/**
	 * counts and writes the amount of "old" links and that of "new" filtered
	 * links
	 *
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			List actsLegs = plan.getActsLegs();
			Stack<Link> newLinks = new Stack<Link>();
			for (int legId = 1; legId < actsLegs.size(); legId += 2) {
				Leg leg = (Leg) actsLegs.get(legId);
				Link[] links = leg.getRoute().getLinkRoute();
				int linksLength = links.length;
				oldLinksNr += linksLength;
				try {
					out.writeBytes("[");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				for (int i = 0; i < linksLength; i++)
					try {
						out.writeBytes(links[i].getId().toString());
						if (i < linksLength - 1)
							out.writeBytes("-");
					} catch (IOException e) {
						e.printStackTrace();
					}
				try {
					out.writeBytes("]");
				} catch (IOException e) {
					e.printStackTrace();
				}
				newLinks.clear();
				for (int i = linksLength - 1; i > 0; i--) {
					Link ssl = links[i];
					String sslId = ssl.getId().toString();
					if (ssLinks.containsKey(sslId)) {
						if (!links[i - 1].getId().toString().equals(
								ssLinks.get(sslId))) {
							newLinks.push(ssl);
						}
					} else {
						newLinks.push(ssl);
					}
				}
				try {
					out.writeBytes("-->[");
				} catch (IOException e) {
					e.printStackTrace();
				}
				newLinksNr += newLinks.size();
				while (!newLinks.empty()) {
					try {
						out.writeBytes(((newLinks.pop())).getId()
								.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (!newLinks.empty()) {
						try {
							out.writeBytes("-");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				try {
					out.writeBytes("]\n--------------------\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * writes the count of "old" links and "new" links
	 *
	 * @throws IOException
	 */
	public void writeEnd() throws IOException {
		out.writeBytes("old links : " + oldLinksNr + ";\nnew links : "
				+ newLinksNr + ";");
		out.close();
	}
}
