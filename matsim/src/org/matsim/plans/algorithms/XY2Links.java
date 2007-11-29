/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Links.java
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

package org.matsim.plans.algorithms;

import java.util.ArrayList;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

public class XY2Links extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkLayer network;
	private double maxDistance = Double.MAX_VALUE;
	private double minLength = Double.MIN_VALUE;
	private ArrayList<Link> linksArray;
	private ArrayList<Node> nodesArray;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public XY2Links(final NetworkLayer network) {
		super();
		this.network = network;
	}

	public XY2Links(final NetworkLayer network, final double maxDistance, final double minLength) {
		this(network);
		this.maxDistance = maxDistance;
		this.minLength = minLength;

		this.linksArray = new ArrayList<Link>();
		for (Link link : network.getLinks().values()) {
			if (link.getLength() >= minLength) {
				this.linksArray.add(link);
			}
		}

		// build nodesArray from linksArray
		this.nodesArray = new ArrayList<Node>();
		for (Link link: this.linksArray) {
			this.nodesArray.add(link.getFromNode());
			this.nodesArray.add(link.getToNode());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	private Link getNearestLink(final CoordI coord) {

		if (this.linksArray == null) {
			// no special links list, use default from network
			return this.network.getNearestLink(coord);
		}

		Link nearestLink = null;
		Node nearestNode = null;
		double shortestDistance = Double.MAX_VALUE;
		double cx = coord.getX();
		double cy = coord.getY();

		// this is quite the same as NetworkLayer.getNearestLink(coord), but it uses our optimized arrays
		// first find nearest node
		int max = this.nodesArray.size();
		for (int i = 0; i < max; i++) {
			Node node = this.nodesArray.get(i);
			Coord coord2 = node.getCoord();
			double x = coord2.getX();
			double y = coord2.getY();
			double dist2 = (cx - x) * (cx - x) + (cy - y) * (cy - y);
			// dist2: as long as we only compare numbers, we do not need Math.sqrt() for the distance

			if (dist2 < shortestDistance) {
				if (node.getOutLinks().size() > 0 && node.getInLinks().size() > 0) {
					shortestDistance = dist2;
					nearestNode = node;
				}
			}
		}

		if (nearestNode == null) {
			return null;
		}

		// now find nearest link from the nearest node
		shortestDistance = Double.MAX_VALUE; // reset the value
		Link nearestLink2 = null; // the nearest link without any restrictions
		double shortestDistance2 = Double.MAX_VALUE;
		for (Link link : nearestNode.getOutLinks().values()) {
			double dist = link.calcDistance(coord);

			if (link.getLength() >= this.minLength) {
				if (dist < shortestDistance2) {
					shortestDistance2 = dist;
					nearestLink2 = link;
				}
			} else {
				if (dist < shortestDistance) {
					shortestDistance = dist;
					nearestLink = link;
				}
			}
		}
		if (nearestLink == null) {
			return nearestLink2;
		}
		return nearestLink;
	}


	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();
		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			if (processPlan(plan) == false) {
				// all activities seem to lie outside our region, ignore this plan
				person.getPlans().remove(planId);
				planId--;
				nofPlans--;
			}
		}
	}

	public void run(final Plan plan) {
		processPlan(plan);
	}

	private boolean processPlan(final Plan plan) {
		int cntActs = 0;
		int cntOutside = 0;
		ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			cntActs++;
			Act act = (Act)actslegs.get(j);
			Link link = getNearestLink(act.getCoord());
			if (null == link) {
				throw new RuntimeException("getNearestLink returned Null!");
			}
			act.setLink(link);
			double dist = link.calcDistance(act.getCoord());
			if (dist > this.maxDistance) {
				cntOutside++;
			}
		}
		if (cntOutside == cntActs) {
			return false;
		}

		return true;
	}
}
