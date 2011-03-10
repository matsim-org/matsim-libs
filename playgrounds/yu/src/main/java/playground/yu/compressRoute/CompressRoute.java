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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * this Class counts and writes the amount of "new" filtered links as well as
 * that of "old" links by reading "old" plansfile, gets compression ratio of the
 * sparely new network with sparely linkroute.
 *
 * @author ychen
 *
 */
public class CompressRoute extends AbstractPersonAlgorithm {
	/**
	 * (arg0) - ssLinkId: the id of the "default next" link of the current link
	 * (arg1) - linkId: the id of a current link
	 */
	private Map<String, String> ssLinks = new TreeMap<String, String>();

	private final DataOutputStream out;

	/** Counter of "old" links */
	private int oldLinksNr;

	/** Counter of "new" filtered links */
	private int newLinksNr;

	public CompressRoute(Map<String, String> ssLinks, Population plans,
			String fileName) throws IOException {
		this.ssLinks = ssLinks;
		this.out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(new File(fileName))));
		System.out.println("  begins to write txt-file");
		this.out.writeBytes("oldLinkRoute\tnewLinkRoute\n");
		this.oldLinksNr = 0;
		this.newLinksNr = 0;
	}

	/**
	 * counts and writes the amount of "old" links and that of "new" filtered
	 * links
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			List actsLegs = plan.getPlanElements();
			Stack<Id> newLinkIds = new Stack<Id>();
			for (int legId = 1; legId < actsLegs.size(); legId += 2) {
				LegImpl leg = (LegImpl) actsLegs.get(legId);
				List<Id> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();
				int linksLength = linkIds.size();
				this.oldLinksNr += linksLength;
				try {
					this.out.writeBytes("[");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				for (int i = 0; i < linksLength; i++)
					try {
						this.out.writeBytes(linkIds.get(i).toString());
						if (i < linksLength - 1)
							this.out.writeBytes("-");
					} catch (IOException e) {
						e.printStackTrace();
					}
				try {
					this.out.writeBytes("]");
				} catch (IOException e) {
					e.printStackTrace();
				}
				newLinkIds.clear();
				for (int i = linksLength - 1; i > 0; i--) {
					Id sslId = linkIds.get(i);
					if (this.ssLinks.containsKey(sslId)) {
						if (!linkIds.get(i - 1).toString().equals(
								this.ssLinks.get(sslId))) {
							newLinkIds.push(sslId);
						}
					} else {
						newLinkIds.push(sslId);
					}
				}
				try {
					this.out.writeBytes("-->[");
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.newLinksNr += newLinkIds.size();
				while (!newLinkIds.empty()) {
					try {
						this.out.writeBytes(((newLinkIds.pop())).toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (!newLinkIds.empty()) {
						try {
							this.out.writeBytes("-");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				try {
					this.out.writeBytes("]\n--------------------\n");
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
		this.out.writeBytes("old links : " + this.oldLinksNr
				+ ";\nnew links : " + this.newLinksNr + ";");
		this.out.close();
	}

	public static void main(final String[] args) throws IOException {
		Config config = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]).loadScenario().getConfig();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		System.out.println("  reading the network...");
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");

		// analyse Netzwerk, make TreeMap<String ssLinkId, String linkId>
		System.out.println("-->analysiing network");
		SubsequentCapacity ss = new SubsequentCapacity(network);
		ss.compute();
		System.out.println("-->done.");

		System.out.println("  setting up plans objects...");
		final PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		// compress routes
		CompressRoute cr = new CompressRoute(ss.getSsLinks(), plans,
				"./test/yu/output/linkrout_capacity.txt");
		System.out.println("  done.");

		System.out.println("  reading and writing plans...");
		plansReader.readFile(config.plans().getInputFile());
		cr.run(plans);
		System.out.println("  done.");

		System.out.println("-->begins to write result...");
		cr.writeEnd();
		System.out.println("-->done");
	}
}
