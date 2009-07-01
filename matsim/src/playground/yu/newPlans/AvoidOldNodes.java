/* *********************************************************************** *
 * project: org.matsim.*
 * AvoidOldNodes.java
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

package playground.yu.newPlans;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;

/**
 * @author yu
 * 
 */
public class AvoidOldNodes extends NewPopulation {
	private boolean nullRoute = false;
	private final Set<String> nodeIds;

	public AvoidOldNodes(final NetworkLayer network, final Population plans) {
		super(network, plans);
		this.nodeIds = new HashSet<String>();
	}

	public void addNode(final String nodeId) {
		this.nodeIds.add(nodeId);
	}

	public void addLink(final String linkId) {
		Link l = this.net.getLink(linkId);
		this.nodeIds.add(l.getFromNode().getId().toString());
		this.nodeIds.add(l.getToNode().getId().toString());
	}

	@Override
	public void run(final PersonImpl person) {
		for (PlanImpl p : person.getPlans()) {
			for (PlanElement pe : p.getPlanElements()) {
				if (pe instanceof BasicLeg) {
					BasicLeg bl = (BasicLeg) pe;
					NetworkRoute br = (NetworkRoute) bl.getRoute();
					if (br != null) {
						tag: for (final Node n : br.getNodes()) {
							final String nId = n.getId().toString();
							for (String nodeId : this.nodeIds) {
								if (nId.equals(nodeId)) {
									this.nullRoute = true;
									break tag;
								}
							}
						}
						if (this.nullRoute) {
							bl.setRoute(null);
							this.nullRoute = false;
						}
					}
				}
			}
		}
		this.pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Config config = new ScenarioLoader(args[0]).loadScenario().getConfig();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		Population population = new PopulationImpl();
		AvoidOldNodes aon = new AvoidOldNodes(network, population);
		aon.addNode("100000");
		aon.addLink("3000000");
		aon.addLink("3000001");
		for (int i = 3000022; i <= 3000025; i++) {
			aon.addLink(Integer.toString(i));
		}
		new MatsimPopulationReader(population, network).readFile(config.plans()
				.getInputFile());
		aon.run(population);
		aon.writeEndPlans();
	}

}
