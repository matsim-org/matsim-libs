/* *********************************************************************** *
 * project: org.matsim.*
 * NewMultiModalNetworkWithoutBusLink.java
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
package playground.yu.newNetwork;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.util.HashSet;
import java.util.Set;

public class NewMultiModalNetworkWithoutBusLink {
	private static Set<Link> busLinks2delete = new HashSet<Link>();

	public static Network excludeBusLinks(Network mmNet,
			TransitSchedule schedule) {
		for (TransitLine tl : schedule.getTransitLines().values()) {
			for (TransitRoute tr : tl.getRoutes().values()) {
				handleRoute(tr, mmNet);
			}
		}
		checkLinks2delete();
		for (Link link : busLinks2delete) {
			mmNet.removeLink(link.getId());
		}
		return mmNet;
	}

	private static void handleBusLink(Link link) {
		if (link != null) {
			// System.out.println("linkId:\t" + link.getId());
			System.out.println("link prior modes:\t" + link.getAllowedModes());
			Set<String> modes = link.getAllowedModes();
			modes.add("bus");
			link.setAllowedModes(modes);
			System.out.println("link posterior modes:\t"
					+ link.getAllowedModes());
			busLinks2delete.add(link);
		}
	}

	private static void handleOtherPtLink(Link link, String mode) {
		if (link != null) {
			System.out.println("link prior modes:\t" + link.getAllowedModes());
			Set<String> modes = link.getAllowedModes();
			modes.add(mode);
			link.setAllowedModes(modes);
			System.out.println("link posterior modes:\t"
					+ link.getAllowedModes());
		}
	}

	private static void handleRoute(TransitRoute route, Network network) {
		String tm = route.getTransportMode();
		NetworkRoute nrwr = route.getRoute();
		if (tm.equals("bus")) {
			handleBusLink(network.getLinks().get(nrwr.getStartLinkId()));
			for (Id linkId : nrwr.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				handleBusLink(link);
			}
			handleBusLink(network.getLinks().get(nrwr.getEndLinkId()));
		} else {
			System.out.println(tm);
			handleOtherPtLink(network.getLinks().get(nrwr.getStartLinkId()), tm);
			for (Id linkId : nrwr.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				handleOtherPtLink(link, tm);
			}
			handleOtherPtLink(network.getLinks().get(nrwr.getEndLinkId()), tm);
		}
	}

	private static void checkLinks2delete() {
		Set<String> modes = new HashSet<String>();
		modes.add(TransportMode.pt);
		modes.add("bus");
		Set<Link> tmpSet = new HashSet<Link>();
		tmpSet.addAll(busLinks2delete);
		for (Link link : tmpSet) {
			for (String mode : link.getAllowedModes()) {
				if (!modes.contains(mode)) {
					busLinks2delete.remove(link);
					break;
				}
			}
		}
	}

	public static void main(final String[] args) {
		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz";
		String transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
		String newNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newMultiModalNetBiggerWithoutBusLinkTest.xml.gz";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);

		Network multiModalNetwork = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(multiModalNetworkFile);

		TransitSchedule schedule = scenario.getTransitSchedule();
		new TransitScheduleReader(scenario).readFile(transitScheduleFile);

		new NetworkWriter(excludeBusLinks(multiModalNetwork, schedule))
				.write(newNetworkFile);
	}
}
