package org.matsim.contrib.evacuation.utils;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests whether two network files have the same physical properties, IDs can be different.
 * Created by laemmel on 18/12/15.
 */
public class NetworksComparator {

	private static final Logger log = Logger.getLogger(NetworksComparator.class);

	private final Map<Link, Link> l2LMapping = new HashMap<>();

	public boolean compare(final String net0, final String net1) {


		Config c0 = ConfigUtils.createConfig();
		Scenario sc0 = ScenarioUtils.createScenario(c0);
		new MatsimNetworkReader(sc0.getNetwork()).readFile(net0);
		Network n0 = sc0.getNetwork();

		Config c1 = ConfigUtils.createConfig();
		Scenario sc1 = ScenarioUtils.createScenario(c1);
		new MatsimNetworkReader(sc1.getNetwork()).readFile(net1);
		Network n1 = sc1.getNetwork();

		if (n0.getCapacityPeriod() != n1.getCapacityPeriod()) {
			return false;
		}

		if (n0.getEffectiveLaneWidth() != n1.getEffectiveLaneWidth()) {
			return false;
		}

		if (n0.getLinks().size() != n1.getLinks().size()) {
			return false;
		}

		if (n0.getNodes().size() != n1.getNodes().size()) {
			return false;
		}

		for (Node n0n : n0.getNodes().values()) {
			Node n1n = NetworkUtils.getNearestNode(((Network) n1),n0n.getCoord());
			if (!aquivalentNodes(n0n, n1n)) {
				log.warn("Nodes do not match. Their might be two or more nodes at the same coordinate! Performing a linear search.");
				boolean found = false;
				for (Node n1nn : n1.getNodes().values()) {
					if (aquivalentNodes(n0n, n1nn)) {

						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}

		}

		for (Link n0l : n0.getLinks().values()) {


			Link n1l = this.l2LMapping.get(n0l);
			if (n1l == null) {
				return false;
			}
			if (n0l.getCoord().getX() != n1l.getCoord().getX()) {
				return false;
			}
			if (n0l.getCoord().getY() != n1l.getCoord().getY()) {
				return false;
			}
			if (n0l.getAllowedModes().size() != n1l.getAllowedModes().size()) {
				return false;
			}
			for (String mode : n0l.getAllowedModes()) {
				if (!n1l.getAllowedModes().contains(mode)) {
					return false;
				}
			}
			if (n0l.getCapacity() != n1l.getCapacity()) {
				return false;
			}
			if (n0l.getFreespeed() != n1l.getFreespeed()) {
				return false;
			}
			if (n0l.getLength() != n1l.getLength()) {
				return false;
			}
			if (n0l.getNumberOfLanes() != n1l.getNumberOfLanes()) {
				return false;
			}

		}


		return true;
	}

	private boolean aquivalentNodes(Node n0n, Node n1n) {
		if (n1n == null) {
			return false;
		}
		if (!sameCharacteristics(n0n, n1n)) {
			return false;
		}


		for (Link n0ol : n0n.getOutLinks().values()) {
			Node n0olToN = n0ol.getToNode();

			boolean found = false;
			for (Link n1ol : n1n.getOutLinks().values()) {
				Node n1olToN = n1ol.getToNode();
				if (sameCharacteristics(n0olToN, n1olToN)) {
					this.l2LMapping.put(n0ol, n1ol);
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		for (Link n0il : n0n.getInLinks().values()) {
			Node n0ilFrN = n0il.getFromNode();

			boolean found = false;
			for (Link n1il : n1n.getInLinks().values()) {
				Node n1ilFrN = n1il.getFromNode();
				if (sameCharacteristics(n0ilFrN, n1ilFrN)) {
					this.l2LMapping.put(n0il, n1il);
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	private static boolean sameCharacteristics(Node n0n, Node n1n) {
		if (n0n.getCoord().getX() != n1n.getCoord().getX()) {
			return false;
		}
		if (n0n.getCoord().getY() != n1n.getCoord().getY()) {
			return false;
		}
		if (n0n.getOutLinks().size() != n1n.getOutLinks().size()) {
			return false;
		}
		return n0n.getInLinks().size() == n1n.getInLinks().size();
	}
}
