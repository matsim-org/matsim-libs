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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

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
 * @author nrieser
 */
public final class SubsequentLinksAnalyzer {

	private final Network network;
	private final String preferredMode;

	/** Stores the logical subsequent link (value) for a given link (by link-id-index). */
	private final Link[] subsequentLinks;

	public SubsequentLinksAnalyzer(final Network network, final String preferredMode) {
		this.network = network;
		this.preferredMode = preferredMode;
		this.subsequentLinks = new Link[Id.getNumberOfIds(Link.class)];
		compute();
	}

	/**
	 * @return an array, giving for each link (link-id-index as array index) the computed subsequent link (value in map).
	 */
	public Link[] getSubsequentLinks() {
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
			Set<String> modes = l.getAllowedModes();
			Node from = l.getFromNode();
			Node to = l.getToNode();
			Coord cFrom = from.getCoord();
			Coord cTo = to.getCoord();
			double xTo = cTo.getX();
			double yTo = cTo.getY();
			double thetaL = Math.atan2(yTo - cFrom.getY(), xTo - cFrom.getX());
			Collection<? extends Link> outLinks = to.getOutLinks().values();

			String requiredMode = modes.size() == 1
					? modes.iterator().next()
					: (modes.contains(this.preferredMode) ? this.preferredMode : (modes.isEmpty() ? this.preferredMode : modes.iterator().next()));

			Collection<? extends Link> potentialLinks = outLinks.stream().filter(link -> link.getAllowedModes().contains(requiredMode)).toList();

			if (potentialLinks.isEmpty()) {
				potentialLinks = outLinks;
			}

			if (potentialLinks.size() > 1) {
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
				this.subsequentLinks[l.getId().index()] = linkData[0].link;
			} else if (potentialLinks.size() == 1) {
				this.subsequentLinks[l.getId().index()] = outLinks.iterator().next();
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
