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

package playground.yu.compressRoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.misc.Time;

/**
 * Analyzes the network to find for each link the "logical subsequent link",
 * i.e. the link that most people are likely to continue driving on when
 * crossing the next node.  This could depend on the geometry (assuming it
 * is more likely to continue straight on instead of making a U-turn), the
 * capacity of links (likely to continue on links with the same or larger
 * capacity instead of using small bumpy roads), the speed limit, other
 * attributes or combinations there of.
 *
 * @author ychen
 * @author mrieser
 */
public class Subsequent {

	private final NetworkLayer network;

	/** Criterion to judge Capacity */
	private static double BETA = 0.0;

	private final static LinkComparator linkComparator = new LinkComparator();

	/** Stores the logical subsequent link (value) for a given link (key). */
	private final TreeMap<Link, Link> subsequentLinks = new TreeMap<Link, Link>(linkComparator);

	public Subsequent(final NetworkLayer network) {
		this.network = network;
		compute();
	}

	/**
	 * @return a map, giving for each link (key in map) the computed subsequent link (value in map).
	 */
	public Map<Link, Link> getSubsequentLinks() {
		return this.subsequentLinks;
	}

	/**
	 * calculates the "default next" link of the current link with respect to
	 * geometry and Capacity (depending on BETA) and writes the result in
	 * ssLinks.
	 */
	private void compute() {
		Map<Link, Double> absDeltaThetas = new TreeMap<Link, Double>(linkComparator);
		for (Link l : this.network.getLinks().values()) {
			Node from = l.getFromNode();
			Node to = l.getToNode();
			Coord cFrom = from.getCoord();
			Coord cTo = to.getCoord();
			double xTo = cTo.getX();
			double yTo = cTo.getY();
			double thetaL = Math.atan2(yTo - cFrom.getY(), xTo - cFrom.getX());
			Collection<? extends Link> outLinks = to.getOutLinks().values();
			absDeltaThetas.clear();
			if (outLinks.size() > 1) {

				for (Link out : outLinks) {
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

	private Link computeSubsequentLink(final Link l, final Map<Link, Double> thetas) {
		Link finalOutLink = null;
		List<Link> minThetaOutLinks = new ArrayList<Link>();
		while (finalOutLink == null) {
			minThetaOutLinks.clear();
			double absMin = Collections.min(thetas.values()).doubleValue();
			for (Map.Entry<Link, Double> entry : thetas.entrySet()) {
				if (absMin == (entry.getValue()).doubleValue())
					minThetaOutLinks.add(entry.getKey());
			}
			if (minThetaOutLinks.size() == 1) {
				Link outLink = minThetaOutLinks.get(0);
				if (outLink.getCapacity(Time.UNDEFINED_TIME) >= BETA * l.getCapacity(Time.UNDEFINED_TIME))
					finalOutLink = outLink;
				else {
					thetas.remove(outLink);
				}
			} else if (minThetaOutLinks.size() == 2) {
				Link outLinkA = minThetaOutLinks.get(0);
				Link outLinkB = minThetaOutLinks.get(1);
				double capA = outLinkA.getCapacity(Time.UNDEFINED_TIME);
				double capB = outLinkB.getCapacity(Time.UNDEFINED_TIME);
				if (l.getCapacity(Time.UNDEFINED_TIME) > Math.min(capA, capB)) {
					finalOutLink = (capA >= capB) ? outLinkA : outLinkB;
				} else {
					finalOutLink = (MatsimRandom.random.nextDouble() < 0.5) ? outLinkA : outLinkB;
				}
			}
		}
		return finalOutLink;
	}

	/*package*/ static class LinkComparator implements Comparator<Link> {
		public int compare(final Link o1, final Link o2) {
			return o1.getId().compareTo(o2.getId());
		}
	}

}
