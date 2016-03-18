/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.lib.tools.networkModification;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Identify links close to a given center and modify them.
 *
 * @author boescpa
 */
public abstract class AbstractNetworkModifier {

	protected int radius;
	protected final Coord center;

	public AbstractNetworkModifier(Coord center) {
		this.center = center;
	}

	public void run(String[] args) {
		String path2MATSimNetwork = args[0];
		radius = Integer.parseInt(args[1]);
		String path2NewMATSimNetwork = args[2];

		// Read network
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(path2MATSimNetwork);
		Network network = scenario.getNetwork();

		// Identify and modify links in area.
		for (Link link : network.getLinks().values()) {
			if (isLinkAffected(link)) {
				link.setCapacity(100.0); // Very low capacity...
				link.setFreespeed(0.000000000000001); // Very low free speed...
			}
		}

		// Write network
		new NetworkWriter(network).write(path2NewMATSimNetwork);
	}

	public abstract boolean isLinkAffected(Link link);
}
