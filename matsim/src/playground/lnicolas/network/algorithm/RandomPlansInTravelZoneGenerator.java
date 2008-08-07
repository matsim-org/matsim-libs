/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlansInTravelZoneGenerator.java
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

package playground.lnicolas.network.algorithm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;

public class RandomPlansInTravelZoneGenerator extends NetworkAlgorithm {

	Rectangle.Double outerZone;
	Rectangle.Double innerZone;
	double nodeDist;
	private final int tripCount;

	public RandomPlansInTravelZoneGenerator(final double xCenter, final double yCenter, final double nodeDist,
			final int tripCount) {
		double outerSideLength = nodeDist+5000;
		this.outerZone = new Rectangle.Double(xCenter - (outerSideLength/2),
				yCenter - (outerSideLength/2),	outerSideLength, outerSideLength);
		double innerSideLength = nodeDist-5000;
		this.innerZone = new Rectangle.Double(xCenter - (innerSideLength/2),
				yCenter - (innerSideLength/2),	innerSideLength, innerSideLength);
		this.nodeDist = nodeDist;
		this.tripCount = tripCount;
	}

	@Override
	public void run(final NetworkLayer network) {

		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Node> fromNodes = new ArrayList<Node>();

		for (Node n : network.getNodes().values()) {
			if (this.outerZone.contains(n.getCoord().getX(), n.getCoord().getY())) {
				nodes.add(n);
				if (this.innerZone.contains(n.getCoord().getX(), n.getCoord().getY()) == false) {
					fromNodes.add(n);
				}
			}
		}
		int roleIndex = network.requestNodeRole();

		Population plans = new Population();
		Person person = new Person(new IdImpl("1"));
		Plan plan = person.createPlan(true);
		try {
			plans.addPerson(person);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int fromNodeIndex = 0;
		boolean done = false;
		double avgFromToDistance = 0;
		while (done == false) {
			avgFromToDistance = 0;
			Node n = fromNodes.get(fromNodeIndex);
			done = true;
			for (int i = 0; i < this.tripCount; i++) {
				Node toNode = getToNode(n, nodes, roleIndex);
				if (toNode == null) {
					resetToNodes(nodes, roleIndex);
					toNode = getToNode(n, nodes, roleIndex);
					if (toNode == null) {
						System.out.println("No further toNode found for node "
								+ n.getId() + " (Iteration " + i + ")");
						// System.exit(0);
						done = false;
						break;
					}
				}
				avgFromToDistance = (avgFromToDistance * i + n.getCoord()
						.calcDistance(toNode.getCoord()))
						/ (i + 1);
				Link toLink = toNode.getInLinks().values().iterator().next();
				addTrip(plan, toLink, i);
				n = toNode;
			}
			fromNodeIndex++;
		}
		System.out.println("Avg from-to distance is " + avgFromToDistance);
		System.out.println("Nodes in outer zone: " + nodes.size());
		System.out.println("Number of from nodes: " + fromNodes.size());

		PopulationWriter plans_writer = new PopulationWriter(plans);
		plans_writer.write();
		System.out.println("Wrote plans to "
				+ Gbl.getConfig().plans().getOutputFile());
	}

	private void resetToNodes(final ArrayList<Node> nodes, final int roleIndex) {
		for (Node n : nodes) {
			PlansGeneratorRole r = getRole(n, roleIndex);
			r.resetToNodes();
		}
	}

	private void addTrip(final Plan plan, final Link toLink, final int id) {
		try {
			if (id != 0) {
				plan.createLeg("car", null, null, null);
			}

			// Get random link
			int endTime = (int) (Math.random() * 48);

			plan.createAct("w", -1, -1, toLink, 0, endTime, 0, false);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private Node getToNode(final Node n, final ArrayList<Node> nodes, final int roleIndex) {
		double minDist = this.nodeDist * 0.9;
		double maxDist = this.nodeDist * 1.1;
		for (Node toNode : nodes) {
			double dist = n.getCoord().calcDistance(toNode.getCoord());
			if ((dist >= minDist) && (dist <= maxDist)) {
				PlansGeneratorRole r = getRole(n, roleIndex);
				if ((r.planExists(toNode) == false) && (toNode.getInLinks().size() > 0)) {
					r.addToNode(toNode);
					return toNode;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the role for the given Node. Creates a new Role if none exists
	 * yet.
	 *
	 * @param n
	 *            The Node for which to create a role.
	 * @return The role for the given Node
	 */
	PlansGeneratorRole getRole(final Node n, final int roleIndex) {
		PlansGeneratorRole r = (PlansGeneratorRole) n.getRole(roleIndex);
		if (null == r) {
			r = new PlansGeneratorRole();
			n.setRole(roleIndex, r);
		}
		return r;
	}

	class PlansGeneratorRole {
		TreeMap<Id, Node> existingPlans = new TreeMap<Id, Node>();

		public boolean planExists(final Node toNode) {
			return this.existingPlans.containsKey(toNode.getId());
		}

		public void resetToNodes() {
			this.existingPlans.clear();
		}

		public void addToNode(final Node toNode) {
			this.existingPlans.put(toNode.getId(), toNode);
		}
	}

}
