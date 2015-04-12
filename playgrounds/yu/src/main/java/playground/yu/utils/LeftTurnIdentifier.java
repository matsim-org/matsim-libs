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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.CalculateAngle;

/**
 * based on {@code org.matsim.signalsystems.CalculateAngle}
 * 
 * @author yu
 * 
 */
public class LeftTurnIdentifier {
	private static String getDirection(boolean leftTurn) {
		return leftTurn ? "left turn" : "right turn";
	}

	/**
	 * @param leg
	 *            a {@code Leg} that will be checked, how many left turns it
	 *            contains
	 * @return the number of
	 */
	public static int getNumberOfLeftTurnsFromALeg(Leg leg,
			Map<Id<Link>, ? extends Link> netLinks) {
		Route route = leg.getRoute();
		if (route instanceof NetworkRoute) {
			// **********************************************
			// if a route contains just ONE Link (incl. startLink and endLink),
			// this is route will be automatically created as a genericRoute,
			// though the Leg Mode could be "car"
			// **********************************************
			Id startLinkId = route.getStartLinkId();
			Id endLinkId = route.getEndLinkId();
			// if (route instanceof GenericRoute) {
			// System.err.println(">>>>>leg:\n" + leg.toString());
			// System.err.println(">>>>>route:\n" + route.toString());
			// throw new RuntimeException(
			// "This Route is a GenericRoute with LegMode \"car\", I don't know why!!!");
			// }
			List<Id<Link>> linkIds = ((NetworkRoute) route).getLinkIds();
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

		} else {// pt, walk, whatever else that is an instance of {@code
				// GenericRoute}
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
					return outLinksSortedByAngle.headMap(lowerKey, true)
							.containsValue(outLink);
				}
				return outLinksSortedByAngle.headMap(lowerKey,
						Math.abs(lowerKey) >= Math.abs(higherKey))
						.containsValue(outLink);
				/* ">"-true-inclusive, "<="-false-exclusive */
			} else {
				return outLinksSortedByAngle.headMap(zeroAngle).containsValue(
						outLink/* ,false strict exclusive 0 */);
			}
		}
		return false;
	}

	public static void main(String[] args) {
		String networkFilename = "test/input/2car1ptRoutes/net2.xml";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		Network network = scenario.getNetwork();

		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Link link1 = links.get(Id.create(1, Link.class)), link2 = links.get(Id.create(2, Link.class)), link10 = links
				.get(Id.create(10, Link.class)), link11 = links.get(Id.create(11, Link.class)), link19 = links
				.get(Id.create(19, Link.class)), link20 = links.get(Id.create(20, Link.class)), link21 = links
				.get(Id.create(21, Link.class)), link22 = links.get(Id.create(22, Link.class)), link23 = links
				.get(Id.create(23, Link.class));

		System.out.println("link1->link2\t"
				+ getDirection(isLeftTurn(link1, link2)));
		System.out.println("link1->link10\t"
				+ getDirection(isLeftTurn(link1, link10)));
		System.out.println("link2->link12\t"
				+ getDirection(isLeftTurn(link2, link11)));
		System.out.println("link10->link19\t"
				+ getDirection(isLeftTurn(link10, link19)));
		System.out.println("link11->link20\t"
				+ getDirection(isLeftTurn(link11, link20)));
		System.out.println("link19->link20\t"
				+ getDirection(isLeftTurn(link19, link20)));
	}
}