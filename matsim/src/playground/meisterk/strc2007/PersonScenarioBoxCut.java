/* *********************************************************************** *
 * project: org.matsim.*
 * PersonScenarioBoxCut.java
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

package playground.meisterk.strc2007;

import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.geometry.CoordI;

public class PersonScenarioBoxCut extends PersonAlgorithm {

	/**
	 * Will hold all persons with home location in the box.
	 */
	private Plans plansCut = null;
	private NetworkLayer network = null;
	/**
	 * Will hold all links and nodes used by the persons in <code>plansCut</code>.
	 */
	private NetworkLayer networkTouched = null;
	private double north;
	private double south;
	private double west;
	private double east;

	public PersonScenarioBoxCut(NetworkLayer network, double north,
			double south, double west, double east) {
		super();
		this.network = network;
		this.north = north;
		this.south = south;
		this.west = west;
		this.east = east;
		this.plansCut = new Plans();
		this.networkTouched = new NetworkLayer();
	}

	public Plans getPlansInTheHomeBox() {
		return this.plansCut;
	}

	@Override
	public void run(Person person) {

		List<Plan> plans = person.getPlans();
		// use the first plan
		Plan firstPlan = plans.get(0);
		// home is first activity in the plan
		Act home = (Act) firstPlan.getIteratorAct().next();
		CoordI homeCoord = home.getCoord();
		// check if home activity is in the box
		if (
				(homeCoord.getX() > this.west) &&
				(homeCoord.getX() < this.east) &&
				(homeCoord.getY() > this.south) &&
				(homeCoord.getY() < this.north)) {

			try {
				// retain this person
				this.plansCut.addPerson(person);
				// extract all the links the first plan of this person touches
				// and add them to the cut network if not yet done
				for (Object o : firstPlan.getActsLegs()) {

					if (o.getClass().equals(Act.class)) {

						Link actLink = ((Act) o).getLink();
						this.addLinkToCutNetwork(actLink);

					} else if (o.getClass().equals(Leg.class)) {

						Link[] routeLinks = ((Leg) o).getRoute().getLinkRoute();
						for (Link routeLink : routeLinks) {
							this.addLinkToCutNetwork(routeLink);
						}
					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public NetworkLayer getTouchedNetwork() {
		return this.networkTouched;
	}

	private void addLinkToCutNetwork(Link link) {

		org.matsim.utils.geometry.shared.Coord nodeCoord = null;

		// is the link already in the cut network? if not, add it
		if (this.networkTouched.getLink(link.getId()) == null) {

			// are the nodes already in the cut network? if not, add them
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();

			if (this.networkTouched.getNode(fromNode.getId().toString()) == null) {

				nodeCoord = fromNode.getCoord();
				this.networkTouched.createNode(
						fromNode.getId().toString(),
						Double.toString(nodeCoord.getX()),
						Double.toString(nodeCoord.getY()),
						fromNode.getType());
			}

			if (this.networkTouched.getNode(toNode.getId().toString()) == null) {

				nodeCoord = toNode.getCoord();
				this.networkTouched.createNode(
						toNode.getId().toString(),
						Double.toString(nodeCoord.getX()),
						Double.toString(nodeCoord.getY()),
						toNode.getType());
			}

			// finally, add the link
			this.networkTouched.createLink(
					link.getId().toString(),
					fromNode.getId().toString(),
					toNode.getId().toString(),
					Double.toString(link.getLength()),
					Double.toString(link.getFreespeed()),
					Double.toString(link.getCapacity()),
					Integer.toString(link.getLanes()),
					link.getOrigId(),
					link.getType());
		}

	}
}