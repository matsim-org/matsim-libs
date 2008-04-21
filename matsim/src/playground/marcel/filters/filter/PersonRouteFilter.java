/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRouteFilter.java
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

package playground.marcel.filters.filter;

import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;

/**
 * transfer the "right" persons to next PersonFilter. These "right" persons
 * don't move on false links and nodes, which should not exist in network(file).
 *
 * 	beurteilen, ob Personen auf nicht existierende Links oder durch nicht existierende Nodes fahren oder andere Aktivit�ten durchf�hren.
	Die richtigen Personen wurden hier zur NewPlansWriter übertragen.

 * @author yu chen
 */
public class PersonRouteFilter extends PersonFilterA {
	/**
	 * The underlying list of link-IDs of this PersonRouteFilter.
	 */
	private List<Id> criterionLinkIds;

	/**
	 * The underlying list of node-IDs of this PersonRouteFilter.
	 */
	private List<Id> criterionNodeIds;

	/**
	 * create a PersonFilter, which deletes Persons moving or staying on some
	 * links and nodes, which should not exist.
	 *
	 * @param linkIds -
	 *            a list of link-IDs, which should not exist in network-file.
	 * @param nodeIds -
	 *            a list of node-IDs, which should not exist in network-file
	 *
	 */
	public PersonRouteFilter(List<Id> linkIds, List<Id> nodeIds) {
		this.criterionLinkIds = linkIds;
		this.criterionNodeIds = nodeIds;
	}

	/**
	 * judge, whether the person will move or stay on some links and nodes,
	 * which should not exist in network(file).
	 */
	@Override
	public boolean judge(Person person) {
		List<Plan> plans = person.getPlans();
		for (Plan plan : plans) {
			if (plan.isSelected()) {
				List<Object> acts_Legs = plan.getActsLegs();
				boolean even = false;
				for (Object obj : acts_Legs) {
					if (even) {
						Leg leg = (Leg) obj;
						Route route = leg.getRoute();
						if (route != null) {
							Link[] links = route.getLinkRoute();
							if (links != null)
								for (Link link : links) {
									if (this.criterionLinkIds.contains(link.getId()))
										return false;
								}
							List<Node> nodes = route.getRoute();
							if (nodes != null)
								for (Node node : nodes) {
									if (this.criterionNodeIds.contains(node.getId()))
										return false;
								}
						}
					} else {
						Act act = (Act) obj;
						if (this.criterionLinkIds.contains(act.getLink().getId()))
							return false;
					}
					even = !even;
				}
			}
		}
		return true;
	}
}
