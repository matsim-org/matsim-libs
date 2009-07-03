/* *********************************************************************** *
 * project: org.matsim.*
 * Subsequent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.network.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.Time;

/**
 * Analyzes the network to find for each link the "logical subsequent link",
 * i.e. the link that most people are likely to continue driving on when
 * crossing the next node.  This could depend on the geometry (assuming it
 * is more likely to continue straight on instead of making a U-turn), the
 * capacity of links (likely to continue on links with the same or larger
 * capacity instead of using small bumpy roads), the speed limit, other
 * attributes or combinations there of. This code only looks at geometry.
 *
 * @author ychen
 * @author mrieser
 */
public class SubsequentLinksAnalyzer {

	private final NetworkLayer network;

	private final static LinkIdComparator linkComparator = new LinkIdComparator();

	/** Stores the logical subsequent link (value) for a given link (key). */
	private final TreeMap<LinkImpl, LinkImpl> subsequentLinks = new TreeMap<LinkImpl, LinkImpl>(linkComparator);

	public SubsequentLinksAnalyzer(final NetworkLayer network) {
		this.network = network;
		compute();
	}

	/**
	 * @return a map, giving for each link (key in map) the computed subsequent link (value in map).
	 */
	public Map<LinkImpl, LinkImpl> getSubsequentLinks() {
		return this.subsequentLinks;
	}

	/**
	 * calculates the "default next" link of the current link with respect to
	 * geometry and Capacity (depending on BETA) and writes the result in
	 * ssLinks.
	 */
	private void compute() {
		Map<LinkImpl, Double> absDeltaThetas = new TreeMap<LinkImpl, Double>(linkComparator);
		for (LinkImpl l : this.network.getLinks().values()) {
			NodeImpl from = l.getFromNode();
			NodeImpl to = l.getToNode();
			Coord cFrom = from.getCoord();
			Coord cTo = to.getCoord();
			double xTo = cTo.getX();
			double yTo = cTo.getY();
			double thetaL = Math.atan2(yTo - cFrom.getY(), xTo - cFrom.getX());
			Collection<? extends LinkImpl> outLinks = to.getOutLinks().values();
			absDeltaThetas.clear();
			if (outLinks.size() > 1) {

				for (LinkImpl out : outLinks) {
					Coord cOut = out.getToNode().getCoord();
					double deltaTheta = Math.atan2(cOut.getY() - yTo, cOut.getX()	- xTo) - thetaL;
					while (deltaTheta < -Math.PI) {
						deltaTheta += 2.0 * Math.PI;
					}
					while (deltaTheta > Math.PI) {
						deltaTheta -= 2.0 * Math.PI;
					}
					absDeltaThetas.put(out, Math.abs(deltaTheta));
				}
				this.subsequentLinks.put(l, computeSubsequentLink(l, absDeltaThetas));
			} else if (outLinks.size() == 1) {
				this.subsequentLinks.put(l, outLinks.iterator().next());
			}
		}
	}

	/**
	 * @param l
	 * @param thetas map of outgoing-link and theta of that link. must include at least one entry, otherwise it won't work!
	 * @return the link with the smallest theta, if multiple links have the same smallest theta, the one with the biggest capacity is returned.
	 */
	private LinkImpl computeSubsequentLink(final LinkImpl l, final Map<LinkImpl, Double> thetas) {
		List<LinkImpl> minThetaOutLinks = new ArrayList<LinkImpl>();

		minThetaOutLinks.clear();
		double absMin = Collections.min(thetas.values()).doubleValue();
		for (Map.Entry<LinkImpl, Double> entry : thetas.entrySet()) {
			if (absMin == (entry.getValue()).doubleValue()) {
				minThetaOutLinks.add(entry.getKey());
			}
		}
		if (minThetaOutLinks.size() == 1) {
			return minThetaOutLinks.get(0);
		}
	
		double maxCapacity = Double.NEGATIVE_INFINITY;
		LinkImpl maxCapLink = null;
		for (LinkImpl link : minThetaOutLinks) {
			double cap = link.getCapacity(Time.UNDEFINED_TIME);
			if (cap > maxCapacity)  {
				maxCapacity = cap;
				maxCapLink = link;
			}
		}
		return maxCapLink;
	}

}
