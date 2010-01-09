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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author yu
 * 
 */
public class AvoidOldNodes extends NewPopulation {
	private boolean nullRoute = false;
	private final Set<String> nodeIds;

	public AvoidOldNodes(final Network network, final Population plans) {
		super(network, plans);
		this.nodeIds = new HashSet<String>();
	}

	public void addNode(final String nodeId) {
		this.nodeIds.add(nodeId);
	}

	public void addLink(final String linkId) {
		Link l = this.net.getLinks().get(new IdImpl(linkId));
		this.nodeIds.add(l.getFromNode().getId().toString());
		this.nodeIds.add(l.getToNode().getId().toString());
	}

	@Override
	public void run(final Person person) {
		for (Plan p : person.getPlans()) {
			for (PlanElement pe : p.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg bl = (Leg) pe;
					NetworkRouteWRefs br = (NetworkRouteWRefs) bl.getRoute();
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
		Scenario scenario = new ScenarioLoaderImpl(args[0]).loadScenario();
	
		Population population = scenario.getPopulation();

		AvoidOldNodes aon = new AvoidOldNodes(scenario.getNetwork(), population);
		aon.addNode("100000");
		aon.addLink("3000000");
		aon.addLink("3000001");
		for (int i = 3000022; i <= 3000025; i++) {
			aon.addLink(Integer.toString(i));
		}
		aon.run(population);
		aon.writeEndPlans();
	}

}
