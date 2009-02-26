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

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;

/**
 * @author yu
 *
 */
public class AvoidOldNodes extends NewPlan {
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
	public void run(final Person person) {
		for (Plan p : person.getPlans()) {
			for (LegIterator i = p.getIteratorLeg(); i.hasNext();) {
				BasicLeg bl = i.next();
				CarRoute br = (CarRoute) bl.getRoute();
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
		this.pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Config config = Gbl.createConfig(args);

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
		population.addAlgorithm(aon);
		new MatsimPopulationReader(population, network).readFile(config.plans()
				.getInputFile());
		population.runAlgorithms();
		aon.writeEndPlans();
	}

}
