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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkIdComparator;

import java.util.*;

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
public final class SubsequentLinksAnalyzer {

	private final Network network;

	/** Stores the logical subsequent link (value) for a given link (key). */
	private final Map<Id<Link>, Id<Link>> subsequentLinks = new IdMap<>(Link.class);

	public SubsequentLinksAnalyzer(final Network network) {
		this.network = network;
		compute();
	}

	/**
	 * @return a map, giving for each link (key in map) the computed subsequent link (value in map).
	 */
	public Map<Id<Link>, Id<Link>> getSubsequentLinks() {
		return this.subsequentLinks;
	}

	/**
	 * calculates the "default next" link of the current link with respect to
	 * geometry and Capacity (depending on BETA) and writes the result in
	 * ssLinks.
	 */
	private void compute() {
		Comparator<LinkData> linkDataComparator = (o1, o2) -> {
			int cmp = Double.compare(o1.theta, o2.theta); // prefer the one with smaller theta
			if (cmp == 0) {
				cmp = Double.compare(o2.link.getCapacity(), o1.link.getCapacity()); // prefer the one with larger capacity
			}
			if (cmp == 0) {
				cmp = o1.link.getId().compareTo(o2.link.getId());
			}
			return cmp;
		};
		LinkData[] linkData = new LinkData[10];
		for (Link l : this.network.getLinks().values()) {
			Node from = l.getFromNode();
			Node to = l.getToNode();
			Coord cFrom = from.getCoord();
			Coord cTo = to.getCoord();
			double xTo = cTo.getX();
			double yTo = cTo.getY();
			double thetaL = Math.atan2(yTo - cFrom.getY(), xTo - cFrom.getX());
			Collection<? extends Link> outLinks = to.getOutLinks().values();
			if (outLinks.size() > 1) {
				int linkCount = 0;

				for (Link out : outLinks) {
					Coord cOut = out.getToNode().getCoord();
					double deltaTheta = Math.atan2(cOut.getY() - yTo, cOut.getX()	- xTo) - thetaL;
					while (deltaTheta < -Math.PI) {
						deltaTheta += 2.0 * Math.PI;
					}
					while (deltaTheta > Math.PI) {
						deltaTheta -= 2.0 * Math.PI;
					}

					// add to link data
					if (linkCount == linkData.length) { // we need more space
						linkData = Arrays.copyOf(linkData, linkData.length * 2);
					}
					if (linkData[linkCount] == null) {
						linkData[linkCount] = new LinkData(out, Math.abs(deltaTheta));
					} else {
						LinkData data = linkData[linkCount];
						data.link = out;
						data.theta = Math.abs(deltaTheta);
					}
					linkCount++;
				}

				// find the best subsequent link: the one with the smallest theta, if multiple have this than the one with the biggest capacity
				Arrays.sort(linkData, 0, linkCount, linkDataComparator);
				this.subsequentLinks.put(l.getId(), linkData[0].link.getId());
			} else if (outLinks.size() == 1) {
				this.subsequentLinks.put(l.getId(), outLinks.iterator().next().getId());
			}
		}
	}

	private static class LinkData {
		Link link;
		double theta;

		public LinkData(Link link, double theta) {
			this.link = link;
			this.theta = theta;
		}
	}

}
