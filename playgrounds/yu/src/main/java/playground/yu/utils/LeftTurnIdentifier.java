/* *********************************************************************** *
 * project: org.matsim.*
 * LeftTurnIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.signalsystems.CalculateAngle;

/**
 * based on {@code org.matsim.signalsystems.CalculateAngle}
 * 
 * @author yu
 * 
 */
public class LeftTurnIdentifier {
	/**
	 * @param leg
	 *            a {@code Leg} that will be checked, how many left turns it
	 *            contains
	 * @return the number of
	 */
	public static int getNumberOfLeftTurnsFromALeg(Leg leg,
			Map<Id, ? extends Link> netLinks) {
		if (leg.getMode().equals(TransportMode.car)) {
			Route route = leg.getRoute();
			Id startLinkId = route.getStartLinkId();
			Id endLinkId = route.getEndLinkId();
			List<Id> linkIds = ((NetworkRoute) route).getLinkIds();
			if (startLinkId.equals(endLinkId) && linkIds.size() == 0) {
				return 0;
			}

			List<Link> allLinks = new LinkedList<Link>();
			// purposely don't add startedLink in the List<Link>
			for (Id linkId : linkIds) {
				allLinks.add(netLinks.get(linkId));
			}
			allLinks.add(netLinks.get(endLinkId));

			Link currentLink = netLinks.get(startLinkId);
			int number = 0;
			for (Link nextLink : allLinks) {
				if (isLeftTurn(currentLink, nextLink)) {
					number++;
				}
				currentLink = nextLink;
			}

			return number;

		} else {// pt, walk, whatever else
			return 0;
		}

	}

	public static boolean isLeftTurn(Link inLink, Link outLink) {
		if (outLink.getToNode().equals(inLink.getFromNode())) {
			/* U-Turn (size==0) */
			return true;
		}

		TreeMap<Double, Link> outLinksSortedByAngle = CalculateAngle
		.getOutLinksSortedByAngle(inLink);
		int realOutLinksSize = outLinksSortedByAngle.size();
		if (realOutLinksSize == 1) {
			/* NOT intersection */
			return false;
		} else if (realOutLinksSize > 1) {
			Double zeroAngle = 0d;
			if (!outLinksSortedByAngle.containsKey(zeroAngle)) {
				/* without straight link */
				Double lowerKey = outLinksSortedByAngle.lowerKey(zeroAngle);
				Double higherKey = outLinksSortedByAngle.higherKey(zeroAngle);
				if (lowerKey == null) {
					/* no left turns */
					return false;
				}
				if (higherKey == null) {
					/* no right turns */
					return outLinksSortedByAngle.headMap(lowerKey)
					.containsValue(outLink);
				}
				return outLinksSortedByAngle.headMap(lowerKey,
						Math.abs(lowerKey) > Math.abs(higherKey))
						.containsValue(outLink);
				/* ">"-true-inclusive, "<="-false-exclusive */
			} else {
				return outLinksSortedByAngle.headMap(zeroAngle).containsValue(
						outLink/* ,false strict exclusive 0 */);
			}
		}
		return false;
	}
}
